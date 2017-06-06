package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.spark.manager.SpecManager;

public class GroupByStreamPartitionFunction extends
    PTransform<PCollection<PartitionedEvent>, PCollectionList<KV<String, PartitionedEvent>>> {

  private int partitionNum;

  public GroupByStreamPartitionFunction(int partitionNum) {
    this.partitionNum = partitionNum;
  }

  @Override public PCollectionList<KV<String, PartitionedEvent>> expand(
      PCollection<PartitionedEvent> input) {
    return input.apply("Extract stream partition", MapElements.into(
        TypeDescriptors.kvs(TypeDescriptors.strings(), new TypeDescriptor<PartitionedEvent>() {

        })).via((PartitionedEvent pevent) -> KV.of(pevent.getPartition().toString(), pevent)))
       /* .apply(GroupByKey.create())*/.apply(Partition.of(partitionNum, new StreamPartitionFn(
            Lists.newArrayList())));
  }
}
