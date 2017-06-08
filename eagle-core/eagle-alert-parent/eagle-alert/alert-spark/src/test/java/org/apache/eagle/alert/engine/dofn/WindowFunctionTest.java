package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.TimestampedValue;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class WindowFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testaccumulateModeEventTimeTrigger() {
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();
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
    StreamPartition streamPartition2 = new StreamPartition();
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
    Instant instant = new Instant(0);
    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(pevent2, new Instant(pevent2.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(1))
        .advanceWatermarkTo(instant.plus(Duration.standardMinutes(6)))
        .addElements(TimestampedValue.of(pevent1, new Instant(pevent1.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(2)).advanceWatermarkToInfinity();

    PCollection<PartitionedEvent> windowed = p.apply(source).apply(
        Window.<PartitionedEvent>into(FixedWindows.of(Duration.standardMinutes(5))).triggering(
            AfterWatermark.pastEndOfWindow().withEarlyFirings(
                AfterProcessingTime.pastFirstElementInPane()
                    .plusDelayOf(Duration.standardMinutes(5)))).accumulatingFiredPanes()
            .withAllowedLateness(Duration.ZERO));

    PCollection<Long> count = windowed
        .apply(Combine.globally(Count.<PartitionedEvent>combineFn()).withoutDefaults());
    count.apply(ParDo.of(new PrintingDoFn1()));

    PCollection<PartitionedEvent> triggered = windowed.apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create()).apply(Flatten.iterables());

    IntervalWindow window = new IntervalWindow(instant, instant.plus(Duration.standardMinutes(5L)));
    PAssert.that(triggered).inOnTimePane(window).containsInAnyOrder(pevent2);
    PAssert.that(triggered).inFinalPane(window).containsInAnyOrder(pevent2);
    p.run();
  }

  @Test public void testaccumulateModeProcessingTimeTrigger() {
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();
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
    StreamPartition streamPartition2 = new StreamPartition();
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

    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(pevent2, new Instant(pevent2.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(12))
        .addElements(TimestampedValue.of(pevent1, new Instant(pevent1.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(6)).advanceWatermarkToInfinity();

    PCollection<PartitionedEvent> windowed = p.apply(source).apply(
        Window.<PartitionedEvent>into(FixedWindows.of(Duration.standardMinutes(5))).triggering(
            AfterWatermark.pastEndOfWindow().withEarlyFirings(
                AfterProcessingTime.pastFirstElementInPane()
                    .plusDelayOf(Duration.standardMinutes(5)))).accumulatingFiredPanes()
            .withAllowedLateness(Duration.ZERO));

    PCollection<Long> count = windowed
        .apply(Combine.globally(Count.<PartitionedEvent>combineFn()).withoutDefaults());
    count.apply(ParDo.of(new PrintingDoFn1()));

    PCollection<PartitionedEvent> triggered = windowed.apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create()).apply(Flatten.iterables());
    Instant instant = new Instant(0);
    IntervalWindow window = new IntervalWindow(instant, instant.plus(Duration.standardMinutes(5L)));
    PAssert.that(triggered).inFinalPane(window).containsInAnyOrder(pevent1, pevent2);
    PAssert.that(triggered).inOnTimePane(window).containsInAnyOrder(pevent1, pevent2);
    p.run();
  }


  @Test public void testdiscardModeProcessingTimeTrigger() {
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();
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
    StreamPartition streamPartition2 = new StreamPartition();
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

    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(pevent2, new Instant(pevent2.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(12))
        .addElements(TimestampedValue.of(pevent1, new Instant(pevent1.getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(6)).advanceWatermarkToInfinity();

    PCollection<PartitionedEvent> windowed = p.apply(source).apply(
        Window.<PartitionedEvent>into(FixedWindows.of(Duration.standardMinutes(5))).triggering(
            AfterWatermark.pastEndOfWindow().withEarlyFirings(
                AfterProcessingTime.pastFirstElementInPane()
                    .plusDelayOf(Duration.standardMinutes(5)))).discardingFiredPanes()
            .withAllowedLateness(Duration.ZERO));

    PCollection<Long> count = windowed
        .apply(Combine.globally(Count.<PartitionedEvent>combineFn()).withoutDefaults());
    count.apply(ParDo.of(new PrintingDoFn1()));

    PCollection<PartitionedEvent> triggered = windowed.apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create()).apply(Flatten.iterables());
    Instant instant = new Instant(0);
    IntervalWindow window = new IntervalWindow(instant, instant.plus(Duration.standardMinutes(5L)));
    PAssert.that(triggered).inFinalPane(window).containsInAnyOrder(Collections.emptyList());
    PAssert.that(triggered).inOnTimePane(window).containsInAnyOrder(Collections.emptyList());
    p.run();
  }
  @Test public void testPCViewTrigger() {
    SpoutSpec newSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testSpoutSpec.json"), SpoutSpec.class);
    PCollectionView<SpoutSpec> specView = p
       // .apply(TextIO.read().from("/local/path/to/file.txt"))
        .apply("getSpec", Create.of(newSpec)).apply("SpoutSpec windows",
        Window.<SpoutSpec>into(new GlobalWindows()).triggering(
            AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(10)))
            .discardingFiredPanes().withAllowedLateness(Duration.ZERO))
        .apply(View.asSingleton());

    PCollection<SpoutSpec> triggered = (PCollection<SpoutSpec>) specView.getPCollection();
    triggered.apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create()).apply(Flatten.iterables())
        .apply(ParDo.of(new PrintingDoFn2()));
    p.run();
  }
  private static class PrintingDoFn extends DoFn<PartitionedEvent, String> {

    @ProcessElement public void processElement(ProcessContext c, BoundedWindow window) {
      c.output(
          c.element() + ":" + c.timestamp().getMillis() + ":" + window.maxTimestamp().getMillis());
      System.out.println(
          c.element() + ":--" + c.timestamp().toDateTime() + "--:" + window.toString() + ":"
              + window.maxTimestamp().toDateTime() + ":" + c.pane());
    }
  }
  private static class PrintingDoFn2 extends DoFn<SpoutSpec, String> {

    @ProcessElement public void processElement(ProcessContext c, BoundedWindow window) {
      c.output(
          c.element() + ":" + c.timestamp().getMillis() + ":" + window.maxTimestamp().getMillis());
      System.out.println(
          c.element() + ":--" + c.timestamp().toDateTime() + "--:" + window.toString() + ":"
              + window.maxTimestamp().toDateTime() + ":" + c.pane());
    }
  }

  private static class PrintingDoFn1 extends DoFn<Long, String> {

    @ProcessElement public void processElement(ProcessContext c, BoundedWindow window) {
      c.output(
          c.element() + ":" + c.timestamp().getMillis() + ":" + window.maxTimestamp().getMillis());
      System.out.println(
          c.element() + ":--" + c.timestamp().toDateTime() + "--:" + window.toString() + ":"
              + window.maxTimestamp().toDateTime() + ":" + c.pane());
    }
  }
}
