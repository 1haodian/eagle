package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.eagle.alert.coordination.model.SpoutSpec;

public class GetConfigFromFileFn extends DoFn<KV<String, String>, String> {

  private final TupleTag<SpoutSpec> spoutSpecTupleTag;
  private final TupleTag<String> message;
  private final SpoutSpec newSpec;

  public GetConfigFromFileFn(TupleTag<SpoutSpec> spoutSpecTupleTag,
      TupleTag<String> message,SpoutSpec newSpec) {
    this.spoutSpecTupleTag = spoutSpecTupleTag;
    this.message = message;
    this.newSpec = newSpec;
  }

  @ProcessElement public void processElement(ProcessContext c) {
    c.output(spoutSpecTupleTag, newSpec);
    c.output(message, "");
  }
 /* @FinishBundle public void finish(FinishBundleContext c) {
    c.output(newSpec, new Instant(0), GlobalWindow.INSTANCE);
  }*/
}
