package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.engine.coordinator.*;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class AlertBoltFunctionTest {

  @Rule public final transient TestPipeline p = TestPipeline.create();

  @Test public void testAlertBoltFunction() {
    AlertBoltSpec alertBoltSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testAlertBoltSpec.json"),
            AlertBoltSpec.class);
    PolicyDefinition policyDefinition1 = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPolicy1.json"),
            PolicyDefinition.class);
    PolicyDefinition policyDefinition2 = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPolicy2.json"),
            PolicyDefinition.class);
    alertBoltSpec.getBoltPoliciesMap().put("alertbolt1", Lists.newArrayList(policyDefinition1));
    String policyName1 = policyDefinition1.getName();
    alertBoltSpec.addBoltPolicy("alertbolt1", policyName1);
    String policyName2 = policyDefinition1.getName();
    alertBoltSpec.addBoltPolicy("alertbolt2", policyName2);
    alertBoltSpec.getBoltPoliciesMap().put("alertbolt2", Lists.newArrayList(policyDefinition2));
    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("sds windows", Window.<KV<String, StreamDefinition>>into(new GlobalWindows())
            .triggering(AfterProcessingTime.pastFirstElementInPane()
                .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
            .withAllowedLateness(Duration.ZERO)).apply(View.asMap());

    PCollectionView<AlertBoltSpec> alertBoltSpecView = p
        .apply("get alert spec", Create.of(alertBoltSpec)).apply("AlertBoltSpec windows",
            Window.<AlertBoltSpec>into(new GlobalWindows()).triggering(
                AfterProcessingTime.pastFirstElementInPane()
                    .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
                .withAllowedLateness(Duration.ZERO)).apply(View.asSingleton());
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();
    StreamSortSpec streamSortSpec = new StreamSortSpec();
    streamSortSpec.setWindowMargin(1000);
    streamSortSpec.setWindowPeriod("PT4S");
    streamPartition.setStreamId("oozieStream");
    streamPartition.setType(StreamPartition.Type.GROUPBY);
    streamPartition.setColumns(Lists.newArrayList("operation"));
    streamPartition.setSortSpec(streamSortSpec);
    StreamEvent streamEvent1 = new StreamEvent();
    streamEvent1.setStreamId("oozieStream");
    streamEvent1.setTimestamp(1496638588877L);
    streamEvent1.setData(
        new Object[] { "yyy.yyy.yyy.yyy", "140648764-oozie-oozi-W2017-06-05 04:56:28", "start",
            1496638588877L });
    pevent1.setEvent(streamEvent1);

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

    List<PartitionedEvent> events = Lists.newArrayList(pevent1, pevent2);

    PCollection<AlertStreamEvent> rs = p.apply("events", Create.of(events)).apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create())
        .apply(new AlertBoltFunction(alertBoltSpecView, sdsView));
    AlertStreamEvent alert = new AlertStreamEvent();
    alert.setTimestamp(1L);
    alert.setSiteId("yhd");
    alert.setStreamId("testAlertStream");
    alert.setData(new Object[]{"140648764-oozie-oozi-W2017-06-05 04:56:28",1,"yyy.yyy.yyy.yyy",  "start"});

//    PAssert.that(rs).containsInAnyOrder(alert);
    rs.apply(ParDo.of(new PrintinDoFn1()));
    p.run();
  }

  @Test public void testAlertBoltFunctionTwoEventGroupBy() {
    AlertBoltSpec alertBoltSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testAlertBoltSpec.json"),
            AlertBoltSpec.class);
    PolicyDefinition policyDefinition1 = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPolicy1.json"),
            PolicyDefinition.class);
    PolicyDefinition policyDefinition2 = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPolicy2.json"),
            PolicyDefinition.class);
    alertBoltSpec.getBoltPoliciesMap().put("alertbolt1", Lists.newArrayList(policyDefinition1));
    String policyName1 = policyDefinition1.getName();
    alertBoltSpec.addBoltPolicy("alertbolt1", policyName1);
    String policyName2 = policyDefinition1.getName();
    alertBoltSpec.addBoltPolicy("alertbolt2", policyName2);
    alertBoltSpec.getBoltPoliciesMap().put("alertbolt2", Lists.newArrayList(policyDefinition2));
    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("sds windows", Window.<KV<String, StreamDefinition>>into(new GlobalWindows())
            .triggering(AfterProcessingTime.pastFirstElementInPane()
                .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
            .withAllowedLateness(Duration.ZERO)).apply(View.asMap());

    PCollectionView<AlertBoltSpec> alertBoltSpecView = p
        .apply("get alert spec", Create.of(alertBoltSpec)).apply("AlertBoltSpec windows",
            Window.<AlertBoltSpec>into(new GlobalWindows()).triggering(
                AfterProcessingTime.pastFirstElementInPane()
                    .plusDelayOf(Duration.standardSeconds(10))).discardingFiredPanes()
                .withAllowedLateness(Duration.ZERO)).apply(View.asSingleton());
    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();
    StreamSortSpec streamSortSpec = new StreamSortSpec();
    streamSortSpec.setWindowMargin(1000);
    streamSortSpec.setWindowPeriod("PT4S");
    streamPartition.setStreamId("oozieStream");
    streamPartition.setType(StreamPartition.Type.GROUPBY);
    streamPartition.setColumns(Lists.newArrayList("operation"));
    streamPartition.setSortSpec(streamSortSpec);
    StreamEvent streamEvent1 = new StreamEvent();
    streamEvent1.setStreamId("oozieStream");
    streamEvent1.setTimestamp(1496638588877L);
    streamEvent1.setData(
        new Object[] { "yyy.yyy.yyy.yyy", "140648764-oozie-oozi-W2017-06-05 04:56:28", "start",
            1496638588877L });
    pevent1.setEvent(streamEvent1);

    pevent1.getEvent().setTimestamp(1);
    pevent1.setPartition(streamPartition);

    PartitionedEvent pevent2 = new PartitionedEvent();
    StreamPartition streamPartition2 = new StreamPartition();
    StreamSortSpec streamSortSpec2 = new StreamSortSpec();
    streamSortSpec2.setWindowMargin(1000);
    streamSortSpec2.setWindowPeriod("PT4S");
    streamPartition2.setStreamId("oozieStream");
    streamPartition2.setType(StreamPartition.Type.GROUPBY);
    streamPartition2.setColumns(Lists.newArrayList("operation"));
    streamPartition2.setSortSpec(streamSortSpec2);

    StreamEvent streamEvent2 = new StreamEvent();
    streamEvent2.setStreamId("oozieStream");
    streamEvent2.setTimestamp(1496638588877L);
    streamEvent2.setData(
        new Object[] { "yyy.yyy.yyy.yyy", "140648764-oozie-oozi-W2017-06-05 04:56:28", "start",
            1496638588877L });
    pevent1.setEvent(streamEvent2);

    pevent2.setEvent(streamEvent2);
    pevent2.getEvent().setTimestamp(13);
    pevent2.setPartition(streamPartition2);

    List<PartitionedEvent> events = Lists.newArrayList(pevent1, pevent2);

    PCollection<AlertStreamEvent> rs = p.apply("events", Create.of(events)).apply(WithKeys.of(1))
        .apply(GroupByKey.create()).apply(Values.create())
        .apply(new AlertBoltFunction(alertBoltSpecView, sdsView));
    rs.apply(ParDo.of(new PrintinDoFn2()));
    p.run();
  }

  private static class PrintinDoFn1 extends DoFn<AlertStreamEvent, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn1" + c.element());
      Assert.assertEquals(
          "Alert {site=yhd, stream=testAlertStream,timestamp=1970-01-01 00:00:00,001,data={jobId=140648764-oozie-oozi-W2017-06-05 04:56:28, visitCount=1, ip=yyy.yyy.yyy.yyy, operation=start}, policyId=policy4yhd, createdBy=StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]yhd, metaVersion=null}",
          c.element().toString());
    }
  }

  private static class PrintinDoFn2 extends DoFn<AlertStreamEvent, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn2" + c.element());
      Assert.assertEquals(
          "Alert {site=yhd, stream=testAlertStream,timestamp=1970-01-01 00:00:00,013,data={jobId=140648764-oozie-oozi-W2017-06-05 04:56:28, visitCount=2, ip=yyy.yyy.yyy.yyy, operation=start}, policyId=policy4yhd, createdBy=StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]yhd, metaVersion=null}",
          c.element().toString());
    }
  }
}
