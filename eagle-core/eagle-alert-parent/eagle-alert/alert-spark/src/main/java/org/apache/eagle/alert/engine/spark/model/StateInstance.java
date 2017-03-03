package org.apache.eagle.alert.engine.spark.model;

import org.apache.spark.Accumulator;
import org.apache.spark.AccumulatorParam;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import java.util.HashMap;

/**
 * Created by root on 2/9/17.
 */
public class StateInstance {
    private static volatile Accumulator instance = null;

    public static Accumulator getInstance(JavaStreamingContext jssc, String accumName, AccumulatorParam clazz) {
        if (instance == null) {
            synchronized (StateInstance.class) {
                if (instance == null) {
                    instance = jssc.sparkContext().accumulator(new HashMap<>(), accumName, clazz);
                }
            }
        }
        return instance;
    }
}
