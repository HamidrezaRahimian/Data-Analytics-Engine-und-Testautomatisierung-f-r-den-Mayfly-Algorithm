package edu.swarmintelligence.mayfly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class MayflyAlgorithm {
    private final List<MayflyEventListener> listeners = new ArrayList<>();

    public void addListener(MayflyEventListener listener) {
        listeners.add(listener);
    }

    private void fireEvent(MayflyEvent event) {
        for (MayflyEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    public MayflyResult run(MayflyConfig config, long seed) {
        RandomGenerator rng = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create(seed);
        double[] gbestPosition = new double[config.dimensions()];
        double[] gbestFitness = { Double.POSITIVE_INFINITY };

        List<Mayfly> males = initializePopulation(config, rng);
        List<Mayfly> females = initializePopulation(config, rng);

        for (Mayfly male : males) {
            updateGlobalBestInitial(male.fitness, male.pos, gbestFitness, gbestPosition, config);
        }
        for (Mayfly female : females) {
            updateGlobalBestInitial(female.fitness, female.pos, gbestFitness, gbestPosition, config);
        }

        Comparator<Mayfly> byFitness = Comparator.comparingDouble(m -> m.fitness);

        for (int iter = 1; iter <= config.maxIterations(); iter++) {
            double w = inertiaWeight(iter, config);
            fireEvent(new MayflyEvent.IterationStarted(iter, w));

            moveMales(males, w, config, rng, gbestPosition, gbestFitness);
            List<Mayfly> sortedMales = sortByFitness(males, byFitness);

            moveFemales(females, sortedMales, w, config, rng, gbestPosition, gbestFitness);
            sortedMales = sortByFitness(males, byFitness);
            List<Mayfly> sortedFemales = sortByFitness(females, byFitness);

            List<Mayfly> offspring = mate(sortedMales, sortedFemales, config, rng, gbestPosition, gbestFitness);

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

    private List<Mayfly> initializePopulation(MayflyConfig config, RandomGenerator rng) {
        List<Mayfly> pop = new ArrayList<>(config.populationSize());
        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly mayfly = new Mayfly(config.dimensions());
            for (int d = 0; d < config.dimensions(); d++) {
                mayfly.pos[d] = rng.nextDouble(config.lowerBound(), config.upperBound());
            }
            mayfly.fitness = evaluate(mayfly.pos, config);
            mayfly.pbestFitness = mayfly.fitness;
            System.arraycopy(mayfly.pos, 0, mayfly.pbestPos, 0, config.dimensions());
            pop.add(mayfly);
        }
        return pop;
    }

    private void moveMales(List<Mayfly> males, double w, MayflyConfig config, RandomGenerator rng,
                           double[] gbestPosition, double[] gbestFitness) {
        Mayfly bestMale = males.stream().min(Comparator.comparingDouble(m -> m.fitness)).orElseThrow();
        double[][] newPos = new double[males.size()][config.dimensions()];
        double[][] newVel = new double[males.size()][config.dimensions()];
        boolean[] isNuptial = new boolean[males.size()];

        for (int i = 0; i < males.size(); i++) {
            Mayfly male = males.get(i);
            double dP2 = squaredDistance(male.pos, male.pbestPos);
            double dG2 = squaredDistance(male.pos, gbestPosition);
            isNuptial[i] = male == bestMale;

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
            double previousFitness = male.fitness;
            System.arraycopy(newPos[i], 0, male.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, male.vel, 0, config.dimensions());
            male.fitness = evaluate(male.pos, config);
            fireEvent(new MayflyEvent.MaleUpdated(male, isNuptial[i], previousFitness));
            updatePersonalBest(male);
            updateGlobalBest(male.fitness, male.pos, gbestFitness, gbestPosition, UpdateSource.MALE, config);
        }
    }

    private void moveFemales(List<Mayfly> females, List<Mayfly> sortedMales, double w, MayflyConfig config,
                             RandomGenerator rng, double[] gbestPosition, double[] gbestFitness) {
        List<Mayfly> sortedFemales = sortByFitness(females, Comparator.comparingDouble(m -> m.fitness));
        double[][] newPos = new double[config.populationSize()][config.dimensions()];
        double[][] newVel = new double[config.populationSize()][config.dimensions()];
        boolean[] isAttractedArr = new boolean[config.populationSize()];

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly female = sortedFemales.get(i);
            Mayfly male = sortedMales.get(i);
            boolean attracted = male.fitness < female.fitness;
            isAttractedArr[i] = attracted;
            double dMF2 = attracted ? squaredDistance(female.pos, male.pos) : 0.0;

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
            double previousFitness = female.fitness;
            System.arraycopy(newPos[i], 0, female.pos, 0, config.dimensions());
            System.arraycopy(newVel[i], 0, female.vel, 0, config.dimensions());
            female.fitness = evaluate(female.pos, config);
            fireEvent(new MayflyEvent.FemaleUpdated(female, isAttractedArr[i], previousFitness));
            updatePersonalBest(female);
            updateGlobalBest(female.fitness, female.pos, gbestFitness, gbestPosition, UpdateSource.FEMALE, config);
        }
    }

    private List<Mayfly> mate(List<Mayfly> sortedMales, List<Mayfly> sortedFemales, MayflyConfig config,
                              RandomGenerator rng, double[] gbestPosition, double[] gbestFitness) {
        List<Mayfly> offspring = new ArrayList<>(config.populationSize() * 2);

        for (int i = 0; i < config.populationSize(); i++) {
            Mayfly p1 = sortedMales.get(i);
            Mayfly p2 = sortedFemales.get(i);
            double l = rng.nextDouble();
            double parentDistance = Math.sqrt(squaredDistance(p1.pos, p2.pos));
            double parentFitnessGap = Math.abs(p1.fitness - p2.fitness);

            Mayfly child1 = new Mayfly(config.dimensions());
            Mayfly child2 = new Mayfly(config.dimensions());

            for (int d = 0; d < config.dimensions(); d++) {
                child1.pos[d] = l * p1.pos[d] + (1.0 - l) * p2.pos[d];
                child2.pos[d] = l * p2.pos[d] + (1.0 - l) * p1.pos[d];
            }

            applyMutationAndEvaluate(child1, config, rng);
            fireEvent(new MayflyEvent.OffspringCreated(child1, parentDistance, parentFitnessGap, true));
            updateGlobalBest(child1.fitness, child1.pos, gbestFitness, gbestPosition, UpdateSource.OFFSPRING, config);
            offspring.add(child1);

            applyMutationAndEvaluate(child2, config, rng);
            fireEvent(new MayflyEvent.OffspringCreated(child2, parentDistance, parentFitnessGap, false));
            updateGlobalBest(child2.fitness, child2.pos, gbestFitness, gbestPosition, UpdateSource.OFFSPRING, config);
            offspring.add(child2);
        }
        return offspring;
    }

    private void applyMutationAndEvaluate(Mayfly mayfly, MayflyConfig config, RandomGenerator rng) {
        for (int d = 0; d < config.dimensions(); d++) {
            mayfly.pos[d] += rng.nextGaussian() * config.mutationStdDev();
            mayfly.pos[d] = clamp(mayfly.pos[d], config);
        }
        mayfly.fitness = evaluate(mayfly.pos, config);
        updatePersonalBest(mayfly);
    }

    private void updatePersonalBest(Mayfly mayfly) {
        if (mayfly.fitness < mayfly.pbestFitness) {
            double previousPbest = mayfly.pbestFitness;
            mayfly.pbestFitness = mayfly.fitness;
            System.arraycopy(mayfly.pos, 0, mayfly.pbestPos, 0, mayfly.pos.length);
            fireEvent(new MayflyEvent.PbestUpdated(mayfly, previousPbest, mayfly.pbestFitness));
        }
    }

    private void updateGlobalBest(double fitness, double[] pos, double[] gbestFitness,
                                  double[] gbestPosition, UpdateSource source, MayflyConfig config) {
        if (fitness < gbestFitness[0]) {
            double previousGbest = gbestFitness[0];
            gbestFitness[0] = fitness;
            System.arraycopy(pos, 0, gbestPosition, 0, config.dimensions());
            fireEvent(new MayflyEvent.GbestUpdated(source, previousGbest, gbestFitness[0]));
        }
    }

    private void updateGlobalBestInitial(double fitness, double[] pos, double[] gbestFitness,
                                         double[] gbestPosition, MayflyConfig config) {
        if (fitness < gbestFitness[0]) {
            gbestFitness[0] = fitness;
            System.arraycopy(pos, 0, gbestPosition, 0, config.dimensions());
        }
    }

    private double inertiaWeight(int iter, MayflyConfig config) {
        return config.wMax() - (config.wMax() - config.wMin()) * ((double) iter / config.maxIterations());
    }

    private double clamp(double value, MayflyConfig config) {
        return Math.clamp(value, config.lowerBound(), config.upperBound());
    }

    private double squaredDistance(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return sum;
    }

    private double evaluate(double[] x, MayflyConfig config) {
        double s1 = 0.0;
        double s2 = 0.0;
        for (double v : x) {
            s1 += v * v;
            s2 += Math.cos(2.0 * Math.PI * v);
        }
        return -20.0 * Math.exp(-0.2 * Math.sqrt(s1 / config.dimensions()))
                - Math.exp(s2 / config.dimensions()) + 20.0 + Math.E;
    }

    private List<Mayfly> sortByFitness(List<Mayfly> list, Comparator<Mayfly> comparator) {
        List<Mayfly> sorted = new ArrayList<>(list);
        sorted.sort(comparator);
        return sorted;
    }
}
