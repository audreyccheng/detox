package shield.benchmarks.freehealth.utils;

import shield.benchmarks.freehealth.FreeHealthExperimentConfiguration;

public class SerializeTester {

    public static void main(String[] args) {
        FreeHealthExperimentConfiguration conf = new FreeHealthExperimentConfiguration();
        SerializableIDSet s1 = new SerializableIDSet(conf);

        s1.add(15);
        s1.add(42);
        s1.add(1125);
        System.out.println(s1.serialize());
    }

}
