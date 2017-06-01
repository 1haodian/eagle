package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;

public class ProcessSpecFunctionTest implements Serializable {

  @Rule public final transient TestPipeline pipeline = TestPipeline.create();

  private final TupleTag<KafkaRecord<String, String>> kafkaRecordTupleTag = new TupleTag<KafkaRecord<String, String>>() {

  };
  private final TupleTag<SpoutSpec> spoutSpecTupleTag = new TupleTag<SpoutSpec>() {

  };

  private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag = new TupleTag<AlertBoltSpec>() {

  };

  @Test public void testProcessSpecFunction() throws Exception {
    KafkaRecord<String, String> kafkaRecord = new KafkaRecord<>("topic", 0, 22L, 0,
        KV.of("oozie", "message"));
    // Create an input PCollection.
    PCollection<KafkaRecord<String, String>> input = pipeline.apply(Create.of(kafkaRecord));
    PCollectionTuple output = input.apply(ParDo
        .of(new ProcessSpecFunction(ConfigFactory.load(), 1, kafkaRecordTupleTag, spoutSpecTupleTag,
            alertBoltSpecTupleTag)).withOutputTags(kafkaRecordTupleTag,
            TupleTagList.of(spoutSpecTupleTag).and(alertBoltSpecTupleTag)));
    output.get(alertBoltSpecTupleTag).apply(ParDo.of(new PrintingAlertBoltSpecDoFn()));
    output.get(spoutSpecTupleTag).apply(ParDo.of(new PrintingSpoutSpecDoFn()));
    output.get(kafkaRecordTupleTag).apply(ParDo.of(new PrintinDoFn()));
    pipeline.run();
  }

  private static class PrintingAlertBoltSpecDoFn extends DoFn<AlertBoltSpec, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertEquals(null, c.element());
    }
  }

  private static class PrintinDoFn extends DoFn<KafkaRecord<String, String>, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertEquals(new KafkaRecord<>("topic", 0, 22L, 0, KV.of("oozie", "message")),
          c.element());
    }
  }

  private static class PrintingSpoutSpecDoFn extends DoFn<SpoutSpec, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      Assert.assertEquals(null, c.element());
    }
  }
}
