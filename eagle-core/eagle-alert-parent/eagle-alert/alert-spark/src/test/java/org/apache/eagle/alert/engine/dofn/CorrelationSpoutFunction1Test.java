package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class CorrelationSpoutFunction1Test implements Serializable {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testCorrelationSpoutFunction() {
    SpoutSpec newSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testSpoutSpec.json"), SpoutSpec.class);
    PCollectionView<SpoutSpec> specView = p.apply("getSpec", Create.of(newSpec))
        .apply(View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    RouterSpec routerSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamRouterBoltSpec.json"),
            RouterSpec.class);
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());
    long starttime = 1496638588877L;
    PCollection<KV<Integer, PartitionedEvent>> input = p.apply("create message", Create.of(KV
        .of("oozie",
            "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":\""
                + starttime + "\"}"))).apply(new CorrelationSpoutFunction1(specView, sdsView, 10));

    Map<StreamPartition, StreamSortSpec> sss = routerSpec.makeSSS();

    Set<StreamPartition> sps = sss.keySet();
    for (StreamPartition sp : sps) {
      PCollectionView<StreamPartition> sssView = p
          .apply("get current sp" + sp.toString(), Create.of(sp))
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
      Assert.assertTrue(
          c.element().toString().startsWith("KV{3, PartitionedEvent[partition=StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]],event=StreamEvent[stream=OOZIESTREAM"));
      System.out.println("PrintinDoFn1" + c.element());
    }
  }
}