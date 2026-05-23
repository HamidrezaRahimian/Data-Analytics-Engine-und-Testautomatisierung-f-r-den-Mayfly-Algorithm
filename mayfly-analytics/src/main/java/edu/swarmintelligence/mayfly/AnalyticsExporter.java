package edu.swarmintelligence.mayfly;

// AI-generated: Phase 2 reporting API, reviewed and accepted manually.
import java.io.IOException;
import java.io.Writer;

public interface AnalyticsExporter {
    void export(AnalyticsReport report, Writer out) throws IOException;
}
