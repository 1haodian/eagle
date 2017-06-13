package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.KV;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Map;

public class KVToMapFn extends DoFn<KV<String, StreamDefinition>, Map<String, StreamDefinition>> {

    private Map<String, StreamDefinition> result;

    @Setup
    public void prepare() {
        result = new HashMap<>();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        result.put(c.element().getKey(), c.element().getValue());
    }

    @FinishBundle
    public void finish(FinishBundleContext c) {
        c.output(result, new Instant(0), GlobalWindow.INSTANCE);
    }
}
