package edu.swarmintelligence.mayfly;

public class Main {
    public static void main(String[] args) {
        MayflyConfig config = MayflyConfig.ackley10D();
        MayflyAlgorithm algorithm = new MayflyAlgorithm();

        // Konsolenausgaben aus MayflyAlgorithm isoliert und hier als Listener verankert
        algorithm.addListener(e -> {
            if (e instanceof MayflyEvent.RunStarted rs) {
                System.out.printf("Mayfly Algorithm (%d-D) - Initial Best: %.10f%n",
                        rs.dimensions(), rs.initialGbestFitness());
            } else if (e instanceof MayflyEvent.IterationCompleted ic) {
                if (ic.iteration() % 100 == 0 || ic.iteration() == config.maxIterations()) {
                    System.out.printf("Iter %4d | Best Fitness: %.10f%n",
                            ic.iteration(), ic.gbestFitness());
                }
            }
        });

        long seed = 42L;
        MayflyResult result = algorithm.run(config, seed);

        System.out.printf("Final Best Fitness: %.10f%n", result.gbestFitness());
    }
}