package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

public class AlertPublisherFunctionTest {

  @Rule public final transient TestPipeline p = TestPipeline.create();
  @Test public void testAlertPublisherFunction() {

    AlertBoltSpec alertBoltSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testAlertBoltSpec.json"),
            AlertBoltSpec.class);

    alertBoltSpec.addPublishPartition("testAlertStream", "policy4", "testAlertPublish1",
        ImmutableSet.of("operation"));
    alertBoltSpec.addPublishPartition("testAlertStream", "policy5", "testAlertPublish2",
        ImmutableSet.of("operation"));
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

    PCollectionView<AlertBoltSpec> alertBoltSpecView = p
        .apply("get alert spec", Create.of(alertBoltSpec)).apply("alert view", View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PublishSpec publishSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPublishSpec.json"),
            PublishSpec.class);

    PCollectionView<PublishSpec> publishSpecView = p
        .apply("get publish spec", Create.of(publishSpec))
        .apply("publish view", View.asSingleton());

    AlertStreamEvent alertStreamEvent = new AlertStreamEvent();
    alertStreamEvent.setStreamId("testAlertStream");
    alertStreamEvent.setSiteId("yhd");
    alertStreamEvent.setTimestamp(13);
    alertStreamEvent.setData(
        new Object[] { "140648764-oozie-oozi-W2017-06-05 04:56:28", 2, "yyy.yyy.yyy.yyy",
            "start" });
    StreamDefinition schema = sds.get("oozieStream");
    alertStreamEvent.setSchema(schema);
    alertStreamEvent.setPolicyId("policy4");
    alertStreamEvent.setCreatedBy(
        "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]yhd, metaVersion=null");

    Config config = ConfigFactory.load();
    p.apply("events", Create.of(KV.of("file-testAlertStream", alertStreamEvent)))
        .apply("group by key", GroupByKey.create()).apply("publish",
        new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView,
            config));
    p.run();

  }
}
