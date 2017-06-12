package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.Map;

public class AlertBoltFunction
    extends PTransform<PCollection<Iterable<PartitionedEvent>>, PCollection<KV<String, AlertStreamEvent>>> {

  private PCollectionView<AlertBoltSpec> alertBoltSpecView;
  private PCollectionView<Map<String, StreamDefinition>> sdsView;

  public AlertBoltFunction(PCollectionView<AlertBoltSpec> alertBoltSpecView,
      PCollectionView<Map<String, StreamDefinition>> sdsView) {
    this.alertBoltSpecView = alertBoltSpecView;
    this.sdsView = sdsView;
  }

  @Override public PCollection<KV<String, AlertStreamEvent>> expand(
      PCollection<Iterable<PartitionedEvent>> input) {
    return input.apply(ParDo.of(new SiddhiFn(alertBoltSpecView, sdsView))
        .withSideInputs(alertBoltSpecView, sdsView));
  }
}
