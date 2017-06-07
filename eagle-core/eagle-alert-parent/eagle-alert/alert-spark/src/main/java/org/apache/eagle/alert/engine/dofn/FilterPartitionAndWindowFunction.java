package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Duration;

public class FilterPartitionAndWindowFunction extends
    PTransform<PCollection<KV<Integer, PartitionedEvent>>, PCollection<KV<Integer, PartitionedEvent>>> {

  private PCollectionView<StreamPartition> sssView;
  private String windowStr = "";

  public FilterPartitionAndWindowFunction(PCollectionView<StreamPartition> sssView,
      String windowStr) {
    this.sssView = sssView;
    this.windowStr = windowStr;
  }

  @Override public PCollection<KV<Integer, PartitionedEvent>> expand(
      PCollection<KV<Integer, PartitionedEvent>> input) {
    return input.apply("Filter Partition",
        ParDo.of(new DoFn<KV<Integer, PartitionedEvent>, KV<Integer, PartitionedEvent>>() {

          @ProcessElement public void processElement(ProcessContext c) {
            KV<Integer, PartitionedEvent> event = c.element();
            if (event.getValue().getPartition().equals(c.sideInput(sssView))) {
              c.output(event);
            }
          }
        }).withSideInputs(sssView));
        /*.apply("windowing", Window.into(FixedWindows.of(Duration.parse((windowStr)))))*/
  }

}
