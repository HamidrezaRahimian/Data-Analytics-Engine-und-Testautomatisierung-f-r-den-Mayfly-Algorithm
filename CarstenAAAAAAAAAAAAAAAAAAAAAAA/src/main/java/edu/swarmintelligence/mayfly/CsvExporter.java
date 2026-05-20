package edu.swarmintelligence.mayfly;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class CsvExporter implements AnalyticsExporter {
    @Override
    public void export(AnalyticsReport report, Writer out) throws IOException {
        write(out, "section", "key", "value");
        write(out, "config", "dimensions", report.config().dimensions());
        write(out, "config", "populationSize", report.config().populationSize());
        write(out, "config", "maxIterations", report.config().maxIterations());
        write(out, "run", "seed", report.seed());
        write(out, "run", "generatedAt", report.generatedAt());

        for (Map.Entry<String, AnalyzerResult> entry : report.byAnalyzer().entrySet()) {
            writeAnalyzer(out, entry.getKey(), entry.getValue());
        }
    }

    private void writeAnalyzer(Writer out, String name, AnalyzerResult result) throws IOException {
        write(out, "analyzer", name, result.getClass().getSimpleName());
        if (result instanceof AgentInteractionAnalyzer.AgentInteractionResult r) {
            write(out, name, "nuptialDanceCount", r.nuptialDanceCount());
            write(out, name, "attractionCount", r.attractionCount());
            write(out, name, "femaleAttractionRateCount", r.femaleAttractionRate().size());
            write(out, name, "interactionHistogram", r.interactionHistogram());
        } else if (result instanceof GlobalMemoryAnalyzer.GlobalMemoryResult r) {
            write(out, name, "gbestUpdateCount", r.gbestUpdateCount());
            write(out, name, "trajectoryCount", r.gbestTrajectory().size());
            write(out, name, "firstHittingIteration", r.firstHittingIteration());
            write(out, name, "sourceDistribution", r.gbestUpdateSourceDistribution());
        } else if (result instanceof LocalMemoryAnalyzer.LocalMemoryResult r) {
            int total = r.pbestUpdateCountPerAgent().values().stream().mapToInt(Integer::intValue).sum();
            write(out, name, "agentCountWithPbestUpdates", r.pbestUpdateCountPerAgent().size());
            write(out, name, "totalPbestUpdates", total);
            write(out, name, "meanPbestImprovement", r.meanPbestImprovement());
            write(out, name, "pbestPositionDiversityCount", r.pbestPositionDiversity().size());
        } else if (result instanceof ConvergenceAnalyzer.ConvergenceResult r) {
            write(out, name, "curveCount", r.convergenceCurve().size());
            write(out, name, "iterationsToThreshold", r.iterationsToThreshold());
            write(out, name, "plateauCount", r.plateauSegments().size());
            write(out, name, "convergenceRateEstimate", r.convergenceRateEstimate());
        }
    }

    private void write(Writer out, String section, String key, Object value) throws IOException {
        out.write(escape(section));
        out.write(',');
        out.write(escape(key));
        out.write(',');
        out.write(escape(String.valueOf(value)));
        out.write(System.lineSeparator());
    }

    private String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
