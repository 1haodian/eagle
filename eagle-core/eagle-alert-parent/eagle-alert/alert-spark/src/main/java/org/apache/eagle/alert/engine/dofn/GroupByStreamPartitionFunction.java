package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

public class GroupByStreamPartitionFunction extends
    PTransform<PCollection<PartitionedEvent>, PCollection<KV<String, Iterable<PartitionedEvent>>>> {

  private PCollectionView<RouterSpec> routerSpecView;

  public GroupByStreamPartitionFunction(PCollectionView<RouterSpec> routerSpecView) {
    this.routerSpecView = routerSpecView;
  }

  @Override public PCollection<KV<String, Iterable<PartitionedEvent>>> expand(
      PCollection<PartitionedEvent> input) {
    return input.apply("Extract stream partition",
        MapElements.into(TypeDescriptors.kvs(TypeDescriptors.strings(), new TypeDescriptor<PartitionedEvent>() {

        })).via((PartitionedEvent pevent) -> KV.of(pevent.getPartition().toString(), pevent))).apply(GroupByKey.create());
  }
}
