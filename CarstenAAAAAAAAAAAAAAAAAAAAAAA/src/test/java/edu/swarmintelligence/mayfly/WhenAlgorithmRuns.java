package edu.swarmintelligence.mayfly;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;
import com.tngtech.jgiven.annotation.ProvidedScenarioState;

import java.util.ArrayList;
import java.util.List;

public class WhenAlgorithmRuns extends Stage<WhenAlgorithmRuns> {
    record ReproducibilityRun(MayflyResult first, MayflyResult second) {
    }

    @ExpectedScenarioState
    MayflyConfig config;

    @ExpectedScenarioState
    long seed;

    @ExpectedScenarioState
    List<Long> seeds;

    @ExpectedScenarioState
    ConvergenceAnalyzer convergenceAnalyzer;

    @ProvidedScenarioState
    MayflyResult result;

    @ProvidedScenarioState
    ReproducibilityRun reproducibilityRun;

    @ProvidedScenarioState
    AnalyticsReport report;

    @ProvidedScenarioState
    List<Double> finalFitnessValues;

    @ProvidedScenarioState
    ConvergenceAnalyzer.ConvergenceResult convergenceResult;

    public WhenAlgorithmRuns the_algorithm_runs() {
        AnalyticsEngine engine = engine(config, seed);
        MayflyAlgorithm algorithm = new MayflyAlgorithm();
        algorithm.addListener(engine);
        result = algorithm.run(config, seed);
        report = engine.report();
        return self();
    }

    public WhenAlgorithmRuns the_algorithm_runs_twice() {
        MayflyResult first = new MayflyAlgorithm().run(config, seed);
        MayflyResult second = new MayflyAlgorithm().run(config, seed);
        reproducibilityRun = new ReproducibilityRun(first, second);
        return self();
    }

    public WhenAlgorithmRuns the_algorithm_runs_for_all_seeds() {
        finalFitnessValues = new ArrayList<>();
        for (long currentSeed : seeds) {
            finalFitnessValues.add(new MayflyAlgorithm().run(config, currentSeed).gbestFitness());
        }
        return self();
    }

    public WhenAlgorithmRuns the_constant_stream_is_analyzed() {
        for (int i = 1; i <= 5; i++) {
            convergenceAnalyzer.onEvent(new MayflyEvent.IterationCompleted(i, 3.0, List.of()));
        }
        convergenceResult = (ConvergenceAnalyzer.ConvergenceResult) convergenceAnalyzer.result();
        return self();
    }

    private AnalyticsEngine engine(MayflyConfig config, long seed) {
        AnalyticsEngine engine = new AnalyticsEngine(config, seed);
        engine.addAnalyzer(new AgentInteractionAnalyzer());
        engine.addAnalyzer(new GlobalMemoryAnalyzer());
        engine.addAnalyzer(new LocalMemoryAnalyzer());
        engine.addAnalyzer(new ConvergenceAnalyzer());
        return engine;
    }
}
