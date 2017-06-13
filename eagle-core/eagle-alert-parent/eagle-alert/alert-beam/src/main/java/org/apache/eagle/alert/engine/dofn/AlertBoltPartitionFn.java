package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.KV;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

public class AlertBoltPartitionFn implements Partition.PartitionFn<KV<Integer, PartitionedEvent>> {

    @Override
    public int partitionFor(KV<Integer, PartitionedEvent> elem, int numPartitions) {
        return elem.getKey();
    }
}
