package edu.swarmintelligence.mayfly;

import java.util.List;

public sealed interface MayflyEvent permits
        MayflyEvent.RunStarted,
        MayflyEvent.IterationStarted,
        MayflyEvent.MaleUpdated,
        MayflyEvent.FemaleUpdated,
        MayflyEvent.OffspringCreated,
        MayflyEvent.PbestUpdated,
        MayflyEvent.GbestUpdated,
        MayflyEvent.IterationCompleted,
        MayflyEvent.RunCompleted {

    record RunStarted(int dimensions, double initialGbestFitness) implements MayflyEvent {}

    record IterationStarted(int iteration, double inertiaWeight) implements MayflyEvent {}
    record MaleUpdated(Mayfly agent, boolean isNuptialDance, double previousFitness) implements MayflyEvent {}
    record FemaleUpdated(Mayfly agent, boolean isAttracted, double previousFitness) implements MayflyEvent {}
    record OffspringCreated(Mayfly offspring) implements MayflyEvent {}
    record PbestUpdated(Mayfly agent, double previousPbestFitness, double newPbestFitness) implements MayflyEvent {}
    record GbestUpdated(UpdateSource source, double previousGbestFitness, double newGbestFitness) implements MayflyEvent {}
    record IterationCompleted(int iteration, double gbestFitness, List<Mayfly> survivors) implements MayflyEvent {}
    record RunCompleted(MayflyResult result) implements MayflyEvent {}
}
