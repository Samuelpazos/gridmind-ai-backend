package com.gridmind.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Synthetic Dataset Generator for GridMind AI.
 * Generates reproducible training data (Seed 42) with normal, gradual degradation,
 * sudden bursts, noise, and class overlap for transformer failure prediction.
 */
@Component
public class SyntheticDatasetGenerator {

    public static final long FIXED_SEED = 42L;

    public record DatasetSample(
            double temperature, // °C
            double voltage,     // kV
            double current,     // A
            double vibration,   // mm/s
            double ageYears,    // years
            int failureLabel    // 0 = Normal, 1 = Failure within N hours
    ) {}

    public List<DatasetSample> generateSyntheticDataset(int totalSamples) {
        Random random = new Random(FIXED_SEED);
        List<DatasetSample> dataset = new ArrayList<>();

        int normalCount = (int) (totalSamples * 0.70);
        int gradualDegradationCount = (int) (totalSamples * 0.18);
        int suddenFailureCount = totalSamples - normalCount - gradualDegradationCount;

        // 1. Normal Operation (70%)
        for (int i = 0; i < normalCount; i++) {
            double temp = 45.0 + random.nextGaussian() * 5.0;     // Mean 45, std 5
            double volt = 13.8 + random.nextGaussian() * 0.2;     // Mean 13.8, std 0.2
            double curr = 220.0 + random.nextGaussian() * 30.0;  // Mean 220, std 30
            double vib = 1.0 + Math.abs(random.nextGaussian() * 0.4); // Mean 1.0, std 0.4
            double age = 1.0 + random.nextDouble() * 10.0;       // 1 to 11 years

            int label = 0;
            // Introduce 3% noise/class overlap for realism
            if (random.nextDouble() < 0.03) {
                label = 1;
            }
            dataset.add(new DatasetSample(temp, volt, curr, vib, age, label));
        }

        // 2. Gradual Degradation (18%)
        for (int i = 0; i < gradualDegradationCount; i++) {
            double progress = random.nextDouble(); // 0 to 1 degradation severity
            double temp = 65.0 + progress * 28.0 + random.nextGaussian() * 2.0;  // 65 to 93 °C
            double volt = 13.8 + (random.nextDouble() - 0.5) * 0.6;
            double curr = 320.0 + progress * 160.0 + random.nextGaussian() * 20.0; // 320 to 480 A
            double vib = 2.5 + progress * 4.0 + random.nextGaussian() * 0.5;      // 2.5 to 6.5 mm/s
            double age = 7.0 + random.nextDouble() * 13.0;                         // 7 to 20 years

            int label = progress > 0.45 ? 1 : 0;
            dataset.add(new DatasetSample(temp, volt, curr, vib, age, label));
        }

        // 3. Sudden Failure Bursts (12%)
        for (int i = 0; i < suddenFailureCount; i++) {
            double temp = 82.0 + random.nextDouble() * 20.0;     // 82 to 102 °C
            double volt = 13.8 + (random.nextDouble() - 0.5) * 1.5;
            double curr = 400.0 + random.nextDouble() * 250.0;   // 400 to 650 A
            double vib = 4.5 + random.nextDouble() * 5.0;        // 4.5 to 9.5 mm/s
            double age = 10.0 + random.nextDouble() * 12.0;      // 10 to 22 years

            int label = 1;
            dataset.add(new DatasetSample(temp, volt, curr, vib, age, label));
        }

        return dataset;
    }
}
