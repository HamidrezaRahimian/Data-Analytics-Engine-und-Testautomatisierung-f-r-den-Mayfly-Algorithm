package edu.swarmintelligence.mayfly;

import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ThenAnalyticsReport extends Stage<ThenAnalyticsReport> {
    @ExpectedScenarioState
    double threshold;

    @ExpectedScenarioState
    MayflyResult result;

    @ExpectedScenarioState
    MayflyResult secondResult;

    @ExpectedScenarioState
    AnalyticsReport report;

    @ExpectedScenarioState
    List<Double> finalFitnessValues;

    @ExpectedScenarioState
    ConvergenceAnalyzer.ConvergenceResult convergenceResult;

    public ThenAnalyticsReport both_runs_have_the_same_gbest() {
        assertThat(result.gbestFitness()).isEqualTo(secondResult.gbestFitness());
        assertThat(result.gbestPosition()).containsExactly(secondResult.gbestPosition());
        return self();
    }

    public ThenAnalyticsReport the_final_fitness_is_below(double expectedFitness) {
        assertThat(result.gbestFitness()).isLessThan(expectedFitness);
        return self();
    }

    public ThenAnalyticsReport the_gbest_trajectory_is_monotonic() {
        GlobalMemoryAnalyzer.GlobalMemoryResult global = globalResult();
        double previous = Double.POSITIVE_INFINITY;
        for (GlobalMemoryAnalyzer.GbestPoint point : global.gbestTrajectory()) {
            assertThat(point.fitness()).isLessThanOrEqualTo(previous);
            previous = point.fitness();
        }
        return self();
    }

    public ThenAnalyticsReport the_female_attraction_rate_is_above_the_threshold() {
        AgentInteractionAnalyzer.AgentInteractionResult interaction = interactionResult();
        double average = interaction.femaleAttractionRate().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        assertThat(average).isGreaterThan(threshold);
        return self();
    }

    public ThenAnalyticsReport pbest_updates_are_recorded_for_agents() {
        LocalMemoryAnalyzer.LocalMemoryResult local = localResult();
        // Agent identity changes during selection, so this checks agents that emitted pbest update events.
        assertThat(local.pbestUpdateCountPerAgent()).isNotEmpty();
        assertThat(local.pbestUpdateCountPerAgent().values()).allMatch(count -> count >= 1);
        return self();
    }

    public ThenAnalyticsReport exactly_one_plateau_is_detected() {
        assertThat(convergenceResult.plateauSegments()).hasSize(1);
        return self();
    }

    public ThenAnalyticsReport mean_plus_standard_deviation_is_reasonable() {
        double mean = finalFitnessValues.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double variance = 0.0;
        for (double value : finalFitnessValues) {
            double diff = value - mean;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / finalFitnessValues.size());
        assertThat(mean + stdDev).isLessThan(1.0);
        return self();
    }

    private AgentInteractionAnalyzer.AgentInteractionResult interactionResult() {
        return (AgentInteractionAnalyzer.AgentInteractionResult) report.byAnalyzer().get("agent-interaction");
    }

    private GlobalMemoryAnalyzer.GlobalMemoryResult globalResult() {
        return (GlobalMemoryAnalyzer.GlobalMemoryResult) report.byAnalyzer().get("global-memory");
    }

    private LocalMemoryAnalyzer.LocalMemoryResult localResult() {
        return (LocalMemoryAnalyzer.LocalMemoryResult) report.byAnalyzer().get("local-memory");
    }
}
