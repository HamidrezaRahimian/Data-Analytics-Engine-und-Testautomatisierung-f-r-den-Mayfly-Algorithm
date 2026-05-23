package edu.swarmintelligence.mayfly;

public record MayflyConfig(
        int dimensions,
        double lowerBound,
        double upperBound,
        int populationSize,
        int maxIterations,
        double wMax,
        double wMin,
        double a1,
        double a2,
        double a3,
        double beta,
        double danceCoeff,
        double flightCoeff,
        double mutationStdDev
) {

    public MayflyConfig {
        if (dimensions <= 0) throw new IllegalArgumentException("dimensions > 0");
        if (!Double.isFinite(lowerBound)) throw new IllegalArgumentException("lowerBound must be finite");
        if (!Double.isFinite(upperBound)) throw new IllegalArgumentException("upperBound must be finite");
        if (lowerBound >= upperBound) throw new IllegalArgumentException("lowerBound < upperBound");
        if (populationSize <= 1) throw new IllegalArgumentException("populationSize > 1");
        if (maxIterations <= 0) throw new IllegalArgumentException("maxIterations > 0");
        if (!Double.isFinite(wMax)) throw new IllegalArgumentException("wMax must be finite");
        if (!Double.isFinite(wMin)) throw new IllegalArgumentException("wMin must be finite");
        if (wMin >= wMax) throw new IllegalArgumentException("wMin < wMax");
        if (!Double.isFinite(a1) || !Double.isFinite(a2) || !Double.isFinite(a3)
                || !Double.isFinite(beta) || !Double.isFinite(danceCoeff)
                || !Double.isFinite(flightCoeff) || !Double.isFinite(mutationStdDev)) {
            throw new IllegalArgumentException("coefficients must be finite");
        }
        if (a1 <= 0 || a2 <= 0 || a3 <= 0 || beta <= 0 || danceCoeff <= 0
                || flightCoeff <= 0 || mutationStdDev <= 0) {
            throw new IllegalArgumentException("coefficients must be > 0");
        }
    }

    public static MayflyConfig ackley10D() {
        return new MayflyConfig(
                10, -32.768, 32.768,
                40, 1000,
                0.9, 0.4,
                1.0, 1.5, 1.5,
                2.0,
                0.1, 0.1,
                0.01 * (32.768 + 32.768)
        );
    }
}
