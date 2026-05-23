package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AgentInteractionAnalyzer implements MayflyAnalyzer {
    private int currentIteration;
    private int nuptialDanceCount;
    private int attractionCount;
    private int femaleAttractionCount;
    private int femaleUpdateCount;
    private double pairDistanceSum;
    private int pairDistanceCount;
    private double pairFitnessGapSum;
    private int pairFitnessGapCount;
    private final List<Double> femaleAttractionRate = new ArrayList<>();
    private final List<Double> meanPairDistance = new ArrayList<>();
    private final List<Double> pairFitnessGap = new ArrayList<>();
    private final EnumMap<InteractionType, Integer> histogram = new EnumMap<>(InteractionType.class);

    public AgentInteractionAnalyzer() {
        for (InteractionType type : InteractionType.values()) {
            histogram.put(type, 0);
        }
    }

    @Override
    public void onEvent(MayflyEvent event) {
        if (event instanceof MayflyEvent.IterationStarted started) {
            currentIteration = started.iteration();
            femaleAttractionCount = 0;
            femaleUpdateCount = 0;
            pairDistanceSum = 0.0;
            pairDistanceCount = 0;
            pairFitnessGapSum = 0.0;
            pairFitnessGapCount = 0;
        } else if (event instanceof MayflyEvent.MaleUpdated maleUpdated) {
            if (maleUpdated.isNuptialDance()) {
                nuptialDanceCount++;
                increment(InteractionType.NUPTIAL_DANCE);
            } else {
                attractionCount++;
                increment(InteractionType.MALE_ATTRACTION);
            }
        } else if (event instanceof MayflyEvent.FemaleUpdated femaleUpdated) {
            femaleUpdateCount++;
            if (femaleUpdated.isAttracted()) {
                femaleAttractionCount++;
                increment(InteractionType.FEMALE_ATTRACTION);
            } else {
                increment(InteractionType.FEMALE_RANDOM_WALK);
            }
        } else if (event instanceof MayflyEvent.OffspringCreated offspringCreated) {
            if (offspringCreated.firstOffspringOfPair()) {
                pairDistanceSum += offspringCreated.parentDistance();
                pairDistanceCount++;
                pairFitnessGapSum += offspringCreated.parentFitnessGap();
                pairFitnessGapCount++;
            }
        } else if (event instanceof MayflyEvent.IterationCompleted) {
            ensureIndex(currentIteration);
            femaleAttractionRate.set(currentIteration - 1,
                    femaleUpdateCount == 0 ? 0.0 : (double) femaleAttractionCount / femaleUpdateCount);
            meanPairDistance.set(currentIteration - 1,
                    pairDistanceCount == 0 ? 0.0 : pairDistanceSum / pairDistanceCount);
            pairFitnessGap.set(currentIteration - 1,
                    pairFitnessGapCount == 0 ? 0.0 : pairFitnessGapSum / pairFitnessGapCount);
        }
    }

    @Override
    public AnalyzerResult result() {
        return new AgentInteractionResult(
                nuptialDanceCount,
                attractionCount,
                List.copyOf(femaleAttractionRate),
                List.copyOf(meanPairDistance),
                List.copyOf(pairFitnessGap),
                Map.copyOf(histogram)
        );
    }

    @Override
    public String name() {
        return "agent-interaction";
    }

    private void increment(InteractionType type) {
        histogram.put(type, histogram.get(type) + 1);
    }

    private void ensureIndex(int iteration) {
        while (femaleAttractionRate.size() < iteration) {
            femaleAttractionRate.add(0.0);
            meanPairDistance.add(0.0);
            pairFitnessGap.add(0.0);
        }
    }

    public enum InteractionType {
        NUPTIAL_DANCE,
        MALE_ATTRACTION,
        FEMALE_ATTRACTION,
        FEMALE_RANDOM_WALK
    }

    public record AgentInteractionResult(
            int nuptialDanceCount,
            int attractionCount,
            List<Double> femaleAttractionRate,
            List<Double> meanPairDistance,
            List<Double> pairFitnessGap,
            Map<InteractionType, Integer> interactionHistogram
    ) implements AnalyzerResult {
    }
}
