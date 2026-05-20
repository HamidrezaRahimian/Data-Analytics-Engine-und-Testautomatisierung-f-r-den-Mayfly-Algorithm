package edu.swarmintelligence.mayfly;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsEngine implements MayflyEventListener {
    private final MayflyConfig config;
    private final long seed;
    private final List<MayflyAnalyzer> analyzers = new ArrayList<>();

    public AnalyticsEngine(MayflyConfig config, long seed) {
        this.config = config;
        this.seed = seed;
    }

    public void addAnalyzer(MayflyAnalyzer analyzer) {
        analyzers.add(analyzer);
    }

    @Override
    public void onEvent(MayflyEvent event) {
        for (MayflyAnalyzer analyzer : analyzers) {
            analyzer.onEvent(event);
        }
    }

    public AnalyticsReport report() {
        Map<String, AnalyzerResult> results = new LinkedHashMap<>();
        for (MayflyAnalyzer analyzer : analyzers) {
            results.put(analyzer.name(), analyzer.result());
        }
        return new AnalyticsReport(results, Instant.now(), config, seed);
    }
}
