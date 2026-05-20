package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 report generator tests, reviewed and accepted manually.
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportGeneratorTest {
    @Test
    void markdown_contains_main_report_parts() {
        String markdown = new MarkdownReportGenerator().generate(report(),
                new MultiRunStatistics().calculate(List.of(0.1, 0.2, 0.3)));

        assertThat(markdown)
                .contains("# Mayfly Analytics Report")
                .contains("## Configuration")
                .contains("## agent-interaction")
                .contains("## global-memory")
                .contains("## local-memory")
                .contains("## convergence")
                .contains("| Metric | Value |")
                .contains("```mermaid")
                .contains("Gbest sparkline")
                .contains("## Multi-run Statistics");
    }

    @Test
    void sparkline_uses_unicode_blocks() {
        String sparkline = new MarkdownReportGenerator().sparkline(List.of(1.0, 2.0, 3.0));

        assertThat(sparkline).contains("▁").contains("█");
    }

    @Test
    void minimal_report_does_not_crash() {
        AnalyticsReport report = new AnalyticsReport(Map.of(), Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 1L);

        String markdown = new MarkdownReportGenerator().generate(report, null);

        assertThat(markdown).contains("# Mayfly Analytics Report").contains("```mermaid");
    }

    private AnalyticsReport report() {
        Map<String, AnalyzerResult> results = new LinkedHashMap<>();
        results.put("agent-interaction", new AgentInteractionAnalyzer.AgentInteractionResult(
                1, 2, List.of(0.5, 0.6), List.of(3.0), List.of(4.0), Map.of()));
        results.put("global-memory", new GlobalMemoryAnalyzer.GlobalMemoryResult(
                List.of(new GlobalMemoryAnalyzer.GbestPoint(1, 5.0),
                        new GlobalMemoryAnalyzer.GbestPoint(2, 2.0)),
                2, Map.of(UpdateSource.MALE, 1.0), List.of(0.0, 3.0), List.of(1), -1, 0.001));
        results.put("local-memory", new LocalMemoryAnalyzer.LocalMemoryResult(
                Map.of(new Mayfly(2), 1), 0.2, List.of(1.0), List.of()));
        results.put("convergence", new ConvergenceAnalyzer.ConvergenceResult(
                List.of(new ConvergenceAnalyzer.ConvergencePoint(1, 5.0),
                        new ConvergenceAnalyzer.ConvergencePoint(2, 2.0)),
                List.of(2.0, 1.0), -1, 0.001, List.of(), 0.0, 5, -0.1));
        return new AnalyticsReport(results, Instant.parse("2026-01-01T00:00:00Z"),
                MayflyConfig.ackley10D(), 42L);
    }
}
