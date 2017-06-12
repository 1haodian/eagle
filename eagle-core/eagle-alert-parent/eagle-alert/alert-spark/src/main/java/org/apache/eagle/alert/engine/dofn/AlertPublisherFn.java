package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PDone;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.PublishPartition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;

public class AlertPublisherFn extends DoFn<Iterable<KV<PublishPartition, AlertStreamEvent>>, PDone> {

  private PCollectionView<PublishSpec> publishSpecView;

  public AlertPublisherFn(PCollectionView<PublishSpec> publishSpecView) {
    this.publishSpecView = publishSpecView;
  }

  @ProcessElement public void processElement(ProcessContext c) throws Exception {

  }
}
