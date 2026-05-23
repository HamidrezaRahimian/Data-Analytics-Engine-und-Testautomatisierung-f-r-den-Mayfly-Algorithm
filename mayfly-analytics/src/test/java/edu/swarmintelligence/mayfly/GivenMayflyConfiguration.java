package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 JGiven stage, reviewed and accepted manually.
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;

import java.util.ArrayList;
import java.util.List;

public class GivenMayflyConfiguration extends Stage<GivenMayflyConfiguration> {
    @ProvidedScenarioState
    MayflyConfig config;

    @ProvidedScenarioState
    long seed;

    @ProvidedScenarioState
    List<Long> seeds;

    @ProvidedScenarioState
    double threshold;

    @ProvidedScenarioState
    ConvergenceAnalyzer convergenceAnalyzer;

    public GivenMayflyConfiguration the_default_configuration() {
        config = MayflyConfig.ackley10D();
        return self();
    }

    public GivenMayflyConfiguration seed_$_is_used(long seed) {
        this.seed = seed;
        return self();
    }

    public GivenMayflyConfiguration the_same_seed_is_used_twice(long seed) {
        this.seed = seed;
        return self();
    }

    public GivenMayflyConfiguration the_female_attraction_threshold_is(double threshold) {
        this.threshold = threshold;
        return self();
    }

    public GivenMayflyConfiguration ten_fixed_seeds_are_used() {
        seeds = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            seeds.add(i);
        }
        return self();
    }

    public GivenMayflyConfiguration a_constant_fitness_stream() {
        convergenceAnalyzer = new ConvergenceAnalyzer(0.001, 0.0, 5);
        return self();
    }
}
