package com.oltpbenchmark.benchmarks.taobench.utils;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Hash {
    private static final HashFunction persistentHash = Hashing.murmur3_128();

    public static long hashPersistent(String key) {
        HashCode hc =
                persistentHash.newHasher(key.length()).putBytes(key.getBytes()).hash();
        return hc.asLong();
    }
}
