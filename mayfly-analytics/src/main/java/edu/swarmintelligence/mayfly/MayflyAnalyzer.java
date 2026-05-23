package edu.swarmintelligence.mayfly;

public interface MayflyAnalyzer extends MayflyEventListener {
    AnalyzerResult result();

    String name();
}
