package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalMemoryAnalyzer")
class LocalMemoryAnalyzerTest {
    private LocalMemoryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new LocalMemoryAnalyzer();
    }

    @Nested
    class HappyPath {
        @Test
        void countsPbestUpdatesPerAgent() {
            Mayfly first = agent(1.0, 0.0, 0.0);
            Mayfly second = agent(2.0, 1.0, 1.0);

            analyzer.onEvent(new MayflyEvent.PbestUpdated(first, 10.0, 5.0));
            analyzer.onEvent(new MayflyEvent.PbestUpdated(first, 5.0, 4.0));
            analyzer.onEvent(new MayflyEvent.PbestUpdated(second, 9.0, 3.0));

            assertThat(result().pbestUpdateCountPerAgent())
                    .containsEntry(first, 2)
                    .containsEntry(second, 1);
        }

        @Test
        void calculatesMeanPbestImprovement() {
            Mayfly agent = agent(1.0, 0.0, 0.0);
            analyzer.onEvent(new MayflyEvent.PbestUpdated(agent, 10.0, 5.0));
            analyzer.onEvent(new MayflyEvent.PbestUpdated(agent, 20.0, 10.0));

            assertThat(result().meanPbestImprovement()).isEqualTo(0.5);
        }

        @Test
        void calculatesPbestPositionDiversity() {
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, List.of(
                    agent(1.0, 0.0, 0.0),
                    agent(2.0, 2.0, 4.0)
            )));

            assertThat(result().pbestPositionDiversity()).containsExactly(1.5);
        }

        @Test
        void calculatesFitnessDistribution() {
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, List.of(
                    agent(1.0, 0.0, 0.0),
                    agent(2.0, 0.0, 0.0),
                    agent(3.0, 0.0, 0.0),
                    agent(4.0, 0.0, 0.0)
            )));

            assertThat(result().pbestFitnessDistribution().getFirst())
                    .isEqualTo(new LocalMemoryAnalyzer.FitnessDistribution(1.0, 1.75, 2.5, 3.25, 4.0));
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void skipsRelativeImprovementWhenPreviousPbestIsZero() {
            analyzer.onEvent(new MayflyEvent.PbestUpdated(agent(1.0, 0.0, 0.0), 0.0, -1.0));

            assertThat(result().meanPbestImprovement()).isZero();
        }

        @Test
        void handlesEmptySurvivors() {
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, List.of()));

            var result = result();

            assertThat(result.pbestPositionDiversity()).containsExactly(0.0);
            assertThat(result.pbestFitnessDistribution().getFirst())
                    .isEqualTo(new LocalMemoryAnalyzer.FitnessDistribution(0.0, 0.0, 0.0, 0.0, 0.0));
        }

        @Test
        void handlesSingleSurvivor() {
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, List.of(agent(7.0, 2.0, 3.0))));

            assertThat(result().pbestPositionDiversity()).containsExactly(0.0);
        }
    }

    @Nested
    class NumericalStability {
        @Test
        void skipsNonFinitePreviousPbestInMeanImprovement() {
            analyzer.onEvent(new MayflyEvent.PbestUpdated(agent(1.0, 0.0, 0.0), Double.POSITIVE_INFINITY, 1.0));

            assertThat(result().meanPbestImprovement()).isZero();
        }
    }

    private LocalMemoryAnalyzer.LocalMemoryResult result() {
        return (LocalMemoryAnalyzer.LocalMemoryResult) analyzer.result();
    }

    private Mayfly agent(double pbestFitness, double first, double second) {
        Mayfly agent = new Mayfly(2);
        agent.pbestFitness = pbestFitness;
        agent.pbestPos[0] = first;
        agent.pbestPos[1] = second;
        return agent;
    }
}
