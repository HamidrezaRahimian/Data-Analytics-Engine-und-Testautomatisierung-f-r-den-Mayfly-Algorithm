package edu.swarmintelligence.mayfly;

public final class Mayfly {
    public final double[] pos;
    public final double[] vel;
    public final double[] pbestPos;
    public double fitness = Double.POSITIVE_INFINITY;
    public double pbestFitness = Double.POSITIVE_INFINITY;

    public Mayfly(int dimensions) {
        this.pos = new double[dimensions];
        this.vel = new double[dimensions];
        this.pbestPos = new double[dimensions];
    }
}