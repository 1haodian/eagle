package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class StreamRouteBoltFunctionTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testStreamRouteBoltFunction() {

    List<KV<Integer, PartitionedEvent>> pevents = Arrays
        .asList(KV.of(3, new PartitionedEvent()), KV.of(4, new PartitionedEvent()));

    PCollection<KV<Integer, PartitionedEvent>> input = p.apply(Create.of(pevents).withCoder(
        KvCoder.of(BigEndianIntegerCoder.of(), SerializableCoder.of(PartitionedEvent.class))));

    RouterSpec routerSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamRouterBoltSpec.json"),
            RouterSpec.class);
    PCollectionView<RouterSpec> routerSpecView = p
        .apply("getRouterSpec", Create.of(routerSpec)).apply(View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView = null;
    PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView= null;

    input.apply(new StreamRouteBoltFunction(routerSpecView, sdsView, sssView, srsView));
    p.run();
  }
}
