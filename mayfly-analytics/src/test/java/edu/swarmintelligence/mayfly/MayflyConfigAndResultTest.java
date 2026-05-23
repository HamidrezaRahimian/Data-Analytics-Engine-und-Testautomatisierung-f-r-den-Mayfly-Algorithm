package edu.swarmintelligence.mayfly;

// AI-generated: Final audit coverage tests, reviewed and accepted manually.
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MayflyConfig and MayflyResult")
class MayflyConfigAndResultTest {

    @Test
    void default_config_is_valid() {
        MayflyConfig config = MayflyConfig.ackley10D();

        assertThat(config.dimensions()).isEqualTo(10);
        assertThat(config.populationSize()).isEqualTo(40);
        assertThat(config.lowerBound()).isLessThan(config.upperBound());
    }

    @ParameterizedTest(name = "invalid config #{index}")
    @MethodSource("invalidConfigs")
    void invalid_config_values_are_rejected(Supplier<MayflyConfig> invalidConfig) {
        assertThatThrownBy(invalidConfig::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void result_defensively_copies_position_array() {
        double[] position = {1.0, 2.0};

        MayflyResult result = new MayflyResult(position, 0.5);
        position[0] = 99.0;
        double[] returned = result.gbestPosition();
        returned[1] = 88.0;

        assertThat(result.gbestPosition()).containsExactly(1.0, 2.0);
    }

    @Test
    void result_allows_null_position() {
        MayflyResult result = new MayflyResult(null, 1.0);

        assertThat(result.gbestPosition()).isNull();
    }

    static Stream<Supplier<MayflyConfig>> invalidConfigs() {
        return Stream.of(
                () -> config(0, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, Double.NaN, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, Double.POSITIVE_INFINITY, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, 1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 1, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 0, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, Double.NaN, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, Double.NaN, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.4, 0.9, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, Double.NaN, 1.0, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, Double.POSITIVE_INFINITY, 1.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 0.0, 1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, -1.0, 0.1, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.0, 0.1, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.0, 0.1),
                () -> config(1, -1.0, 1.0, 2, 1, 0.9, 0.4, 1.0, 1.0, 1.0, 1.0, 0.1, 0.1, 0.0)
        );
    }

    private static MayflyConfig config(int dimensions, double lowerBound, double upperBound,
                                       int populationSize, int maxIterations,
                                       double wMax, double wMin,
                                       double a1, double a2, double a3,
                                       double beta, double danceCoeff,
                                       double flightCoeff, double mutationStdDev) {
        return new MayflyConfig(dimensions, lowerBound, upperBound, populationSize, maxIterations,
                wMax, wMin, a1, a2, a3, beta, danceCoeff, flightCoeff, mutationStdDev);
    }
}
