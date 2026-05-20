package edu.swarmintelligence.mayfly;

import java.util.Arrays;

public record MayflyResult(double[] gbestPosition, double gbestFitness) {
    public MayflyResult {
        gbestPosition = gbestPosition != null ? Arrays.copyOf(gbestPosition, gbestPosition.length) : null;
    }
}
