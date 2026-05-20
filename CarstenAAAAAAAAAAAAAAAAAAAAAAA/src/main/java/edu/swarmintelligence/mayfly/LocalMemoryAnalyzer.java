package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalMemoryAnalyzer implements MayflyAnalyzer {
    private final IdentityHashMap<Mayfly, Integer> pbestUpdateCountPerAgent = new IdentityHashMap<>();
    private double pbestImprovementSum;
    private int pbestImprovementCount;
    private final List<Double> pbestPositionDiversity = new ArrayList<>();
    private final List<FitnessDistribution> pbestFitnessDistribution = new ArrayList<>();

    @Override
    public void onEvent(MayflyEvent event) {
        if (event instanceof MayflyEvent.PbestUpdated updated) {
            pbestUpdateCountPerAgent.merge(updated.agent(), 1, Integer::sum);
            if (Double.isFinite(updated.previousPbestFitness()) && updated.previousPbestFitness() != 0.0) {
                pbestImprovementSum += (updated.previousPbestFitness() - updated.newPbestFitness())
                        / Math.abs(updated.previousPbestFitness());
                pbestImprovementCount++;
            }
        } else if (event instanceof MayflyEvent.IterationCompleted completed) {
            pbestPositionDiversity.add(positionDiversity(completed.survivors()));
            pbestFitnessDistribution.add(fitnessDistribution(completed.survivors()));
        }
    }

    @Override
    public AnalyzerResult result() {
        return new LocalMemoryResult(
                new LinkedHashMap<>(pbestUpdateCountPerAgent),
                pbestImprovementCount == 0 ? 0.0 : pbestImprovementSum / pbestImprovementCount,
                List.copyOf(pbestPositionDiversity),
                List.copyOf(pbestFitnessDistribution)
        );
    }

    @Override
    public String name() {
        return "local-memory";
    }

    private double positionDiversity(List<Mayfly> agents) {
        if (agents.isEmpty()) {
            return 0.0;
        }
        int dimensions = agents.getFirst().pbestPos.length;
        double totalStdDev = 0.0;
        for (int d = 0; d < dimensions; d++) {
            double mean = 0.0;
            for (Mayfly agent : agents) {
                mean += agent.pbestPos[d];
            }
            mean /= agents.size();

            double variance = 0.0;
            for (Mayfly agent : agents) {
                double diff = agent.pbestPos[d] - mean;
                variance += diff * diff;
            }
            variance /= agents.size();
            totalStdDev += Math.sqrt(variance);
        }
        return totalStdDev / dimensions;
    }

    private FitnessDistribution fitnessDistribution(List<Mayfly> agents) {
        if (agents.isEmpty()) {
            return new FitnessDistribution(0.0, 0.0, 0.0, 0.0, 0.0);
        }
        double[] values = new double[agents.size()];
        for (int i = 0; i < agents.size(); i++) {
            values[i] = agents.get(i).pbestFitness;
        }
        Arrays.sort(values);
        return new FitnessDistribution(
                values[0],
                percentile(values, 0.25),
                percentile(values, 0.50),
                percentile(values, 0.75),
                values[values.length - 1]
        );
    }

    private double percentile(double[] sorted, double p) {
        if (sorted.length == 1) {
            return sorted[0];
        }
        double index = p * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted[lower];
        }
        double weight = index - lower;
        return sorted[lower] * (1.0 - weight) + sorted[upper] * weight;
    }

    public record FitnessDistribution(double min, double q25, double median, double q75, double max) {
    }

    public record LocalMemoryResult(
            Map<Mayfly, Integer> pbestUpdateCountPerAgent,
            double meanPbestImprovement,
            List<Double> pbestPositionDiversity,
            List<FitnessDistribution> pbestFitnessDistribution
    ) implements AnalyzerResult {
    }
}
