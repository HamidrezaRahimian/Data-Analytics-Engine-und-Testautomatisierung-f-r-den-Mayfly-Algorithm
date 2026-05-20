package edu.swarmintelligence.mayfly;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class MayflyOptimizer {
    private final MayflyOptimizerConfig config;
    private final double[] gbestPosition;
    private final RandomGenerator rng;
    @Getter
    private double gbestFitness = Double.POSITIVE_INFINITY;

    public MayflyOptimizer(MayflyOptimizerConfig config) {
        this.config = config;
        this.gbestPosition = new double[config.dimensions()];
        this.rng = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create(42L);
    }

    /**
     * Runs the algorithm and returns the best fitness found.
     */
    public double optimize() {
        List<Mayfly> males = initializePopulation();
        List<Mayfly> females = initializePopulation();

        // Synchronous global best from initial populations
        males.forEach(m -> updateGlobalBest(m.fitness, m.pos));
        females.forEach(f -> updateGlobalBest(f.fitness, f.pos));

        System.out.printf("Mayfly Algorithm (%d-D) - Initial Best: %.10f%n",
                config.dimensions(), gbestFitness);

        Comparator<Mayfly> byFitness = Comparator.comparingDouble(m -> m.fitness);

        for (int iter = 1; iter <= config.maxIterations(); iter++) {
            double w = inertiaWeight(iter);

            // ----- 1. Male movement (synchronous) -----
            moveMales(males, w);

            // Re‑rank males for female movement and mating
            List<Mayfly> sortedMales = sortByFitness(males);

            // ----- 2. Female movement (synchronous) -----
            moveFemales(females, sortedMales, w);

            // Re‑sort both populations after female movement
            sortedMales = sortByFitness(males);
            List<Mayfly> sortedFemales = sortByFitness(females);

            // ----- 3. Mating -----
            List<Mayfly> offspring = mate(sortedMales, sortedFemales);

            // ----- 4. Global selection (Zervoudakis & Tsafarakis) -----
            List<Mayfly> pool = new ArrayList<>(config.populationSize() * 4);
            pool.addAll(males);
            pool.addAll(females);
            pool.addAll(offspring);
            pool.sort(byFitness);

            males = new ArrayList<>(pool.subList(0, config.populationSize()));
            females = new ArrayList<>(pool.subList(config.populationSize(), 2 * config.populationSize()));

            if (iter % 100 == 0 || iter == config.maxIterations()) {
                System.out.printf("Iter %4d | Best Fitness: %.10f%n", iter, gbestFitness);
            }
        }
        return gbestFitness;
    }

    // ---------- Initialization ----------
    private List<Mayfly> initializePopulation() {
        List<Mayfly> pop = new ArrayList<>(config.populationSize());
        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly m = new Mayfly(config.dimensions());
            for (int d = 0; d < config.dimensions(); d++) {
                m.pos[d] = rng.nextDouble(config.lowerBound(), config.upperBound());
            }
            m.fitness = evaluate(m.pos);
            m.updatePersonalBest();
            pop.add(m);
        }
        return pop;
    }

    // ---------- Movement ----------
    private void moveMales(List<Mayfly> males, double w) {
        Mayfly bestMale = males.stream().min(Comparator.comparingDouble(m -> m.fitness)).orElseThrow();

        // Compute new positions/velocities in temporary arrays (synchronous update)
        double[][] newPos = new double[males.size()][config.dimensions()];
        double[][] newVel = new double[males.size()][config.dimensions()];

        for (int i = 0; i < males.size(); i++) {
            Mayfly male = males.get(i);
            double dP2 = squaredDistance(male.pos, male.pbestPos);
            double dG2 = squaredDistance(male.pos, gbestPosition);

            for (int d = 0; d < config.dimensions(); d++) {
                double vel;
                if (male == bestMale) {
                    vel = w * male.vel[d] + config.danceCoeff() * rng.nextDouble(-1.0, 1.0);
                } else {
                    vel = w * male.vel[d]
                            + config.a1() * Math.exp(-config.beta() * dP2) * (male.pbestPos[d] - male.pos[d])
                            + config.a2() * Math.exp(-config.beta() * dG2) * (gbestPosition[d] - male.pos[d]);
                }
                newVel[i][d] = vel;
                newPos[i][d] = clamp(male.pos[d] + vel);
            }
        }

        // Apply and evaluate
        for (int i = 0; i < males.size(); i++) {
            Mayfly male = males.get(i);
            System.arraycopy(newPos[i], 0, male.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, male.vel, 0, config.dimensions());
            male.fitness = evaluate(male.pos);
            male.updatePersonalBest();
            updateGlobalBest(male.fitness, male.pos);
        }
    }

    private void moveFemales(List<Mayfly> females, List<Mayfly> sortedMales, double w) {
        // Sort females by current fitness (ascending)
        List<Mayfly> sortedFemales = sortByFitness(females);

        double[][] newPos = new double[config.populationSize()][config.dimensions()];
        double[][] newVel = new double[config.populationSize()][config.dimensions()];

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly female = sortedFemales.get(i);
            Mayfly male = sortedMales.get(i);   // pairing by rank
            boolean attracted = male.fitness < female.fitness;

            double dMF2 = attracted ? squaredDistance(female.pos, male.pos) : 0;

            for (int d = 0; d < config.dimensions(); d++) {
                double vel;
                if (attracted) {
                    vel = w * female.vel[d]
                            + config.a3() * Math.exp(-config.beta() * dMF2) * (male.pos[d] - female.pos[d]);
                } else {
                    vel = w * female.vel[d] + config.flightCoeff() * rng.nextDouble(-1.0, 1.0);
                }
                newVel[i][d] = vel;
                newPos[i][d] = clamp(female.pos[d] + vel);
            }
        }

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly female = sortedFemales.get(i);
            System.arraycopy(newPos[i], 0, female.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, female.vel, 0, config.dimensions());
            female.fitness = evaluate(female.pos);
            female.updatePersonalBest();
            updateGlobalBest(female.fitness, female.pos);
        }
    }

    // ---------- Mating ----------
    private List<Mayfly> mate(List<Mayfly> sortedMales, List<Mayfly> sortedFemales) {
        List<Mayfly> offspring = new ArrayList<>(config.populationSize() * 2);

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly p1 = sortedMales.get(i);
            Mayfly p2 = sortedFemales.get(i);
            double L = rng.nextDouble();

            Mayfly child1 = new Mayfly(config.dimensions());
            Mayfly child2 = new Mayfly(config.dimensions());

            for (int d = 0; d < config.dimensions(); d++) {
                child1.pos[d] = L * p1.pos[d] + (1.0 - L) * p2.pos[d];
                child2.pos[d] = L * p2.pos[d] + (1.0 - L) * p1.pos[d];
            }

            // Mutation: always applied to all dimensions of all offspring
            applyMutation(child1);
            applyMutation(child2);

            updateGlobalBest(child1.fitness, child1.pos);
            updateGlobalBest(child2.fitness, child2.pos);
            offspring.add(child1);
            offspring.add(child2);
        }
        return offspring;
    }

    private void applyMutation(Mayfly m) {
        for (int d = 0; d < config.dimensions(); d++) {
            m.pos[d] += rng.nextGaussian() * config.mutationStdDev();
            m.pos[d] = clamp(m.pos[d]);
        }
        m.fitness = evaluate(m.pos);
        m.updatePersonalBest();
    }

    // ---------- Helpers ----------
    private double inertiaWeight(int iter) {
        return config.wMax() - (config.wMax() - config.wMin()) * ((double) iter / config.maxIterations());
    }

    private double clamp(double value) {
        return Math.clamp(value, config.lowerBound(), config.upperBound());
    }

    private double squaredDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    private double evaluate(double[] x) {
        double s1 = 0, s2 = 0;
        for (double v : x) {
            s1 += v * v;
            s2 += Math.cos(2 * Math.PI * v);
        }
        return -20.0 * Math.exp(-0.2 * Math.sqrt(s1 / config.dimensions()))
                - Math.exp(s2 / config.dimensions()) + 20.0 + Math.E;
    }

    private void updateGlobalBest(double fitness, double[] pos) {
        if (fitness < gbestFitness) {
            gbestFitness = fitness;
            System.arraycopy(pos, 0, gbestPosition, 0, config.dimensions());
        }
    }

    private List<Mayfly> sortByFitness(List<Mayfly> list) {
        return list.stream()
                .sorted(Comparator.comparingDouble(m -> m.fitness))
                .toList();
    }
}