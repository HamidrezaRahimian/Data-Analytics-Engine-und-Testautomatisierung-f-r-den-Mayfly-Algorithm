package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiRunStatisticsTest {
    @Test
    void calculates_basic_statistics() {
        MultiRunStatistics.Statistics statistics = new MultiRunStatistics().calculate(List.of(1.0, 2.0, 3.0, 4.0));

        assertThat(statistics.mean()).isEqualTo(2.5);
        assertThat(statistics.median()).isEqualTo(2.5);
        assertThat(statistics.q25()).isEqualTo(1.75);
        assertThat(statistics.q75()).isEqualTo(3.25);
        assertThat(statistics.standardDeviation()).isCloseTo(1.290, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void calculates_confidence_interval() {
        MultiRunStatistics.Statistics statistics = new MultiRunStatistics().calculate(List.of(1.0, 2.0, 3.0, 4.0));

        assertThat(statistics.confidenceLow()).isLessThan(statistics.mean());
        assertThat(statistics.confidenceHigh()).isGreaterThan(statistics.mean());
    }

    @Test
    void empty_values_return_nan_result() {
        MultiRunStatistics.Statistics statistics = new MultiRunStatistics().calculate(List.of());

        assertThat(statistics.count()).isZero();
        assertThat(statistics.mean()).isNaN();
        assertThat(statistics.confidenceLow()).isNaN();
    }
}
