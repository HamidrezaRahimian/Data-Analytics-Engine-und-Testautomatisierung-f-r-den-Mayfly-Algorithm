package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiRunStatistics {
    public Statistics calculate(List<Double> values) {
        if (values.isEmpty()) {
            return new Statistics(0, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        double mean = mean(sorted);
        double stdDev = standardDeviation(sorted, mean);
        double margin = tCritical(sorted.size()) * stdDev / Math.sqrt(sorted.size());
        return new Statistics(
                sorted.size(),
                mean,
                percentile(sorted, 0.50),
                stdDev,
                percentile(sorted, 0.25),
                percentile(sorted, 0.75),
                mean - margin,
                mean + margin
        );
    }

    private double mean(List<Double> values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private double standardDeviation(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / (values.size() - 1));
    }

    private double percentile(List<Double> sorted, double p) {
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }
        double index = p * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }

    private double tCritical(int sampleSize) {
        // Two-sided 95 percent t critical values for df 1..30, then normal approximation.
        double[] values = {
                12.706, 4.303, 3.182, 2.776, 2.571, 2.447, 2.365, 2.306, 2.262, 2.228,
                2.201, 2.179, 2.160, 2.145, 2.131, 2.120, 2.110, 2.101, 2.093, 2.086,
                2.080, 2.074, 2.069, 2.064, 2.060, 2.056, 2.052, 2.048, 2.045, 2.042
        };
        int df = sampleSize - 1;
        if (df <= 0) {
            return 0.0;
        }
        if (df <= values.length) {
            return values[df - 1];
        }
        return 1.96;
    }

    public record Statistics(
            int count,
            double mean,
            double median,
            double standardDeviation,
            double q25,
            double q75,
            double confidenceLow,
            double confidenceHigh
    ) {
    }
}
