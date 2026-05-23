package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConvergenceAnalyzer")
class ConvergenceAnalyzerTest {
    private ConvergenceAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ConvergenceAnalyzer();
    }

    @Nested
    class HappyPath {
        @Test
        void collectsConvergenceCurve() {
            complete(1, 10.0);
            complete(2, 5.0);

            assertThat(result().convergenceCurve())
                    .extracting(ConvergenceAnalyzer.ConvergencePoint::fitness)
                    .containsExactly(10.0, 5.0);
        }

        @Test
        void calculatesPopulationDiversityFromSurvivors() {
            complete(1, 1.0, List.of(agent(0.0, 0.0), agent(3.0, 4.0)));

            assertThat(result().populationDiversity()).containsExactly(5.0);
        }

        @Test
        void findsIterationsToThreshold() {
            complete(1, 1.0);
            complete(2, 0.0005);

            assertThat(result().iterationsToThreshold()).isEqualTo(2);
        }

        @Test
        void estimatesConvergenceRate() {
            complete(1, 10.0);
            complete(2, 5.0);
            complete(3, 2.5);
            complete(4, 1.25);
            complete(5, 0.625);

            assertThat(result().convergenceRateEstimate()).isLessThan(0.0);
        }

        @ParameterizedTest(name = "delta={0}, length={1}")
        @MethodSource("edu.swarmintelligence.mayfly.ConvergenceAnalyzerTest#plateauParameters")
        void parameterizedPlateauDetection(double delta, int length) {
            analyzer = new ConvergenceAnalyzer(0.001, delta, length);
            complete(1, 4.0);
            complete(2, 4.0);
            complete(3, 4.0);
            complete(4, 4.0);

            assertThat(result().plateauSegments()).hasSize(1);
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void detectsConstantFitnessPlateau() {
            complete(1, 3.0);
            complete(2, 3.0);
            complete(3, 3.0);
            complete(4, 3.0);
            complete(5, 3.0);

            assertThat(result().plateauSegments())
                    .containsExactly(new ConvergenceAnalyzer.PlateauSegment(1, 5, 5));
        }

        @Test
        void handlesSingleSurvivorDiversity() {
            complete(1, 1.0, List.of(agent(1.0, 1.0)));

            assertThat(result().populationDiversity()).containsExactly(0.0);
        }
    }

    @Nested
    class NumericalStability {
        @Test
        void handlesInvalidValuesInRateEstimate() {
            complete(1, 0.0);
            complete(2, -1.0);
            complete(3, Double.NaN);
            complete(4, Double.POSITIVE_INFINITY);

            assertThat(result().convergenceRateEstimate()).isFinite();
        }
    }

    static Stream<Arguments> plateauParameters() {
        return Stream.of(
                Arguments.of(0.0, 4),
                Arguments.of(0.01, 3)
        );
    }

    private void complete(int iteration, double fitness) {
        complete(iteration, fitness, List.of());
    }

    private void complete(int iteration, double fitness, List<Mayfly> survivors) {
        analyzer.onEvent(new MayflyEvent.IterationCompleted(iteration, fitness, survivors));
    }

    private ConvergenceAnalyzer.ConvergenceResult result() {
        return (ConvergenceAnalyzer.ConvergenceResult) analyzer.result();
    }

    private Mayfly agent(double first, double second) {
        Mayfly agent = new Mayfly(2);
        agent.pos[0] = first;
        agent.pos[1] = second;
        return agent;
    }
}
