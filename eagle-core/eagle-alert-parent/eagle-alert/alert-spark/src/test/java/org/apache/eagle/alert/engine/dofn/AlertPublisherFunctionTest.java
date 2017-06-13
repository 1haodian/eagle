package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.factory.AlertStreamEventFactory;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

public class AlertPublisherFunctionTest {

  @Rule public final transient TestPipeline p = TestPipeline.create();
  @Test public void testAlertPublisherFunction() {


    PCollectionView<AlertBoltSpec> alertBoltSpecView = p
        .apply("get alert spec", Create.of(SpecFactory.createAlertSpec())).apply("alert view", View.asSingleton());

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", SpecFactory.createSds().get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PublishSpec publishSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testPublishSpec.json"),
            PublishSpec.class);

    PCollectionView<PublishSpec> publishSpecView = p
        .apply("get publish spec", Create.of(publishSpec))
        .apply("publish view", View.asSingleton());


    Config config = ConfigFactory.load();
    p.apply("events", Create.of(KV.of("file-testAlertStream",
        AlertStreamEventFactory.createAlertEvent())))
        .apply("group by key", GroupByKey.create()).apply("publish",
        new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView,
            config)).apply(ParDo.of(new PrintinDoFn()));
    p.run();

  }

  private static class PrintinDoFn extends DoFn<String, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println(c.element());
      Assert.assertEquals("success alert publishID file-testAlertStreamresult size 1", c.element());
    }
  }
}
