package com.gridmind.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SyntheticDatasetGenerator {

    public static final long FIXED_SEED = 42L;

    public enum Scenario {
        NORMAL,
        GRADUAL_DEGRADATION,
        SUDDEN_FAILURE
    }

    public record DatasetSample(
            double temperature,
            double voltage,
            double current,
            double vibration,
            double ageYears,
            int failureLabel,
            Scenario scenario
    ) {}

    public List<DatasetSample> generateSyntheticDataset(int totalSamples) {
        if (totalSamples < 10) {
            throw new IllegalArgumentException("El dataset debe tener al menos 10 muestras.");
        }

        Random random = new Random(FIXED_SEED);
        List<DatasetSample> dataset = new ArrayList<>(totalSamples);
        int normalCount = (int) (totalSamples * 0.70);
        int gradualCount = (int) (totalSamples * 0.18);
        int suddenCount = totalSamples - normalCount - gradualCount;

        for (int i = 0; i < normalCount; i++) {
            double load = random.nextDouble();
            boolean transientSensorProfile = i < normalCount * 0.15;
            dataset.add(new DatasetSample(
                    transientSensorProfile
                            ? 80.0 + random.nextGaussian() * 7.0
                            : 45.0 + load * 25.0 + random.nextGaussian() * 7.0,
                    13.8 + random.nextGaussian() * 0.28,
                    transientSensorProfile
                            ? 360.0 + random.nextDouble() * 130.0 + random.nextGaussian() * 35.0
                            : 180.0 + load * 220.0 + random.nextGaussian() * 45.0,
                    transientSensorProfile
                            ? Math.abs(4.2 + random.nextDouble() * 1.6 + random.nextGaussian() * 0.65)
                            : Math.abs(0.7 + load * 2.4 + random.nextGaussian() * 0.7),
                    transientSensorProfile
                            ? 6.0 + random.nextDouble() * 16.0
                            : 1.0 + random.nextDouble() * 20.0,
                    0,
                    Scenario.NORMAL
            ));
        }

        for (int i = 0; i < gradualCount; i++) {
            double progress = random.nextDouble();
            dataset.add(new DatasetSample(
                    64.0 + progress * 30.0 + random.nextGaussian() * 5.0,
                    13.8 + random.nextGaussian() * 0.42,
                    285.0 + progress * 190.0 + random.nextGaussian() * 55.0,
                    2.0 + progress * 4.5 + random.nextGaussian() * 0.9,
                    4.0 + random.nextDouble() * 18.0,
                    1,
                    Scenario.GRADUAL_DEGRADATION
            ));
        }

        for (int i = 0; i < suddenCount; i++) {
            dataset.add(new DatasetSample(
                    76.0 + random.nextDouble() * 27.0 + random.nextGaussian() * 4.0,
                    13.8 + random.nextGaussian() * 0.65,
                    360.0 + random.nextDouble() * 260.0 + random.nextGaussian() * 45.0,
                    3.4 + random.nextDouble() * 5.2 + random.nextGaussian() * 0.8,
                    3.0 + random.nextDouble() * 20.0,
                    1,
                    Scenario.SUDDEN_FAILURE
            ));
        }

        return dataset;
    }
}
