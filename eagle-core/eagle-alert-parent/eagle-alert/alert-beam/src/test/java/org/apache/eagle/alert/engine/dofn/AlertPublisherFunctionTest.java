package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.commons.io.FileUtils;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.factory.AlertStreamEventFactory;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.publisher.PublishConstants;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AlertPublisherFunctionTest {

  @Rule public final transient TestPipeline p = TestPipeline.create();
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Test public void testAlertPublisherFunction() throws IOException {

    Map<String, Object> properties = new HashMap<>();
    properties.put(PublishConstants.ROTATE_EVERY_KB, 1);
    properties.put(PublishConstants.NUMBER_OF_FILES, 1);
    File file = testFolder.newFile("eagle-alert.log");
    String path = file.getAbsolutePath();
    System.out.println("OS current temporary directory is " + path);
    properties.put(PublishConstants.FILE_NAME, path);

    PCollectionView<AlertBoltSpec> alertBoltSpecView = p
        .apply("get alert spec", Create.of(SpecFactory.createAlertSpec()))
        .apply("alert view", View.asSingleton());

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", SpecFactory.createSds().get("oozieStream")),
            KV.of("hdfs", new StreamDefinition()))).apply("viewTags", View.asMap());
    PublishSpec publishSpec = SpecFactory.createPublishSpec();
    publishSpec.getPublishments().get(0).setProperties(properties);

    PCollectionView<PublishSpec> publishSpecView = p
        .apply("get publish spec", Create.of(publishSpec))
        .apply("publish view", View.asSingleton());

    Config config = ConfigFactory.load();
    p.apply("events",
        Create.of(KV.of("file-testAlertStream", AlertStreamEventFactory.createAlertEvent())))
        .apply("group by key", GroupByKey.create()).apply("publish",
        new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView, config))
        .apply(ParDo.of(new PrintinDoFn(path)));

    p.run();
  }

  private static class PrintinDoFn extends DoFn<String, String> {

    private String path;

    public PrintinDoFn(String path) {
      this.path = path;
    }

    @ProcessElement public void processElement(ProcessContext c) throws IOException {
      File log = new File(path);
      String logContent = FileUtils.readFileToString(log);
      System.out.println(logContent);
      Assert.assertEquals(
          "siteId\":\"yhd\",\"appIds\":[null],\"policyId\":\"policy4\",\"policyValue\":\"from oozieStream#window.externalTime(timestamp, 4 sec)  select ip, jobId , operation, count(ip) as visitCount group by operation insert into testAlertStream; \",\"alertTimestamp\":0,\"alertData\":{\"jobId\":2,\"ip\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\",\"operation\":\"yyy.yyy.yyy.yyy\",\"timestamp\":\"start\"},\"alertSubject\":\"\",\"alertBody\":\"\",\"streamId\":\"testAlertStream\",\"createdBy\":\"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]yhd, metaVersion=null\",\"createdTime\":0}",
          logContent.substring(71).trim());
      Assert.assertEquals("success alert publishID file-testAlertStreamresult size 1", c.element());
    }
  }
}
