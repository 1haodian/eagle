package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.KV;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.Collections;
import java.util.List;

public class StreamPartitionFn implements Partition.PartitionFn<KV<String, PartitionedEvent>> {

  private List<String> keys;

  public StreamPartitionFn(List<String> keys) {
    keys = Collections.unmodifiableList(keys);
  }

  @Override public int partitionFor(KV<String, PartitionedEvent> elem, int numPartitions) {
    if ("StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT5S,windowMargin=2000]]]"
        .equals(elem.getKey())) {
      return 1;
    }

    return 0;
  }
}
