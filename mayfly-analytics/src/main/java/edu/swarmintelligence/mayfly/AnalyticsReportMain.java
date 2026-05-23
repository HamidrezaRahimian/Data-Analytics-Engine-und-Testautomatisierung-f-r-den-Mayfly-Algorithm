package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 report generation entry point, reviewed and accepted manually.
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsReportMain {
    public static void main(String[] args) throws IOException {
        MayflyConfig config = MayflyConfig.ackley10D();
        long seed = 42L;
        AnalyticsReport report = runWithAnalytics(config, seed);

        List<Double> finalFitnessValues = new ArrayList<>();
        for (long currentSeed = 1; currentSeed <= 10; currentSeed++) {
            finalFitnessValues.add(new MayflyAlgorithm().run(config, currentSeed).gbestFitness());
        }
        MultiRunStatistics.Statistics statistics = new MultiRunStatistics().calculate(finalFitnessValues);

        Files.createDirectories(Path.of("docs"));
        Files.writeString(Path.of("docs", "analytics-report.md"),
                new MarkdownReportGenerator().generate(report, statistics));

        Files.createDirectories(Path.of("target", "analytics"));
        writeExport(Path.of("target", "analytics", "analytics-report.csv"), new CsvExporter(), report);
        writeExport(Path.of("target", "analytics", "analytics-report.json"), new JsonExporter(), report);
    }

    private static AnalyticsReport runWithAnalytics(MayflyConfig config, long seed) {
        AnalyticsEngine engine = new AnalyticsEngine(config, seed);
        engine.addAnalyzer(new AgentInteractionAnalyzer());
        engine.addAnalyzer(new GlobalMemoryAnalyzer());
        engine.addAnalyzer(new LocalMemoryAnalyzer());
        engine.addAnalyzer(new ConvergenceAnalyzer());

        MayflyAlgorithm algorithm = new MayflyAlgorithm();
        algorithm.addListener(engine);
        algorithm.run(config, seed);
        return engine.report();
    }

    private static void writeExport(Path path, AnalyticsExporter exporter, AnalyticsReport report) throws IOException {
        StringWriter writer = new StringWriter();
        exporter.export(report, writer);
        Files.writeString(path, writer.toString());
    }
}
