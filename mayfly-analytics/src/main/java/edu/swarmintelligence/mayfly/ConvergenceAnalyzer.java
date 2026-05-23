package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.List;

public class ConvergenceAnalyzer implements MayflyAnalyzer {
    private static final double DEFAULT_THRESHOLD = 1.0e-3;
    private static final double DEFAULT_PLATEAU_DELTA = 1.0e-12;
    private static final int DEFAULT_PLATEAU_LENGTH = 5;
    private static final double LOG_EPSILON = 1.0e-12;

    private final double threshold;
    private final double plateauDelta;
    private final int plateauLength;
    private final List<ConvergencePoint> convergenceCurve = new ArrayList<>();
    private final List<Double> populationDiversity = new ArrayList<>();

    public ConvergenceAnalyzer() {
        this(DEFAULT_THRESHOLD, DEFAULT_PLATEAU_DELTA, DEFAULT_PLATEAU_LENGTH);
    }

    public ConvergenceAnalyzer(double threshold, double plateauDelta, int plateauLength) {
        this.threshold = threshold;
        this.plateauDelta = plateauDelta;
        this.plateauLength = plateauLength;
    }

    @Override
    public void onEvent(MayflyEvent event) {
        if (event instanceof MayflyEvent.IterationCompleted completed) {
            convergenceCurve.add(new ConvergencePoint(completed.iteration(), completed.gbestFitness()));
            populationDiversity.add(populationDiversity(completed.survivors()));
        }
    }

    @Override
    public AnalyzerResult result() {
        return new ConvergenceResult(
                List.copyOf(convergenceCurve),
                List.copyOf(populationDiversity),
                iterationsToThreshold(threshold),
                threshold,
                plateauSegments(plateauDelta, plateauLength),
                plateauDelta,
                plateauLength,
                convergenceRateEstimate()
        );
    }

    @Override
    public String name() {
        return "convergence";
    }

    private double populationDiversity(List<Mayfly> agents) {
        if (agents.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                sum += distance(agents.get(i).pos, agents.get(j).pos);
                count++;
            }
        }
        return sum / count;
    }

    private double distance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private int iterationsToThreshold(double epsilon) {
        for (ConvergencePoint point : convergenceCurve) {
            if (point.fitness() <= epsilon) {
                return point.iteration();
            }
        }
        return -1;
    }

    private List<PlateauSegment> plateauSegments(double delta, int k) {
        List<PlateauSegment> segments = new ArrayList<>();
        int start = -1;
        int length = 0;

        for (int i = 1; i < convergenceCurve.size(); i++) {
            double improvement = convergenceCurve.get(i - 1).fitness() - convergenceCurve.get(i).fitness();
            if (Math.abs(improvement) <= delta) {
                if (start < 0) {
                    start = convergenceCurve.get(i - 1).iteration();
                    length = 2;
                } else {
                    length++;
                }
            } else {
                if (start >= 0 && length >= k) {
                    segments.add(new PlateauSegment(start, convergenceCurve.get(i - 1).iteration(), length));
                }
                start = -1;
                length = 0;
            }
        }

        if (start >= 0 && length >= k) {
            segments.add(new PlateauSegment(start, convergenceCurve.getLast().iteration(), length));
        }
        return segments;
    }

    private double convergenceRateEstimate() {
        if (convergenceCurve.size() < 2) {
            return 0.0;
        }
        int start = Math.max(0, convergenceCurve.size() - Math.max(2, convergenceCurve.size() / 5));
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;
        int count = 0;

        for (int i = start; i < convergenceCurve.size(); i++) {
            ConvergencePoint point = convergenceCurve.get(i);
            double y = Math.log(Math.max(point.fitness(), LOG_EPSILON));
            if (!Double.isFinite(y)) {
                continue;
            }
            double x = point.iteration();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
            count++;
        }

        double denominator = count * sumXX - sumX * sumX;
        if (count < 2 || denominator == 0.0) {
            return 0.0;
        }
        return (count * sumXY - sumX * sumY) / denominator;
    }

    public record ConvergencePoint(int iteration, double fitness) {
    }

    public record PlateauSegment(int startIteration, int endIteration, int length) {
    }

    public record ConvergenceResult(
            List<ConvergencePoint> convergenceCurve,
            List<Double> populationDiversity,
            int iterationsToThreshold,
            double threshold,
            List<PlateauSegment> plateauSegments,
            double plateauDelta,
            int plateauLength,
            double convergenceRateEstimate
    ) implements AnalyzerResult {
    }
}
