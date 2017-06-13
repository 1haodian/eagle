package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class CorrelationSpoutFunction1Test implements Serializable {

  @Rule public final transient TestPipeline p = TestPipeline.create();
  private final TupleTag<SpoutSpec> spoutSpecTupleTag = new TupleTag<SpoutSpec>() {

  };
  private final TupleTag<String> message = new TupleTag<String>() {

  };

  @Test public void testCorrelationSpoutFunction() {

    long starttime = 1496638588877L;
    TestStream<KV<String, String>> source = TestStream
        .create(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())).addElements(KV.of("oozie",
            "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":\""
                + starttime + "\"}")).advanceWatermarkToInfinity();
    PCollection<KV<String, String>> rawMessage = p.apply("get config by source", source);

    PCollectionTuple rs = rawMessage.apply(
        ParDo.of(new GetConfigFromFileFn(spoutSpecTupleTag, message, SpecFactory.createSpoutSpec()))
            .withOutputTags(message, TupleTagList.of(spoutSpecTupleTag)));
    PCollectionView<SpoutSpec> specView = rs.get(spoutSpecTupleTag).apply("SpoutSpec windows",
        Window.<SpoutSpec>into(new GlobalWindows()).triggering(
            AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(10)))
            .discardingFiredPanes().withAllowedLateness(Duration.ZERO)).apply(View.asSingleton());

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", SpecFactory.createSds().get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("sds windows", Window.<KV<String, StreamDefinition>>into(new GlobalWindows())
            .triggering(AfterProcessingTime.pastFirstElementInPane()
                .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
            .withAllowedLateness(Duration.ZERO)).apply(View.asMap());

    PCollection<KV<Integer, PartitionedEvent>> input = rawMessage
        .apply(new CorrelationSpoutFunction1(specView, sdsView, 10));


    Map<StreamPartition, StreamSortSpec> sss = SpecFactory.createRouterSpec().makeSSS();

    Set<StreamPartition> sps = sss.keySet();
    for (StreamPartition sp : sps) {
      PCollectionView<StreamPartition> sssView = p
          .apply("get current sp" + sp.toString(), Create.of(sp))
          .apply("sp windows" + sp.toString(), Window.<StreamPartition>into(new GlobalWindows())
              .triggering(AfterProcessingTime.pastFirstElementInPane()
                  .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
              .withAllowedLateness(Duration.ZERO))
          .apply("current sp" + sp.toString(), View.asSingleton());

      input.apply("FilterPartitionAndWindowFunction" + sp.toString(),
          new FilterPartitionAndWindowFunction(sssView, sp.getSortSpec().getWindowPeriod()))
          .apply("print" + sp.toString(), ParDo.of(new PrintinDoFn1()));
    }
    p.run();
  }

  private static class PrintinDoFn1
      extends DoFn<KV<Integer, PartitionedEvent>, KV<Integer, PartitionedEvent>> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertTrue(c.element().toString().startsWith(
          "KV{3, PartitionedEvent[partition=StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]],event=StreamEvent[stream=OOZIESTREAM"));
      System.out.println("PrintinDoFn1" + c.element());
    }
  }
}