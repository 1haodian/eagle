package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.TimestampedValue;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.factory.PeventFactory;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

public class WindowFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testaccumulateModeEventTimeTrigger() {
    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(PeventFactory.createPevent2(), new Instant(PeventFactory.createPevent2().getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(1))
        .advanceWatermarkTo(new Instant(0).plus(Duration.standardMinutes(6)))
        .addElements(TimestampedValue.of(
            PeventFactory.createPevent1(), new Instant(PeventFactory.createPevent1().getTimestamp())))
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

    IntervalWindow window = new IntervalWindow(new Instant(0), new Instant(0).plus(Duration.standardMinutes(5L)));
    PAssert.that(triggered).inOnTimePane(window).containsInAnyOrder(PeventFactory.createPevent2());
    PAssert.that(triggered).inFinalPane(window).containsInAnyOrder(PeventFactory.createPevent2());
    p.run();
  }

  @Test public void testaccumulateModeProcessingTimeTrigger() {
  

    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(PeventFactory.createPevent2(), new Instant(PeventFactory.createPevent2().getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(12))
        .addElements(TimestampedValue.of(PeventFactory.createPevent1(), new Instant(PeventFactory.createPevent1().getTimestamp())))
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
    PAssert.that(triggered).inFinalPane(window).containsInAnyOrder(PeventFactory.createPevent1(), PeventFactory.createPevent2());
    PAssert.that(triggered).inOnTimePane(window).containsInAnyOrder(PeventFactory.createPevent1(), PeventFactory.createPevent2());
    p.run();
  }


  @Test public void testdiscardModeProcessingTimeTrigger() {

    TestStream<PartitionedEvent> source = TestStream
        .create(SerializableCoder.of(PartitionedEvent.class))
        .addElements(TimestampedValue.of(PeventFactory.createPevent2(), new Instant(PeventFactory.createPevent2().getTimestamp())))
        .advanceProcessingTime(Duration.standardMinutes(12))
        .addElements(TimestampedValue.of(PeventFactory.createPevent1(), new Instant(PeventFactory.createPevent1().getTimestamp())))
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
    PCollectionView<SpoutSpec> specView = p
       // .apply(TextIO.read().from("/local/path/to/file.txt"))
        .apply("getSpec", Create.of(SpecFactory.createSpoutSpec())).apply("SpoutSpec windows",
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
