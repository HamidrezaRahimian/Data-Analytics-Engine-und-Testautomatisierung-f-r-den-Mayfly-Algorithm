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
        if (lowerBound >= upperBound) throw new IllegalArgumentException("lowerBound < upperBound");
        if (populationSize <= 1) throw new IllegalArgumentException("populationSize > 1");
        if (maxIterations <= 0) throw new IllegalArgumentException("maxIterations > 0");
        if (wMin >= wMax) throw new IllegalArgumentException("wMin < wMax");
        if (a1 <= 0 || a2 <= 0 || a3 <= 0 || beta <= 0 || danceCoeff <= 0 || flightCoeff <= 0 || mutationStdDev <= 0) {
            throw new IllegalArgumentException("Koeffizienten müssen > 0 sein");
        }
    }
    public static MayflyConfig ackley10D() {
        return new MayflyConfig(
                10, -32.768, 32.768,       // bounds
                40, 1000,                  // pop size, iterations
                0.9, 0.4,                  // inertia weights
                1.0, 1.5, 1.5,             // attraction coefficients
                2.0,                       // beta
                0.1, 0.1,                  // dance, flight
                0.01 * (32.768 + 32.768)   // mutation std (0.01 × range) as per paper
        );
    }
}