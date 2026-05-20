package edu.swarmintelligence.mayfly;

public class Main {
    static void main() {
        MayflyOptimizerConfig config = MayflyOptimizerConfig.ackley10D();
        MayflyOptimizer optimizer = new MayflyOptimizer(config);
        double best = optimizer.optimize();
        System.out.printf("Final Best Fitness: %.10f%n", best);
    }
}