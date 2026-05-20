package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalMemoryAnalyzer")
class GlobalMemoryAnalyzerTest {
    private GlobalMemoryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new GlobalMemoryAnalyzer();
    }

    @Nested
    class HappyPath {
        @Test
        void collectsGbestTrajectory() {
            complete(1, 10.0);
            complete(2, 8.0);

            assertThat(result().gbestTrajectory())
                    .extracting(GlobalMemoryAnalyzer.GbestPoint::fitness)
                    .containsExactly(10.0, 8.0);
        }

        @Test
        void countsUpdatesAndSourceDistribution() {
            analyzer.onEvent(new MayflyEvent.GbestUpdated(UpdateSource.MALE, 10.0, 8.0));
            analyzer.onEvent(new MayflyEvent.GbestUpdated(UpdateSource.OFFSPRING, 8.0, 4.0));

            var result = result();

            assertThat(result.gbestUpdateCount()).isEqualTo(2);
            assertThat(result.gbestUpdateSourceDistribution())
                    .containsEntry(UpdateSource.MALE, 0.5)
                    .containsEntry(UpdateSource.FEMALE, 0.0)
                    .containsEntry(UpdateSource.OFFSPRING, 0.5);
        }

        @Test
        void calculatesImprovementDelta() {
            complete(1, 10.0);
            complete(2, 7.5);
            complete(3, 6.0);

            assertThat(result().improvementDelta()).containsExactly(0.0, 2.5, 1.5);
        }

        @ParameterizedTest(name = "fitness {0} hits at iteration {1}")
        @CsvSource({
                "0.002, -1",
                "0.001, 2",
                "0.0005, 2"
        })
        void parameterizedFirstHittingIteration(double fitness, int expectedIteration) {
            complete(1, 1.0);
            complete(2, fitness);

            assertThat(result().firstHittingIteration()).isEqualTo(expectedIteration);
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void detectsStagnationStreaks() {
            complete(1, 5.0);
            complete(2, 5.0);
            complete(3, 4.0);
            complete(4, 4.0);
            complete(5, 4.0);

            assertThat(result().stagnationStreaks()).containsExactly(2, 2);
        }

        @Test
        void handlesNoUpdates() {
            var result = result();

            assertThat(result.gbestUpdateCount()).isZero();
            assertThat(result.gbestUpdateSourceDistribution().values()).containsOnly(0.0);
            assertThat(result.firstHittingIteration()).isEqualTo(-1);
        }
    }

    @Nested
    class NumericalStability {
        @Test
        void storesNaNAndInfinityConsistently() {
            complete(1, Double.POSITIVE_INFINITY);
            complete(2, Double.NaN);

            assertThat(result().gbestTrajectory())
                    .extracting(GlobalMemoryAnalyzer.GbestPoint::fitness)
                    .containsExactly(Double.POSITIVE_INFINITY, Double.NaN);
        }
    }

    private void complete(int iteration, double fitness) {
        analyzer.onEvent(new MayflyEvent.IterationCompleted(iteration, fitness, List.of()));
    }

    private GlobalMemoryAnalyzer.GlobalMemoryResult result() {
        return (GlobalMemoryAnalyzer.GlobalMemoryResult) analyzer.result();
    }
}
