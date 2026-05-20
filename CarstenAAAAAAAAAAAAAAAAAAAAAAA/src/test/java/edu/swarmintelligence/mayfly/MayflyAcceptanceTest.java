package edu.swarmintelligence.mayfly;

import com.tngtech.jgiven.annotation.Description;
import com.tngtech.jgiven.annotation.Tag;
import com.tngtech.jgiven.junit5.ScenarioTest;
import org.junit.jupiter.api.Test;

class MayflyAcceptanceTest extends ScenarioTest<GivenMayflyConfiguration, WhenAlgorithmRuns, ThenAnalyticsReport> {

    @Test
    @Description("AT1 Reproducibility: same seed gives the same global best.")
    void same_seed_produces_same_gbest() {
        given().the_default_configuration()
                .and().the_same_seed_is_used_twice(42L);

        when().the_algorithm_runs_twice();

        then().both_runs_have_the_same_gbest();
    }

    @Test
    @Tag("convergence")
    @Description("AT2 Convergence: default configuration reaches the Ackley smoke bound.")
    void default_run_converges_below_smoke_bound() {
        given().the_default_configuration()
                .and().seed_$_is_used(42L);

        when().the_algorithm_runs();

        then().the_final_fitness_is_below(1.0e-3);
    }

    @Test
    @Tag("global-memory")
    @Description("AT3 Global memory: global best fitness never increases.")
    void gbest_trajectory_is_monotonic() {
        given().the_default_configuration()
                .and().seed_$_is_used(42L);

        when().the_algorithm_runs();

        then().the_gbest_trajectory_is_monotonic();
    }

    @Test
    @Tag("agent-interaction")
    @Description("AT4 Agent interaction: female attraction rate is above a small threshold.")
    void female_attraction_rate_is_above_threshold() {
        given().the_default_configuration()
                .and().seed_$_is_used(42L)
                .and().the_female_attraction_threshold_is(0.05);

        when().the_algorithm_runs();

        then().the_female_attraction_rate_is_above_the_threshold();
    }

    @Test
    @Tag("local-memory")
    @Description("AT5 Local memory: agents with pbest update events have at least one recorded update.")
    void pbest_updates_are_recorded() {
        given().the_default_configuration()
                .and().seed_$_is_used(42L);

        when().the_algorithm_runs();

        then().pbest_updates_are_recorded_for_agents();
    }

    @Test
    @Tag("convergence")
    @Description("AT6 Plateau detection: a constant fitness stream creates one plateau.")
    void constant_fitness_stream_has_one_plateau() {
        given().a_constant_fitness_stream();

        when().the_constant_stream_is_analyzed();

        then().exactly_one_plateau_is_detected();
    }

    @Test
    @Tag("convergence")
    @Description("Multi-run: ten deterministic seeds keep mean plus standard deviation reasonable.")
    void multiple_runs_have_reasonable_average_fitness() {
        given().the_default_configuration()
                .and().ten_fixed_seeds_are_used();

        when().the_algorithm_runs_for_all_seeds();

        then().mean_plus_standard_deviation_is_reasonable();
    }
}
