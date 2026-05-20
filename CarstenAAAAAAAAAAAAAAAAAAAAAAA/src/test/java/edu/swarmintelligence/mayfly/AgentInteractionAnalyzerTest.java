package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentInteractionAnalyzer")
class AgentInteractionAnalyzerTest {
    private AgentInteractionAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new AgentInteractionAnalyzer();
    }

    @Nested
    class HappyPath {
        @Test
        void countsNuptialDanceAndMaleAttractionSeparately() {
            analyzer.onEvent(new MayflyEvent.MaleUpdated(agent(0.0), true, 3.0));
            analyzer.onEvent(new MayflyEvent.MaleUpdated(agent(1.0), false, 4.0));

            var result = result();

            assertThat(result.nuptialDanceCount()).isEqualTo(1);
            assertThat(result.attractionCount()).isEqualTo(1);
        }

        @Test
        void calculatesFemaleAttractionRatePerIteration() {
            analyzer.onEvent(new MayflyEvent.IterationStarted(1, 0.9));
            analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(1.0), true, 5.0));
            analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(2.0), false, 6.0));
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, java.util.List.of()));

            assertThat(result().femaleAttractionRate()).containsExactly(0.5);
        }

        @Test
        void calculatesMeanPairDistanceAndFitnessGap() {
            analyzer.onEvent(new MayflyEvent.IterationStarted(1, 0.9));
            analyzer.onEvent(new MayflyEvent.OffspringCreated(agent(0.0), 3.0, 5.0, true));
            analyzer.onEvent(new MayflyEvent.OffspringCreated(agent(0.0), 3.0, 5.0, false));
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, java.util.List.of()));

            assertThat(result().meanPairDistance()).containsExactly(3.0);
            assertThat(result().pairFitnessGap()).containsExactly(5.0);
        }

        @ParameterizedTest(name = "female attraction count {0}")
        @ValueSource(ints = {0, 1, 3})
        void parameterizedFemaleAttractionRate(int attractedCount) {
            analyzer.onEvent(new MayflyEvent.IterationStarted(1, 0.9));
            for (int i = 0; i < attractedCount; i++) {
                analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(i), true, 1.0));
            }
            for (int i = attractedCount; i < 4; i++) {
                analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(i), false, 1.0));
            }
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, java.util.List.of()));

            assertThat(result().femaleAttractionRate()).containsExactly(attractedCount / 4.0);
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void handlesNoFemaleUpdates() {
            analyzer.onEvent(new MayflyEvent.IterationStarted(1, 0.9));
            analyzer.onEvent(new MayflyEvent.IterationCompleted(1, 1.0, java.util.List.of()));

            assertThat(result().femaleAttractionRate()).containsExactly(0.0);
        }

        @Test
        void fillsInteractionHistogram() {
            analyzer.onEvent(new MayflyEvent.MaleUpdated(agent(0.0), true, 1.0));
            analyzer.onEvent(new MayflyEvent.MaleUpdated(agent(0.0), false, 1.0));
            analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(0.0), true, 1.0));
            analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(0.0), false, 1.0));

            assertThat(result().interactionHistogram())
                    .containsEntry(AgentInteractionAnalyzer.InteractionType.NUPTIAL_DANCE, 1)
                    .containsEntry(AgentInteractionAnalyzer.InteractionType.MALE_ATTRACTION, 1)
                    .containsEntry(AgentInteractionAnalyzer.InteractionType.FEMALE_ATTRACTION, 1)
                    .containsEntry(AgentInteractionAnalyzer.InteractionType.FEMALE_RANDOM_WALK, 1);
        }
    }

    @Nested
    class NumericalStability {
        @Test
        void handlesUnusualPreviousFitnessValues() {
            analyzer.onEvent(new MayflyEvent.MaleUpdated(agent(Double.NaN), true, Double.NaN));
            analyzer.onEvent(new MayflyEvent.FemaleUpdated(agent(Double.POSITIVE_INFINITY), false, Double.POSITIVE_INFINITY));

            assertThat(result().nuptialDanceCount()).isEqualTo(1);
            assertThat(result().interactionHistogram())
                    .containsEntry(AgentInteractionAnalyzer.InteractionType.FEMALE_RANDOM_WALK, 1);
        }
    }

    private AgentInteractionAnalyzer.AgentInteractionResult result() {
        return (AgentInteractionAnalyzer.AgentInteractionResult) analyzer.result();
    }

    private Mayfly agent(double fitness) {
        Mayfly agent = new Mayfly(2);
        agent.fitness = fitness;
        return agent;
    }
}
