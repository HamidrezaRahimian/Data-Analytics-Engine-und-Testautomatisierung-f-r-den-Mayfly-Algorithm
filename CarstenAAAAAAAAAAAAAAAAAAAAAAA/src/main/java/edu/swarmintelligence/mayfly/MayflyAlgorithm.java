package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class MayflyAlgorithm {
    private final List<MayflyEventListener> listeners = new ArrayList<>();

    public void addListener(MayflyEventListener l) {
        listeners.add(l);
    }

    private void fireEvent(MayflyEvent e) {
        for (MayflyEventListener l : listeners) {
            l.onEvent(e);
        }
    }

    public MayflyResult run(MayflyConfig config, long seed) {
        RandomGenerator rng = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create(seed);

        // Lokaler State der Optimierung
        double[] gbestPosition = new double[config.dimensions()];
        double[] gbestFitness = { Double.POSITIVE_INFINITY }; // Array als Container für Pass-by-Reference in Helper-Methoden

        List<Mayfly> males = initializePopulation(config, rng);
        List<Mayfly> females = initializePopulation(config, rng);

        // Initiale Gbest-Bestimmung (löst gemäß Anforderung absichtlich kein GbestUpdated-Event aus!)
        males.forEach(m -> updateGlobalBestInitial(m.fitness, m.pos, gbestFitness, gbestPosition, config));
        females.forEach(f -> updateGlobalBestInitial(f.fitness, f.pos, gbestFitness, gbestPosition, config));

        // Ermöglicht das externe Logging des Initial-Werts ohne Sysout in dieser Klasse
        fireEvent(new MayflyEvent.RunStarted(config.dimensions(), gbestFitness[0]));

        Comparator<Mayfly> byFitness = Comparator.comparingDouble(m -> m.fitness);

        for (int iter = 1; iter <= config.maxIterations(); iter++) {
            double w = inertiaWeight(iter, config);
            fireEvent(new MayflyEvent.IterationStarted(iter, w));

            // ----- 1. Male movement -----
            moveMales(males, w, config, rng, gbestPosition, gbestFitness);

            // Re‑rank males
            List<Mayfly> sortedMales = sortByFitness(males, byFitness);

            // ----- 2. Female movement -----
            moveFemales(females, sortedMales, w, config, rng, gbestPosition, gbestFitness);

            // Re‑sort both populations
            sortedMales = sortByFitness(males, byFitness);
            List<Mayfly> sortedFemales = sortByFitness(females, byFitness);

            // ----- 3. Mating -----
            List<Mayfly> offspring = mate(sortedMales, sortedFemales, config, rng, gbestPosition, gbestFitness);

            // ----- 4. Global selection (Zervoudakis & Tsafarakis) -----
            List<Mayfly> pool = new ArrayList<>(config.populationSize() * 4);
            pool.addAll(males);
            pool.addAll(females);
            pool.addAll(offspring);
            pool.sort(byFitness);

            males = new ArrayList<>(pool.subList(0, config.populationSize()));
            females = new ArrayList<>(pool.subList(config.populationSize(), 2 * config.populationSize()));

            List<Mayfly> survivors = new ArrayList<>(config.populationSize() * 2);
            survivors.addAll(males);
            survivors.addAll(females);

            fireEvent(new MayflyEvent.IterationCompleted(iter, gbestFitness[0], survivors));
        }

        MayflyResult result = new MayflyResult(gbestPosition, gbestFitness[0]);
        fireEvent(new MayflyEvent.RunCompleted(result));
        return result;
    }

    // ---------- Initialization ----------
    private List<Mayfly> initializePopulation(MayflyConfig config, RandomGenerator rng) {
        List<Mayfly> pop = new ArrayList<>(config.populationSize());
        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly m = new Mayfly(config.dimensions());
            for (int d = 0; d < config.dimensions(); d++) {
                m.pos[d] = rng.nextDouble(config.lowerBound(), config.upperBound());
            }
            m.fitness = evaluate(m.pos, config);
            // Vor Iteration 1 keine Events triggern
            m.pbestFitness = m.fitness;
            System.arraycopy(m.pos, 0, m.pbestPos, 0, config.dimensions());
            pop.add(m);
        }
        return pop;
    }

    // ---------- Movement ----------
    private void moveMales(List<Mayfly> males, double w, MayflyConfig config, RandomGenerator rng, double[] gbestPosition, double[] gbestFitness) {
        Mayfly bestMale = males.stream().min(Comparator.comparingDouble(m -> m.fitness)).orElseThrow();

        double[][] newPos = new double[males.size()][config.dimensions()];
        double[][] newVel = new double[males.size()][config.dimensions()];
        boolean[] isNuptial = new boolean[males.size()];

        for (int i = 0; i < males.size(); i++) {
            Mayfly male = males.get(i);
            double dP2 = squaredDistance(male.pos, male.pbestPos);
            double dG2 = squaredDistance(male.pos, gbestPosition);

            isNuptial[i] = (male == bestMale);

            for (int d = 0; d < config.dimensions(); d++) {
                double vel;
                if (isNuptial[i]) {
                    vel = w * male.vel[d] + config.danceCoeff() * rng.nextDouble(-1.0, 1.0);
                } else {
                    vel = w * male.vel[d]
                            + config.a1() * Math.exp(-config.beta() * dP2) * (male.pbestPos[d] - male.pos[d])
                            + config.a2() * Math.exp(-config.beta() * dG2) * (gbestPosition[d] - male.pos[d]);
                }
                newVel[i][d] = vel;
                newPos[i][d] = clamp(male.pos[d] + vel, config);
            }
        }

        for (int i = 0; i < males.size(); i++) {
            Mayfly male = males.get(i);
            double prevFitness = male.fitness;

            System.arraycopy(newPos[i], 0, male.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, male.vel, 0, config.dimensions());

            male.fitness = evaluate(male.pos, config);
            fireEvent(new MayflyEvent.MaleUpdated(male, isNuptial[i], prevFitness));

            updatePersonalBest(male);
            updateGlobalBest(male.fitness, male.pos, gbestFitness, gbestPosition, UpdateSource.MALE, config);
        }
    }

    private void moveFemales(List<Mayfly> females, List<Mayfly> sortedMales, double w, MayflyConfig config, RandomGenerator rng, double[] gbestPosition, double[] gbestFitness) {
        List<Mayfly> sortedFemales = sortByFitness(females, Comparator.comparingDouble(m -> m.fitness));

        double[][] newPos = new double[config.populationSize()][config.dimensions()];
        double[][] newVel = new double[config.populationSize()][config.dimensions()];
        boolean[] isAttractedArr = new boolean[config.populationSize()];

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly female = sortedFemales.get(i);
            Mayfly male = sortedMales.get(i);
            boolean attracted = male.fitness < female.fitness;
            isAttractedArr[i] = attracted;

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
                newPos[i][d] = clamp(female.pos[d] + vel, config);
            }
        }

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly female = sortedFemales.get(i);
            double prevFitness = female.fitness;

            System.arraycopy(newPos[i], 0, female.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, female.vel, 0, config.dimensions());

            female.fitness = evaluate(female.pos, config);
            fireEvent(new MayflyEvent.FemaleUpdated(female, isAttractedArr[i], prevFitness));

            updatePersonalBest(female);
            updateGlobalBest(female.fitness, female.pos, gbestFitness, gbestPosition, UpdateSource.FEMALE, config);
        }
    }

    // ---------- Mating ----------
    private List<Mayfly> mate(List<Mayfly> sortedMales, List<Mayfly> sortedFemales, MayflyConfig config, RandomGenerator rng, double[] gbestPosition, double[] gbestFitness) {
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

            applyMutationAndEvaluate(child1, config, rng);
            fireEvent(new MayflyEvent.OffspringCreated(child1));
            updateGlobalBest(child1.fitness, child1.pos, gbestFitness, gbestPosition, UpdateSource.OFFSPRING, config);
            offspring.add(child1);

            applyMutationAndEvaluate(child2, config, rng);
            fireEvent(new MayflyEvent.OffspringCreated(child2));
            updateGlobalBest(child2.fitness, child2.pos, gbestFitness, gbestPosition, UpdateSource.OFFSPRING, config);
            offspring.add(child2);
        }
        return offspring;
    }

    private void applyMutationAndEvaluate(Mayfly m, MayflyConfig config, RandomGenerator rng) {
        for (int d = 0; d < config.dimensions(); d++) {
            m.pos[d] += rng.nextGaussian() * config.mutationStdDev();
            m.pos[d] = clamp(m.pos[d], config);
        }
        m.fitness = evaluate(m.pos, config);
        updatePersonalBest(m); // Triggert PbestUpdated beim Offspring
    }

    // ---------- Event-triggered State Updaters ----------
    private void updatePersonalBest(Mayfly m) {
        if (m.fitness < m.pbestFitness) {
            double prevPbest = m.pbestFitness;
            m.pbestFitness = m.fitness;
            System.arraycopy(m.pos, 0, m.pbestPos, 0, m.pos.length);
            fireEvent(new MayflyEvent.PbestUpdated(m, prevPbest, m.pbestFitness));
        }
    }

    private void updateGlobalBest(double fitness, double[] pos, double[] gbestFitness, double[] gbestPosition, UpdateSource source, MayflyConfig config) {
        if (fitness < gbestFitness[0]) {
            double prevGbest = gbestFitness[0];
            gbestFitness[0] = fitness;
            System.arraycopy(pos, 0, gbestPosition, 0, config.dimensions());
            fireEvent(new MayflyEvent.GbestUpdated(source, prevGbest, gbestFitness[0]));
        }
    }

    private void updateGlobalBestInitial(double fitness, double[] pos, double[] gbestFitness, double[] gbestPosition, MayflyConfig config) {
        if (fitness < gbestFitness[0]) {
            gbestFitness[0] = fitness;
            System.arraycopy(pos, 0, gbestPosition, 0, config.dimensions());
        }
    }

    // ---------- Helpers (Stateless) ----------
    private double inertiaWeight(int iter, MayflyConfig config) {
        return config.wMax() - (config.wMax() - config.wMin()) * ((double) iter / config.maxIterations());
    }

    private double clamp(double value, MayflyConfig config) {
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

    private double evaluate(double[] x, MayflyConfig config) {
        double s1 = 0, s2 = 0;
        for (double v : x) {
            s1 += v * v;
            s2 += Math.cos(2 * Math.PI * v);
        }
        return -20.0 * Math.exp(-0.2 * Math.sqrt(s1 / config.dimensions()))
                - Math.exp(s2 / config.dimensions()) + 20.0 + Math.E;
    }

    private List<Mayfly> sortByFitness(List<Mayfly> list, Comparator<Mayfly> cmp) {
        return list.stream().sorted(cmp).toList();
    }
}