package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class StreamPartitionFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testGroupByStreamPartitionFunction() {
    RouterSpec routerSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamRouterBoltSpec.json"),
            RouterSpec.class);
    List<StreamPartition> sps = Lists.newArrayList(routerSpec.makeSSS().keySet());
    PCollectionView<List<StreamPartition>> spView = p.apply("getSss", Create.of(sps))
        .apply(View.asList());

    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();//"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]"
    StreamSortSpec streamSortSpec = new StreamSortSpec();
    streamSortSpec.setWindowMargin(1000);
    streamSortSpec.setWindowPeriod("PT4S");
    streamPartition.setStreamId("oozieStream");
    streamPartition.setType(StreamPartition.Type.GROUPBY);
    streamPartition.setColumns(Lists.newArrayList("operation"));
    streamPartition.setSortSpec(streamSortSpec);
    pevent1.setEvent(new StreamEvent());
    pevent1.getEvent().setTimestamp(1);
    pevent1.setPartition(streamPartition);

    PartitionedEvent pevent2 = new PartitionedEvent();
    StreamPartition streamPartition2 = new StreamPartition();//"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]"
    StreamSortSpec streamSortSpec2 = new StreamSortSpec();
    streamSortSpec2.setWindowMargin(2000);
    streamSortSpec2.setWindowPeriod("PT5S");
    streamPartition2.setStreamId("oozieStream");
    streamPartition2.setType(StreamPartition.Type.GROUPBY);
    streamPartition2.setColumns(Lists.newArrayList("operation"));
    streamPartition2.setSortSpec(streamSortSpec2);
    pevent2.setEvent(new StreamEvent());
    pevent2.getEvent().setTimestamp(13);
    pevent2.setPartition(streamPartition2);

    List<PartitionedEvent> pevents = Arrays.asList(pevent1, pevent2);
    List<String> keys = Lists.newArrayList(
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]",
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT5S,windowMargin=2000]]]");
    PCollection<PartitionedEvent> input = p.apply("create pevent", Create.of(pevents));
    int partNum = routerSpec.makeSSS().keySet().size();
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
