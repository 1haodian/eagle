package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PDone;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.PublishPartition;
import org.apache.eagle.alert.engine.io.ConsoleIO;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;

public class AlertPublisherFunction
    extends PTransform<PCollection<Iterable<KV<PublishPartition, AlertStreamEvent>>>, PDone> {

  private PCollectionView<PublishSpec> publishSpecView;

  public AlertPublisherFunction(PCollectionView<PublishSpec> publishSpecView) {
    this.publishSpecView = publishSpecView;
  }

  @Override public PDone expand(PCollection<Iterable<KV<PublishPartition, AlertStreamEvent>>> input) {
    return input
        .apply(ParDo.of(new AlertPublisherFn(publishSpecView)).withSideInputs(publishSpecView))
        .apply(ConsoleIO.Write.out());
  }
}