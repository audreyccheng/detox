package com.oltpbenchmark.benchmarks.taobench.utils;

import java.util.List;

public class TaobenchGenerator {
    private static final char[] ALPHANUM =  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    public static String RandDiscreteString(List<Integer> values, List<Double> weights, boolean num_only) {
        int size = Generator.getDiscrete(values, weights);
        return RandString(size, size+1, num_only);
    }

    /**
     * Generates a random string of size between min and max, and optionally consisting
     * of numbers only
     *
     * @param num_only
     * @return
     */
    public static String RandString(int min, int max, boolean num_only) {
        StringBuffer bf = new StringBuffer();
        int len = Generator.generateInt(min, max);
        for (int i = 0; i < len; ++i) {
            bf.append(RandCharNum(num_only));
        }
        return bf.toString();
    }

    public static char RandCharNum(boolean numOnly) {
        int x = Generator.generateInt(0,numOnly?10:26);
        return ALPHANUM[x];
    }
}
