package com.gridmind.ai;

import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import smile.anomaly.IsolationForest;
import smile.base.cart.SplitRule;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructType;
import smile.data.type.StructField;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

@Service
public class SmileMlService {

    private static final Logger log = LoggerFactory.getLogger(SmileMlService.class);
    private static final int DATASET_SIZE = 2_500;
    private static final int RANDOM_FOREST_TREES = 200;
    private static final int ISOLATION_FOREST_TREES = 100;
    private static final double FAILURE_DECISION_THRESHOLD = 0.20;
    private static final String[] DISPLAY_FEATURE_NAMES = {
            "Temperatura", "Voltaje", "Corriente", "Vibración", "Edad del Transformador"
    };

    private final SyntheticDatasetGenerator datasetGenerator;
    private volatile RandomForest classifier;
    private volatile IsolationForest anomalyDetector;
    private volatile StructType inputSchema;
    private volatile boolean modelTrained;
    private volatile double modelPrecision;
    private volatile double modelRecall;
    private volatile double modelF1Score;
    private volatile double modelRocAuc;
    private volatile int trainingSamplesCount;
    private volatile int testSamplesCount;
    private volatile int[][] confusionMatrix = new int[2][2];
    private volatile String trainingError;
    private volatile Map<String, Double> featureImportances = Map.of();

    public SmileMlService(SyntheticDatasetGenerator datasetGenerator) {
        this.datasetGenerator = datasetGenerator;
    }

    @PostConstruct
    public void init() {
        trainModelsOnStartup();
    }

    public synchronized void trainModelsOnStartup() {
        modelTrained = false;
        trainingError = null;
        try {
            List<SyntheticDatasetGenerator.DatasetSample> samples =
                    datasetGenerator.generateSyntheticDataset(DATASET_SIZE);
            DatasetSplit split = stratifiedSplit(samples);

            DataFrame trainingData = toDataFrame(split.training());
            inputSchema = trainingData.schema();
            classifier = RandomForest.fit(
                    Formula.lhs("failureLabel"),
                    trainingData,
                    RANDOM_FOREST_TREES,
                    2,
                    SplitRule.GINI,
                    20,
                    100,
                    1,
                    0.7,
                    null,
                    LongStream.range(
                            SyntheticDatasetGenerator.FIXED_SEED,
                            SyntheticDatasetGenerator.FIXED_SEED + RANDOM_FOREST_TREES
                    )
            );

            double[][] normalSamples = samples.stream()
                    .filter(sample -> sample.scenario() == SyntheticDatasetGenerator.Scenario.NORMAL)
                    .map(SmileMlService::toFeatureVector)
                    .toArray(double[][]::new);
            anomalyDetector = IsolationForest.fit(
                    normalSamples,
                    ISOLATION_FOREST_TREES,
                    Math.min(256, normalSamples.length),
                    0.99,
                    0
            );

            calculateMetrics(split.test());
            calculateFeatureImportances();
            trainingSamplesCount = split.training().size();
            testSamplesCount = split.test().size();
            modelTrained = true;

            log.info("Smile models trained on {} samples; test precision={}, recall={}, F1={}, ROC-AUC={}, confusion matrix={}",
                    samples.size(), modelPrecision, modelRecall, modelF1Score, modelRocAuc,
                    formatConfusionMatrix(confusionMatrix));
        } catch (RuntimeException exception) {
            classifier = null;
            anomalyDetector = null;
            featureImportances = Map.of();
            trainingError = exception.getMessage();
            log.error("Smile model training failed; supervised and anomaly strategies will use degraded mode",
                    exception);
        }
    }

    public double calculateAnomalyScore(TransformerEntity transformer, Map<String, Double> sensorValues) {
        IsolationForest detector = requireAnomalyDetector();
        return detector.score(toFeatureVector(
                sensorValues.getOrDefault("TEMPERATURE", 50.0),
                sensorValues.getOrDefault("VOLTAGE", 13.8),
                sensorValues.getOrDefault("CURRENT", 250.0),
                sensorValues.getOrDefault("VIBRATION", 1.0),
                ageOf(transformer)
        ));
    }

    public EvaluationResult predictFailureProbability(
            TransformerEntity transformer,
            Map<String, Double> sensorValues
    ) {
        RandomForest model = requireClassifier();
        double[] features = toFeatureVector(
                sensorValues.getOrDefault("TEMPERATURE", 50.0),
                sensorValues.getOrDefault("VOLTAGE", 13.8),
                sensorValues.getOrDefault("CURRENT", 250.0),
                sensorValues.getOrDefault("VIBRATION", 1.0),
                ageOf(transformer)
        );
        double[] posteriori = new double[model.numClasses()];
        model.predict(toTuple(features), posteriori);
        double probability = positiveClassProbability(model, posteriori);
        RiskLevel risk = RiskLevel.fromProbability(probability);
        double anomalyScore = calculateAnomalyScore(transformer, sensorValues);

        List<String> topFactors = explainFactors(sensorValues, ageOf(transformer), risk);
        String explanation = switch (risk) {
            case HIGH, CRITICAL ->
                    "El modelo detectó una probabilidad elevada de falla según los patrones aprendidos.";
            case MEDIUM -> "El modelo detectó señales de degradación que requieren seguimiento.";
            default -> "El modelo no detectó señales significativas de falla inminente.";
        };

        return EvaluationResult.builder()
                .failureProbability(probability)
                .riskLevel(risk)
                .anomalyScore(anomalyScore)
                .topFactors(topFactors)
                .explanation(explanation)
                .strategyName("SUPERVISED_MODEL_RANDOM_FOREST")
                .fallbackUsed(false)
                .build();
    }

    private void calculateMetrics(List<SyntheticDatasetGenerator.DatasetSample> testSamples) {
        int[][] matrix = new int[2][2];
        List<ScoredLabel> scores = new ArrayList<>(testSamples.size());
        RandomForest model = requireClassifier();

        for (SyntheticDatasetGenerator.DatasetSample sample : testSamples) {
            double[] posteriori = new double[model.numClasses()];
            int modelPrediction = model.predict(toTuple(toFeatureVector(sample)), posteriori);
            int prediction = modelPrediction == 1
                    || positiveClassProbability(model, posteriori) >= FAILURE_DECISION_THRESHOLD ? 1 : 0;
            int actual = sample.failureLabel();
            matrix[actual][prediction]++;
            scores.add(new ScoredLabel(actual, positiveClassProbability(model, posteriori)));
        }

        int truePositive = matrix[1][1];
        int falsePositive = matrix[0][1];
        int falseNegative = matrix[1][0];
        modelPrecision = ratio(truePositive, truePositive + falsePositive);
        modelRecall = ratio(truePositive, truePositive + falseNegative);
        modelF1Score = ratio(2 * modelPrecision * modelRecall, modelPrecision + modelRecall);
        modelRocAuc = calculateRocAuc(scores);
        confusionMatrix = matrix;
    }

    private void calculateFeatureImportances() {
        double[] importances = requireClassifier().importance();
        double total = 0.0;
        for (double importance : importances) {
            total += importance;
        }

        Map<String, Double> calculated = new LinkedHashMap<>();
        for (int i = 0; i < importances.length && i < DISPLAY_FEATURE_NAMES.length; i++) {
            calculated.put(
                    DISPLAY_FEATURE_NAMES[i],
                    total == 0.0 ? 0.0 : importances[i] / total
            );
        }
        featureImportances = Collections.unmodifiableMap(calculated);
    }

    private static double calculateRocAuc(List<ScoredLabel> scores) {
        long positives = scores.stream().filter(score -> score.label() == 1).count();
        long negatives = scores.size() - positives;
        if (positives == 0 || negatives == 0) {
            return 0.0;
        }

        List<ScoredLabel> sorted = scores.stream()
                .sorted(Comparator.comparingDouble(ScoredLabel::score))
                .toList();
        double positiveRankSum = 0.0;
        int start = 0;
        while (start < sorted.size()) {
            int end = start + 1;
            while (end < sorted.size()
                    && Double.compare(sorted.get(start).score(), sorted.get(end).score()) == 0) {
                end++;
            }
            double averageRank = ((start + 1) + end) / 2.0;
            for (int i = start; i < end; i++) {
                if (sorted.get(i).label() == 1) {
                    positiveRankSum += averageRank;
                }
            }
            start = end;
        }

        return (positiveRankSum - positives * (positives + 1.0) / 2.0) / (positives * negatives);
    }

    private static DatasetSplit stratifiedSplit(List<SyntheticDatasetGenerator.DatasetSample> samples) {
        Map<Integer, List<SyntheticDatasetGenerator.DatasetSample>> byLabel = new LinkedHashMap<>();
        for (SyntheticDatasetGenerator.DatasetSample sample : samples) {
            byLabel.computeIfAbsent(sample.failureLabel(), ignored -> new ArrayList<>()).add(sample);
        }

        List<SyntheticDatasetGenerator.DatasetSample> training = new ArrayList<>();
        List<SyntheticDatasetGenerator.DatasetSample> test = new ArrayList<>();
        for (Map.Entry<Integer, List<SyntheticDatasetGenerator.DatasetSample>> entry : byLabel.entrySet()) {
            List<SyntheticDatasetGenerator.DatasetSample> classSamples = entry.getValue();
            Collections.shuffle(classSamples, new java.util.Random(
                    SyntheticDatasetGenerator.FIXED_SEED + entry.getKey()
            ));
            int split = Math.max(1, (int) Math.floor(classSamples.size() * 0.8));
            training.addAll(classSamples.subList(0, split));
            test.addAll(classSamples.subList(split, classSamples.size()));
        }
        Collections.shuffle(training, new java.util.Random(SyntheticDatasetGenerator.FIXED_SEED));
        Collections.shuffle(test, new java.util.Random(SyntheticDatasetGenerator.FIXED_SEED + 1));
        return new DatasetSplit(training, test);
    }

    private static DataFrame toDataFrame(List<SyntheticDatasetGenerator.DatasetSample> samples) {
        return DataFrame.of(
                DoubleVector.of("temperature", samples.stream().mapToDouble(
                        SyntheticDatasetGenerator.DatasetSample::temperature).toArray()),
                DoubleVector.of("voltage", samples.stream().mapToDouble(
                        SyntheticDatasetGenerator.DatasetSample::voltage).toArray()),
                DoubleVector.of("current", samples.stream().mapToDouble(
                        SyntheticDatasetGenerator.DatasetSample::current).toArray()),
                DoubleVector.of("vibration", samples.stream().mapToDouble(
                        SyntheticDatasetGenerator.DatasetSample::vibration).toArray()),
                DoubleVector.of("ageYears", samples.stream().mapToDouble(
                        SyntheticDatasetGenerator.DatasetSample::ageYears).toArray()),
                IntVector.of(
                        new StructField(
                                "failureLabel",
                                DataTypes.IntegerType,
                                new NominalScale("normal", "failure")
                        ),
                        samples.stream().mapToInt(
                                SyntheticDatasetGenerator.DatasetSample::failureLabel).toArray()
                )
        );
    }

    private static double positiveClassProbability(RandomForest model, double[] posteriori) {
        int[] classes = model.classes();
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == 1) {
                return posteriori[i];
            }
        }
        throw new IllegalStateException("El modelo no contiene la clase positiva de falla.");
    }

    private static List<String> explainFactors(
            Map<String, Double> sensorValues,
            double age,
            RiskLevel risk
    ) {
        List<String> factors = new ArrayList<>();
        double temperature = sensorValues.getOrDefault("TEMPERATURE", 50.0);
        double vibration = sensorValues.getOrDefault("VIBRATION", 1.0);
        double current = sensorValues.getOrDefault("CURRENT", 250.0);
        if (temperature > 70.0) factors.add("Temperatura elevada (" + temperature + " °C)");
        if (vibration > 2.0) factors.add("Vibración elevada (" + vibration + " mm/s)");
        if (current > 300.0) factors.add("Corriente elevada (" + current + " A)");
        if (age > 10.0) factors.add("Antigüedad del transformador (" + (int) age + " años)");
        if (factors.isEmpty() && risk == RiskLevel.LOW) {
            factors.add("Operación dentro de parámetros normales");
        }
        return factors;
    }

    private static double[] toFeatureVector(SyntheticDatasetGenerator.DatasetSample sample) {
        return toFeatureVector(
                sample.temperature(),
                sample.voltage(),
                sample.current(),
                sample.vibration(),
                sample.ageYears()
        );
    }

    private static double[] toFeatureVector(
            double temperature,
            double voltage,
            double current,
            double vibration,
            double ageYears
    ) {
        return new double[]{temperature, voltage, current, vibration, ageYears};
    }

    private Tuple toTuple(double[] features) {
        StructType schema = inputSchema;
        if (schema == null) {
            throw new IllegalStateException("El esquema de entrada del modelo no está disponible.");
        }
        return Tuple.of(new Object[]{
                features[0], features[1], features[2], features[3], features[4], 0
        }, schema);
    }

    private static double ratio(double numerator, double denominator) {
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    private static String formatConfusionMatrix(int[][] matrix) {
        return String.format("[[%d,%d],[%d,%d]]", matrix[0][0], matrix[0][1], matrix[1][0], matrix[1][1]);
    }

    private static double ageOf(TransformerEntity transformer) {
        return transformer != null && transformer.getAgeYears() != null
                ? transformer.getAgeYears()
                : 5.0;
    }

    private RandomForest requireClassifier() {
        RandomForest model = classifier;
        if (model == null) {
            throw new IllegalStateException("El clasificador Smile no está disponible.", null);
        }
        return model;
    }

    private IsolationForest requireAnomalyDetector() {
        IsolationForest detector = anomalyDetector;
        if (detector == null) {
            throw new IllegalStateException("El detector Smile no está disponible.", null);
        }
        return detector;
    }

    public boolean isModelTrained() {
        return modelTrained;
    }

    public String getTrainingError() {
        return trainingError;
    }

    public double getModelPrecision() {
        return modelPrecision;
    }

    public double getModelRecall() {
        return modelRecall;
    }

    public double getModelF1Score() {
        return modelF1Score;
    }

    public double getModelRocAuc() {
        return modelRocAuc;
    }

    public int getTrainingDatasetSize() {
        return trainingSamplesCount;
    }

    public int getTestDatasetSize() {
        return testSamplesCount;
    }

    public int[][] getConfusionMatrix() {
        return new int[][]{confusionMatrix[0].clone(), confusionMatrix[1].clone()};
    }

    public Map<String, Double> getFeatureImportances() {
        return featureImportances;
    }

    private record DatasetSplit(
            List<SyntheticDatasetGenerator.DatasetSample> training,
            List<SyntheticDatasetGenerator.DatasetSample> test
    ) {}

    private record ScoredLabel(int label, double score) {}
}
