package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 JSON exporter, reviewed and accepted manually.
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonExporter implements AnalyticsExporter {
    @Override
    public void export(AnalyticsReport report, Writer out) throws IOException {
        out.write(toJson(reportData(report)));
    }

    private Map<String, Object> reportData(AnalyticsReport report) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("config", configData(report.config()));
        root.put("seed", report.seed());
        root.put("generatedAt", report.generatedAt().toString());

        Map<String, Object> analyzers = new LinkedHashMap<>();
        for (Map.Entry<String, AnalyzerResult> entry : report.byAnalyzer().entrySet()) {
            analyzers.put(entry.getKey(), analyzerData(entry.getValue()));
        }
        root.put("analyzers", analyzers);
        return root;
    }

    private Map<String, Object> configData(MayflyConfig config) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dimensions", config.dimensions());
        data.put("lowerBound", config.lowerBound());
        data.put("upperBound", config.upperBound());
        data.put("populationSize", config.populationSize());
        data.put("maxIterations", config.maxIterations());
        data.put("wMax", config.wMax());
        data.put("wMin", config.wMin());
        return data;
    }

    private Object analyzerData(AnalyzerResult result) {
        if (result instanceof AgentInteractionAnalyzer.AgentInteractionResult r) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("nuptialDanceCount", r.nuptialDanceCount());
            data.put("attractionCount", r.attractionCount());
            data.put("femaleAttractionRate", r.femaleAttractionRate());
            data.put("meanPairDistance", summary(r.meanPairDistance()));
            data.put("pairFitnessGap", summary(r.pairFitnessGap()));
            data.put("interactionHistogram", enumMap(r.interactionHistogram()));
            return data;
        }
        if (result instanceof GlobalMemoryAnalyzer.GlobalMemoryResult r) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("gbestUpdateCount", r.gbestUpdateCount());
            data.put("gbestUpdateSourceDistribution", enumMap(r.gbestUpdateSourceDistribution()));
            data.put("improvementDelta", summary(r.improvementDelta()));
            data.put("stagnationStreaks", r.stagnationStreaks());
            data.put("firstHittingIteration", r.firstHittingIteration());
            data.put("gbestTrajectory", gbestPoints(r.gbestTrajectory()));
            return data;
        }
        if (result instanceof LocalMemoryAnalyzer.LocalMemoryResult r) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("agentCountWithPbestUpdates", r.pbestUpdateCountPerAgent().size());
            data.put("totalPbestUpdates", r.pbestUpdateCountPerAgent().values().stream().mapToInt(Integer::intValue).sum());
            data.put("meanPbestImprovement", r.meanPbestImprovement());
            data.put("pbestPositionDiversity", summary(r.pbestPositionDiversity()));
            data.put("lastPbestFitnessDistribution", lastDistribution(r.pbestFitnessDistribution()));
            return data;
        }
        if (result instanceof ConvergenceAnalyzer.ConvergenceResult r) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("iterationsToThreshold", r.iterationsToThreshold());
            data.put("threshold", r.threshold());
            data.put("plateauCount", r.plateauSegments().size());
            data.put("convergenceRateEstimate", r.convergenceRateEstimate());
            data.put("populationDiversity", summary(r.populationDiversity()));
            data.put("convergenceCurve", convergencePoints(r.convergenceCurve()));
            return data;
        }
        return String.valueOf(result);
    }

    private Map<String, Object> summary(List<Double> values) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", values.size());
        if (values.isEmpty()) {
            data.put("min", null);
            data.put("max", null);
            data.put("first", List.of());
            data.put("last", List.of());
            return data;
        }
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (Double.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        data.put("min", min == Double.POSITIVE_INFINITY ? null : min);
        data.put("max", max == Double.NEGATIVE_INFINITY ? null : max);
        data.put("first", values.subList(0, Math.min(5, values.size())));
        data.put("last", values.subList(Math.max(0, values.size() - 5), values.size()));
        return data;
    }

    private Map<String, Object> enumMap(Map<?, ?> source) {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            data.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return data;
    }

    private List<Map<String, Object>> gbestPoints(List<GlobalMemoryAnalyzer.GbestPoint> points) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (GlobalMemoryAnalyzer.GbestPoint point : points) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("iteration", point.iteration());
            row.put("fitness", point.fitness());
            data.add(row);
        }
        return data;
    }

    private List<Map<String, Object>> convergencePoints(List<ConvergenceAnalyzer.ConvergencePoint> points) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (ConvergenceAnalyzer.ConvergencePoint point : points) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("iteration", point.iteration());
            row.put("fitness", point.fitness());
            data.add(row);
        }
        return data;
    }

    private Object lastDistribution(List<LocalMemoryAnalyzer.FitnessDistribution> distributions) {
        if (distributions.isEmpty()) {
            return null;
        }
        LocalMemoryAnalyzer.FitnessDistribution d = distributions.getLast();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("min", d.min());
        data.put("q25", d.q25());
        data.put("median", d.median());
        data.put("q75", d.q75());
        data.put("max", d.max());
        return data;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escape(text) + "\"";
        }
        if (value instanceof Double number && !Double.isFinite(number)) {
            return "null";
        }
        if (value instanceof Float number && !Float.isFinite(number)) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                json.append(toJson(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
                first = false;
            }
            return json.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (Object item : values) {
                if (!first) {
                    json.append(',');
                }
                json.append(toJson(item));
                first = false;
            }
            return json.append(']').toString();
        }
        return toJson(String.valueOf(value));
    }

    private String escape(String text) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 32) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
