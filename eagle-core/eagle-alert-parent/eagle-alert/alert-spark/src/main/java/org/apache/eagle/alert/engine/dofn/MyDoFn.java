package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.PCollectionView;

public class MyDoFn extends DoFn<String, Integer> {

  private PCollectionView<Long> view;

  public MyDoFn() {
  }

  public MyDoFn(PCollectionView<Long> view) {
    this.view = view;
  }

  @ProcessElement public void processElement(ProcessContext c) {
    String word = c.element();
    System.out.println(c.sideInput(view));
    Integer length = word.length();
    c.output(length);
  }
}
