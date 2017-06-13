package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.List;

public class AssignPartitionFn extends DoFn<PartitionedEvent, KV<Integer, PartitionedEvent>> {
    private PCollectionView<List<StreamPartition>> spView;

    public AssignPartitionFn(PCollectionView<List<StreamPartition>> spView) {
        this.spView = spView;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        PartitionedEvent pevent = c.element();
        List<StreamPartition> sps = c.sideInput(spView);
        for (int i = 0; i < sps.size(); i++) {
            if (sps.get(i).equals(pevent.getPartition())) {
                c.output(KV.of(i, pevent));
                break;
            }
        }
    }
}
