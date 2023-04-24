package com.oltpbenchmark.benchmarks.taobench.utils;

import java.util.List;
import java.util.Random;

/**
 * Utility class for generating uniformly random integers
 *
 * @author ncrooks
 */
public class Generator {

    private static Random random = new Random(Utils.hash(System.currentTimeMillis()));

    /**
     * Generates random integer [i, j) with uniform probability
     */
    public static int generateInt(int i, int j) {
        if (i == j) {
            return i; // 1; // random.nextInt(1);
        }
        return random.nextInt(j - i) + i;
    }

    public static int getDiscrete(List<Integer> values, List<Double> weights) {
        double prob = 0.0;
        double randDouble = random.nextDouble();
        int val = 0;
        int index = 0;
        while (prob < randDouble) {
            val = values.get(index);
            prob += weights.get(index);
            index += 1;
        }
        return val;
    }
}
