package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;

public class ProcessSpecFunctionTest implements Serializable {

  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  private final TupleTag<KV<String, String>> kafkaRecordTupleTag = new TupleTag<KV<String, String>>() {

  };
  private final TupleTag<SpoutSpec> spoutSpecTupleTag = new TupleTag<SpoutSpec>() {

  };

  private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag = new TupleTag<AlertBoltSpec>() {

  };
  private final TupleTag<String> message = new TupleTag<String>() {

  };

  @Test public void testProcessSpecFunction() throws Exception {
    KafkaRecord<String, String> kafkaRecord = new KafkaRecord<>("topic", 0, 22L, 0,
        KV.of("oozie", "message"));
    // Create an input PCollection.
   // PCollection<KafkaRecord<String, String>> input = pipeline.apply(Create.of(kafkaRecord));
    PCollectionTuple output = pipeline.apply(Create.of(KV.of("oozie", "message"))).apply(ParDo
        .of(new ProcessSpecFunction(ConfigFactory.load(), 1, kafkaRecordTupleTag, spoutSpecTupleTag,
            alertBoltSpecTupleTag)).withOutputTags(kafkaRecordTupleTag,
            TupleTagList.of(spoutSpecTupleTag).and(alertBoltSpecTupleTag)));
    output.get(alertBoltSpecTupleTag).apply(ParDo.of(new PrintingAlertBoltSpecDoFn()));
    output.get(spoutSpecTupleTag).apply(ParDo.of(new PrintingSpoutSpecDoFn()));
    output.get(kafkaRecordTupleTag).apply(ParDo.of(new PrintinDoFn()));
    pipeline.run();
  }

 /* @Test public void testProcessSpecFunction1() throws Exception {
    long starttime = 1496638588877L;
    TestStream<KV<String, String>> source = TestStream
        .create(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())).addElements(KV.of("oozie",
            "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":\""
                + starttime + "\"}")).advanceWatermarkToInfinity();
    PCollectionTuple rs = pipeline.apply("get config by source", source).apply(
        ParDo.of(new GetConfigFromFileFn(spoutSpecTupleTag, message))
            .withOutputTags(message, TupleTagList.of(spoutSpecTupleTag)));
    PCollectionView<SpoutSpec> specView = rs.get(spoutSpecTupleTag).apply("SpoutSpec windows",
        Window.<SpoutSpec>into(new GlobalWindows()).triggering(
            AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(10)))
            .discardingFiredPanes().withAllowedLateness(Duration.ZERO)).apply(View.asSingleton());
    pipeline.run();
  }*/

  private static class PrintingAlertBoltSpecDoFn extends DoFn<AlertBoltSpec, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertEquals(null, c.element());
    }
  }

  private static class PrintinDoFn extends DoFn<KV<String, String>, String> {

    @ProcessElement public void processElement(ProcessContext c) {
 /*     Assert.assertEquals(new KV<>("topic", 0, 22L, 0, KV.of("oozie", "message")),
          c.element());*/
    }
  }

  private static class PrintingSpoutSpecDoFn extends DoFn<SpoutSpec, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertEquals(null, c.element());
    }
  }
}
