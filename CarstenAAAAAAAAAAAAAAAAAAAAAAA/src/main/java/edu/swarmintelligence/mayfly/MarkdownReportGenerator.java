package edu.swarmintelligence.mayfly;

import java.util.List;
import java.util.Map;

public class MarkdownReportGenerator {
    private static final char[] SPARKLINE = {'▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

    public String generate(AnalyticsReport report, MultiRunStatistics.Statistics statistics) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Mayfly Analytics Report\n\n");
        markdown.append("## Run\n\n");
        markdown.append("| Key | Value |\n");
        markdown.append("| --- | --- |\n");
        row(markdown, "Seed", report.seed());
        row(markdown, "Generated at", report.generatedAt());
        row(markdown, "Java version", System.getProperty("java.version"));
        row(markdown, "JUnit", "5");
        row(markdown, "AssertJ", "configured");
        row(markdown, "JGiven", "configured");
        row(markdown, "JaCoCo", "configured");

        markdown.append("\n## Configuration\n\n");
        markdown.append("| Key | Value |\n");
        markdown.append("| --- | --- |\n");
        MayflyConfig config = report.config();
        row(markdown, "Dimensions", config.dimensions());
        row(markdown, "Bounds", config.lowerBound() + " to " + config.upperBound());
        row(markdown, "Population size", config.populationSize());
        row(markdown, "Max iterations", config.maxIterations());
        row(markdown, "Inertia", config.wMin() + " to " + config.wMax());

        for (Map.Entry<String, AnalyzerResult> entry : report.byAnalyzer().entrySet()) {
            analyzerSection(markdown, entry.getKey(), entry.getValue());
        }

        if (statistics != null) {
            markdown.append("\n## Multi-run Statistics\n\n");
            markdown.append("| Metric | Value |\n");
            markdown.append("| --- | --- |\n");
            row(markdown, "Count", statistics.count());
            row(markdown, "Mean", statistics.mean());
            row(markdown, "Median", statistics.median());
            row(markdown, "Standard deviation", statistics.standardDeviation());
            row(markdown, "Q25", statistics.q25());
            row(markdown, "Q75", statistics.q75());
            row(markdown, "95% confidence interval",
                    statistics.confidenceLow() + " to " + statistics.confidenceHigh());
        }

        markdown.append("\n## Convergence Diagram\n\n");
        markdown.append("```mermaid\n");
        markdown.append("xychart-beta\n");
        markdown.append("    title \"Convergence\"\n");
        markdown.append("    x-axis \"step\" [");
        List<Double> curve = convergenceFitness(report);
        for (int i = 0; i < Math.min(10, curve.size()); i++) {
            if (i > 0) {
                markdown.append(", ");
            }
            markdown.append(i + 1);
        }
        markdown.append("]\n");
        markdown.append("    y-axis \"fitness\"\n");
        markdown.append("    line [");
        for (int i = 0; i < Math.min(10, curve.size()); i++) {
            if (i > 0) {
                markdown.append(", ");
            }
            markdown.append(curve.get(i));
        }
        markdown.append("]\n");
        markdown.append("```\n");
        return markdown.toString();
    }

    public String sparkline(List<Double> values) {
        if (values.isEmpty()) {
            return "";
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (Double.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        if (min == Double.POSITIVE_INFINITY) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (double value : values) {
            if (!Double.isFinite(value)) {
                continue;
            }
            int index = max == min ? 0 : (int) Math.round((value - min) / (max - min) * (SPARKLINE.length - 1));
            text.append(SPARKLINE[index]);
        }
        return text.toString();
    }

    private void analyzerSection(StringBuilder markdown, String name, AnalyzerResult result) {
        markdown.append("\n## ").append(name).append("\n\n");
        markdown.append("| Metric | Value |\n");
        markdown.append("| --- | --- |\n");
        if (result instanceof AgentInteractionAnalyzer.AgentInteractionResult r) {
            row(markdown, "Nuptial dances", r.nuptialDanceCount());
            row(markdown, "Male attraction updates", r.attractionCount());
            row(markdown, "Female attraction rate", summarize(r.femaleAttractionRate()));
            row(markdown, "Mean pair distance", summarize(r.meanPairDistance()));
            row(markdown, "Pair fitness gap", summarize(r.pairFitnessGap()));
        } else if (result instanceof GlobalMemoryAnalyzer.GlobalMemoryResult r) {
            row(markdown, "Gbest updates", r.gbestUpdateCount());
            row(markdown, "First hitting iteration", r.firstHittingIteration());
            row(markdown, "Stagnation streak count", r.stagnationStreaks().size());
            row(markdown, "Gbest sparkline", sparkline(gbestFitness(r.gbestTrajectory())));
        } else if (result instanceof LocalMemoryAnalyzer.LocalMemoryResult r) {
            int total = r.pbestUpdateCountPerAgent().values().stream().mapToInt(Integer::intValue).sum();
            row(markdown, "Agents with pbest updates", r.pbestUpdateCountPerAgent().size());
            row(markdown, "Total pbest updates", total);
            row(markdown, "Mean pbest improvement", r.meanPbestImprovement());
            row(markdown, "Pbest diversity", summarize(r.pbestPositionDiversity()));
        } else if (result instanceof ConvergenceAnalyzer.ConvergenceResult r) {
            row(markdown, "Iterations to threshold", r.iterationsToThreshold());
            row(markdown, "Plateau segments", r.plateauSegments().size());
            row(markdown, "Convergence rate estimate", r.convergenceRateEstimate());
            row(markdown, "Population diversity", summarize(r.populationDiversity()));
        }
    }

    private List<Double> gbestFitness(List<GlobalMemoryAnalyzer.GbestPoint> points) {
        return points.stream().map(GlobalMemoryAnalyzer.GbestPoint::fitness).toList();
    }

    private List<Double> convergenceFitness(AnalyticsReport report) {
        AnalyzerResult result = report.byAnalyzer().get("convergence");
        if (result instanceof ConvergenceAnalyzer.ConvergenceResult convergence) {
            return convergence.convergenceCurve().stream().map(ConvergenceAnalyzer.ConvergencePoint::fitness).toList();
        }
        return List.of();
    }

    private String summarize(List<Double> values) {
        if (values.isEmpty()) {
            return "count=0";
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (Double.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        return "count=" + values.size() + ", first=" + values.getFirst()
                + ", last=" + values.getLast() + ", min=" + min + ", max=" + max;
    }

    private void row(StringBuilder markdown, String key, Object value) {
        markdown.append("| ").append(key).append(" | ").append(value).append(" |\n");
    }
}
