package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coder.MyType;

public class MyDoFn extends DoFn<String, Integer> {

  private PCollectionView<SpoutSpec> view;
  private SpoutSpec spoutSpec;

  public MyDoFn() {
  }

  public MyDoFn(SpoutSpec spoutSpec) {
    this.spoutSpec = spoutSpec;
  }

  public MyDoFn(PCollectionView<SpoutSpec> view) {
    this.view = view;
  }

  @ProcessElement public void processElement(ProcessContext c) {
    String word = c.element();
    //System.out.println(spoutSpec.getKafka2TupleMetadataMap());

    System.out.println(c.sideInput(view).getStreamRepartitionMetadataMap());
    System.out.println(c.sideInput(view).getKafka2TupleMetadataMap());
    System.out.println(c.sideInput(view).getTuple2StreamMetadataMap());
    Integer length = word.length();
    c.output(length);
  }
}
