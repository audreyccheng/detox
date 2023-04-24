package com.oltpbenchmark.benchmarks.taobench;

import java.util.Arrays;
import java.util.List;

public class TaobenchConstants {
    public static final double a_1 = 2.0;
    public static final double a_2 = 1.3;
    public static final double PROB_TRX_READ= 59.0; // 58.0; //  49.0;
    public static final double PROB_TRX_READ_TXN = 84.0; // 83.0; // 89.0
    public static final double PROB_TRX_READ_SCAN = 97.0; // 100.0; //
    public static final double PROB_TRX_READ_1= 60.0; // 100.0 // 60.0
    public static final double PROB_TRX_WRITE = 100.0;
    public static final int NB_OBJECTS = 100000;
    private static final int BASE_SIZE = 10000;
    public static final int GROUP_1 = BASE_SIZE;

    // TODO(jchan): What is group 2?
    public static final int GROUP_2 = BASE_SIZE * 2;
    public static final int GROUP_3 = BASE_SIZE * 3;
    public static final String OBJECTS_TABLE = "objects";

    public static final List<Integer> TXN_SIZES_5 = Arrays.asList(2,3,4); // 10,15,20,25,30); // 5,6,7,8,10,20); //
    public static final List<Double> TXN_WEIGHTS_5 = Arrays.asList(0.4,0.35,0.25); // 0.20,0.20,0.20,0.20,0.20); // 0.2,0.2,0.2,0.2,0.19,0.01); //
    public static final List<Integer> TXN_SIZES_6 = Arrays.asList(2,3,4); // 50,51,52,53,54,55,56,57,58,59,60); // 10,20,30,40,50,60); //
    public static final  List<Double> TXN_WEIGHTS_6 = Arrays.asList(0.4,0.35,0.25); //0.05,0.05,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1); // 0.2,0.2,0.1,0.1,0.2,0.2); //
    public static final List<Integer> TXN_SIZES_7 = Arrays.asList(2,3,4,5,6,7,8,9,10,20);

    // TODO(jchan): These weights didn't add up to 1 in detox, so I changed them.
    public static final List<Double> TXN_WEIGHTS_7 = Arrays.asList(0.15,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.05);
    public static final List<Integer> DATA_SIZES = Arrays.asList(75);
    public static final List<Double> DATA_WEIGHTS = Arrays.asList(1.0);
    public static final int NB_LOADER_THREADS = 16;

    public static final double PROB_TRX_GROUP = 100.0;
    public static final int TXN_SIZE_GROUP = 10;
}
