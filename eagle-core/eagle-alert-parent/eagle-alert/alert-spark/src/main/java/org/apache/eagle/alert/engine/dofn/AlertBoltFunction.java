package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

public class AlertBoltFunction
    extends PTransform<PCollection<PartitionedEvent>, PCollection<AlertStreamEvent>> {

  @Override public PCollection<AlertStreamEvent> expand(
      PCollection<PartitionedEvent> input) {
    return null;//ParDoLifecycleTest
  }
}
