package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 exporter tests, reviewed and accepted manually.
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonExporterTest {
    @Test
    void json_contains_expected_structure() throws Exception {
        StringWriter writer = new StringWriter();

        new JsonExporter().export(CsvExporterTest.report(), writer);

        assertThat(writer.toString())
                .startsWith("{")
                .endsWith("}")
                .contains("\"config\"")
                .contains("\"seed\":42")
                .contains("\"analyzers\"")
                .contains("\"agent-interaction\"");
    }

    @Test
    void json_escapes_strings() throws Exception {
        Map<String, AnalyzerResult> results = new LinkedHashMap<>();
        results.put("name\"with\nline", new AnalyzerResult() {
            @Override
            public String toString() {
                return "value\"with\nline";
            }
        });
        AnalyticsReport report = new AnalyticsReport(results, Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 7L);

        StringWriter writer = new StringWriter();
        new JsonExporter().export(report, writer);

        assertThat(writer.toString())
                .contains("name\\\"with\\nline")
                .contains("value\\\"with\\nline");
    }

    @Test
    void non_finite_numbers_are_written_as_null() throws Exception {
        Map<String, AnalyzerResult> results = Map.of("global-memory", new GlobalMemoryAnalyzer.GlobalMemoryResult(
                java.util.List.of(), 0, Map.of(UpdateSource.MALE, Double.NaN),
                java.util.List.of(Double.POSITIVE_INFINITY), java.util.List.of(), -1, 0.001));

        StringWriter writer = new StringWriter();
        new JsonExporter().export(new AnalyticsReport(results, Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 1L), writer);

        assertThat(writer.toString()).doesNotContain("NaN").doesNotContain("Infinity");
    }
}
