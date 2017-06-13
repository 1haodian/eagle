package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.factory.PeventFactory;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class StreamPartitionFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testGroupByStreamPartitionFunction() {
    List<StreamPartition> sps = Lists.newArrayList(SpecFactory.createRouterSpec().makeSSS().keySet());
    PCollectionView<List<StreamPartition>> spView = p.apply("getSss", Create.of(sps))
        .apply(View.asList());


    List<String> keys = Lists.newArrayList(
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]",
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT5S,windowMargin=2000]]]");
    PCollection<PartitionedEvent> input = p.apply("create pevent", Create.of(PeventFactory.createPevents()));
    int partNum = SpecFactory.createRouterSpec().makeSSS().keySet().size();
    PCollectionList<KV<Integer, PartitionedEvent>> rs = input
        .apply("covert to (streampartition->pevent)", new StreamPartitionFunction(spView, partNum));

    for (int i = 0; i < partNum; i++) {
      PCollection<KV<Integer, PartitionedEvent>> partition = rs.get(i);
      partition
          .apply("PrintinDoFn" + i, ParDo.of(new PrintinDoFn(i, spView)).withSideInputs(spView));
    }
    p.run();
  }

  private static class PrintinDoFn extends DoFn<KV<Integer, PartitionedEvent>, String> {

    private int partNum;
    private PCollectionView<List<StreamPartition>> spView;

    public PrintinDoFn(int partNum, PCollectionView<List<StreamPartition>> spView) {
      this.partNum = partNum;
      this.spView = spView;
    }

    @ProcessElement public void processElement(ProcessContext c) {
      KV<Integer, PartitionedEvent> kv = c.element();
      List<StreamPartition> sps = c.sideInput(spView);
      System.out.println("PrintinDoFn" + partNum + "   " + kv);
      Assert.assertEquals(new Integer(partNum), kv.getKey());
      Assert.assertEquals(sps.get(partNum), kv.getValue().getPartition());
    }
  }

}
