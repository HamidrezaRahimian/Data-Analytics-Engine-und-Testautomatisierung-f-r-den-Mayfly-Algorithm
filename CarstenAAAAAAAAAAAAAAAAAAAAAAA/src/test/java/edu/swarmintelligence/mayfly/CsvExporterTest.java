package edu.swarmintelligence.mayfly;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {
    @Test
    void csv_contains_sections_and_values() throws Exception {
        StringWriter writer = new StringWriter();

        new CsvExporter().export(report(), writer);

        assertThat(writer.toString())
                .contains("section,key,value")
                .contains("config,dimensions,10")
                .contains("run,seed,42")
                .contains("agent-interaction,nuptialDanceCount,2")
                .contains("global-memory,gbestUpdateCount,3");
    }

    @Test
    void minimal_report_does_not_crash() throws Exception {
        StringWriter writer = new StringWriter();
        AnalyticsReport report = new AnalyticsReport(Map.of(), Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 1L);

        new CsvExporter().export(report, writer);

        assertThat(writer.toString()).contains("config,dimensions,10");
    }

    static AnalyticsReport report() {
        Map<String, AnalyzerResult> results = new LinkedHashMap<>();
        results.put("agent-interaction", new AgentInteractionAnalyzer.AgentInteractionResult(
                2, 4, List.of(0.5), List.of(1.0), List.of(2.0), Map.of()));
        results.put("global-memory", new GlobalMemoryAnalyzer.GlobalMemoryResult(
                List.of(new GlobalMemoryAnalyzer.GbestPoint(1, 5.0)),
                3, Map.of(UpdateSource.MALE, 1.0), List.of(0.0), List.of(1), -1, 0.001));
        return new AnalyticsReport(results, Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 42L);
    }
}
