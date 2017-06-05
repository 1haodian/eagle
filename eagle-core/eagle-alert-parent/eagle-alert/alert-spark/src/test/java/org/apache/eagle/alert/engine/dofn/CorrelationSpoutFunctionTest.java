package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class CorrelationSpoutFunctionTest implements Serializable {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testCorrelationSpoutFunctionEmpty() {

    SpoutSpec newSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testSpoutSpec.json"), SpoutSpec.class);
    PCollectionView<SpoutSpec> specView = p.apply("getSpec", Create.of(newSpec))
        .apply(View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PCollection<KV<Integer, PartitionedEvent>> input = p
        .apply("create message", Create.of(KV.of("oozie", "message")))
        .apply(new CorrelationSpoutFunction(specView, sdsView, 10));
    PAssert.that(input).containsInAnyOrder(Collections.emptyList());
    p.run();
  }

  @Test public void testCorrelationSpoutFunction() {
    SpoutSpec newSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testSpoutSpec.json"), SpoutSpec.class);
    PCollectionView<SpoutSpec> specView = p.apply("getSpec", Create.of(newSpec))
        .apply(View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());
    long starttime = 1496638588877L;
    PCollection<KV<Integer, PartitionedEvent>> input = p.apply("create message", Create.of(KV
        .of("oozie",
            "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":\""
                + starttime + "\"}"))).apply(new CorrelationSpoutFunction(specView, sdsView, 10));

    Map<String, Object> messageContent = new TreeMap<>();
    messageContent.put("jobId", "140648764-oozie-oozi-W2017-06-05 04:56:28");
    messageContent.put("ip", "yyy.yyy.yyy.yyy");
    messageContent.put("operation", "start");
    messageContent.put("timestamp", starttime);
    StreamPartition streamPartition = newSpec.getStreamRepartitionMetadataMap().get("oozie")
        .get(0).groupingStrategies.get(0).partition;
    StreamEvent streamEvent = new StreamEvent("OOZIESTREAM", starttime, new Object[] {});
    PartitionedEvent pevent = new PartitionedEvent(streamEvent, streamPartition, 109758167);

    PAssert.that(input)
        .satisfies((SerializableFunction<Iterable<KV<Integer, PartitionedEvent>>, Void>) kvs -> {
          Iterator<KV<Integer, PartitionedEvent>> itr = kvs.iterator();
          Assert.assertTrue(itr.hasNext());
          KV<Integer, PartitionedEvent> rs = itr.next();
          Assert.assertTrue(3L == rs.getKey());
          Assert.assertTrue(rs.getValue().toString().startsWith("PartitionedEvent[partition=StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]],event=StreamEvent[stream=OOZIESTREAM"));
          return null;
        });
    p.run();
  }
}
