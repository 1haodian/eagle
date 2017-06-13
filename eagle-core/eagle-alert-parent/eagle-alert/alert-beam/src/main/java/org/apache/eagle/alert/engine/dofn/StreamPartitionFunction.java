package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.List;

public class StreamPartitionFunction extends
        PTransform<PCollection<PartitionedEvent>, PCollectionList<KV<Integer, PartitionedEvent>>> {

    private PCollectionView<List<StreamPartition>> spView;
    private int partitionNum;

    public StreamPartitionFunction(PCollectionView<List<StreamPartition>> spView, int partitionNum) {
        this.spView = spView;
        this.partitionNum = partitionNum;
    }

    @Override
    public PCollectionList<KV<Integer, PartitionedEvent>> expand(
            PCollection<PartitionedEvent> input) {
        return input.apply("add sp to key",
                ParDo.of(new DoFn<PartitionedEvent, KV<Integer, PartitionedEvent>>() {

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
                }).withSideInputs(spView)).apply(Partition.of(partitionNum, new StreamPartitionFn()));/*.apply("Extract stream partition", MapElements.into(
        TypeDescriptors.kvs(TypeDescriptors.strings(), new TypeDescriptor<PartitionedEvent>() {

        })).via((PartitionedEvent pevent) -> KV.of(pevent.getPartition().toString(), pevent)))
        .apply(GroupByKey.create()).apply(Partition.of(partitionNum, new StreamPartitionFn(
            Lists.newArrayList())));*/
    }
}
