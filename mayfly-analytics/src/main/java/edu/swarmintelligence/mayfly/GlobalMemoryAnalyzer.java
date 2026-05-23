package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GlobalMemoryAnalyzer implements MayflyAnalyzer {
    private static final double DEFAULT_EPSILON = 1.0e-3;

    private int gbestUpdateCount;
    private double lastIterationFitness = Double.NaN;
    private final List<GbestPoint> gbestTrajectory = new ArrayList<>();
    private final List<Double> improvementDelta = new ArrayList<>();
    private final EnumMap<UpdateSource, Integer> sourceCounts = new EnumMap<>(UpdateSource.class);

    public GlobalMemoryAnalyzer() {
        for (UpdateSource source : UpdateSource.values()) {
            sourceCounts.put(source, 0);
        }
    }

    @Override
    public void onEvent(MayflyEvent event) {
        if (event instanceof MayflyEvent.GbestUpdated updated) {
            gbestUpdateCount++;
            sourceCounts.put(updated.source(), sourceCounts.get(updated.source()) + 1);
        } else if (event instanceof MayflyEvent.IterationCompleted completed) {
            double delta = Double.isNaN(lastIterationFitness)
                    ? 0.0
                    : lastIterationFitness - completed.gbestFitness();
            gbestTrajectory.add(new GbestPoint(completed.iteration(), completed.gbestFitness()));
            improvementDelta.add(delta);
            lastIterationFitness = completed.gbestFitness();
        }
    }

    @Override
    public AnalyzerResult result() {
        return new GlobalMemoryResult(
                List.copyOf(gbestTrajectory),
                gbestUpdateCount,
                sourceDistribution(),
                List.copyOf(improvementDelta),
                stagnationStreaks(),
                firstHittingIteration(DEFAULT_EPSILON),
                DEFAULT_EPSILON
        );
    }

    @Override
    public String name() {
        return "global-memory";
    }

    private Map<UpdateSource, Double> sourceDistribution() {
        EnumMap<UpdateSource, Double> distribution = new EnumMap<>(UpdateSource.class);
        for (UpdateSource source : UpdateSource.values()) {
            distribution.put(source, gbestUpdateCount == 0 ? 0.0 : (double) sourceCounts.get(source) / gbestUpdateCount);
        }
        return distribution;
    }

    private List<Integer> stagnationStreaks() {
        List<Integer> streaks = new ArrayList<>();
        int current = 0;
        for (double delta : improvementDelta) {
            if (delta == 0.0) {
                current++;
            } else if (current > 0) {
                streaks.add(current);
                current = 0;
            }
        }
        if (current > 0) {
            streaks.add(current);
        }
        return streaks;
    }

    private int firstHittingIteration(double epsilon) {
        for (GbestPoint point : gbestTrajectory) {
            if (point.fitness() <= epsilon) {
                return point.iteration();
            }
        }
        return -1;
    }

    public record GbestPoint(int iteration, double fitness) {
    }

    public record GlobalMemoryResult(
            List<GbestPoint> gbestTrajectory,
            int gbestUpdateCount,
            Map<UpdateSource, Double> gbestUpdateSourceDistribution,
            List<Double> improvementDelta,
            List<Integer> stagnationStreaks,
            int firstHittingIteration,
            double epsilon
    ) implements AnalyzerResult {
    }
}
