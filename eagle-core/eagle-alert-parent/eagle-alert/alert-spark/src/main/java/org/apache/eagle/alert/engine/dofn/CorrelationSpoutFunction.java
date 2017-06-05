package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.Map;

public class CorrelationSpoutFunction extends
    PTransform<PCollection<KV<String, String>>, PCollection<KV<Integer, PartitionedEvent>>> {

  private PCollectionView<SpoutSpec> spoutSpecView;
  private PCollectionView<Map<String, StreamDefinition>> sdsView;
  private int numOfRouterBolts;

  public CorrelationSpoutFunction(PCollectionView<SpoutSpec> spoutSpecView,
      PCollectionView<Map<String, StreamDefinition>> sdsView, int numOfRouterBolts) {
    this.spoutSpecView = spoutSpecView;
    this.sdsView = sdsView;
    this.numOfRouterBolts = numOfRouterBolts;
  }

  @Override public PCollection<KV<Integer, PartitionedEvent>> expand(
      PCollection<KV<String, String>> messages) {
    return messages
        .apply(ParDo.of(new ConvertToPevent(spoutSpecView, sdsView, numOfRouterBolts)).withSideInputs(spoutSpecView,sdsView));
  }
}
