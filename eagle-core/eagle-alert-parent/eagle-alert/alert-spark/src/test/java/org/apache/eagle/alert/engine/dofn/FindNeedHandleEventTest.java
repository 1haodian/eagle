package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.beam.sdk.coders.BigEndianIntegerCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FindNeedHandleEventTest implements Serializable {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testFindNeedHandleEvent() {
    PartitionedEvent pevent1 = new PartitionedEvent();
    pevent1.setEvent(new StreamEvent());
    pevent1.getEvent().setTimestamp(1);
    List<KV<Integer, PartitionedEvent>> pevents = Arrays
        .asList(KV.of(3, pevent1), KV.of(4, new PartitionedEvent()));

    PCollection<KV<Integer, PartitionedEvent>> input = p.apply(Create.of(pevents).withCoder(
        KvCoder.of(BigEndianIntegerCoder.of(), SerializableCoder.of(PartitionedEvent.class))));

    RouterSpec routerSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamRouterBoltSpec.json"),
            RouterSpec.class);
    PCollectionView<RouterSpec> routerSpecView = p.apply("getRouterSpec", Create.of(routerSpec))
        .apply(View.asSingleton());

    Map<String, StreamDefinition> sds = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testStreamDefinitionsSpec.json"),
            new TypeReference<Map<String, StreamDefinition>>() {

            });
    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", sds.get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView = p
        .apply("getSSS", Create.of(routerSpec.makeSSS())).apply("ViewSSSAsMap", View.asMap());
    PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView = p
        .apply("getSRS", Create.of(routerSpec.makeSRS())).apply("ViewSRSAsMap", View.asMap());

    TupleTag<KV<Integer, Iterable<PartitionedEvent>>> peventNeedHandle = new TupleTag<KV<Integer, Iterable<PartitionedEvent>>>(
        "peventNeedHandle") {

    };
    TupleTag<KV<Integer, Iterable<PartitionedEvent>>> peventNOTNeedHandle = new TupleTag<KV<Integer, Iterable<PartitionedEvent>>>(
        "peventNOTNeedHandle") {

    };
    PCollectionTuple output = input.apply("GroupByKey", GroupByKey.create())
        .apply("Find need handle",
            new FindNeedHandleEvent(routerSpecView, sdsView, sssView, srsView));
    output.get(peventNOTNeedHandle).apply("print1", ParDo.of(new PrintinDoFn1()));
    PAssert.that(output.get(peventNeedHandle)).satisfies(
        (SerializableFunction<Iterable<KV<Integer, Iterable<PartitionedEvent>>>, Void>) input1 -> {
          Iterator<KV<Integer, Iterable<PartitionedEvent>>> kvs = input1.iterator();
          Assert.assertTrue(kvs.hasNext());
          KV<Integer, Iterable<PartitionedEvent>> rs = kvs.next();
          Assert.assertEquals(new Integer(3), rs.getKey());
          Iterator<PartitionedEvent> rsitr = rs.getValue().iterator();
          Assert.assertTrue(rsitr.hasNext());
          Assert.assertEquals(pevent1, rsitr.next());
          return null;
        });

    PAssert.that(output.get(peventNOTNeedHandle)).satisfies(
        (SerializableFunction<Iterable<KV<Integer, Iterable<PartitionedEvent>>>, Void>) input1 -> {
          Iterator<KV<Integer, Iterable<PartitionedEvent>>> kvs = input1.iterator();
          Assert.assertTrue(kvs.hasNext());
          KV<Integer, Iterable<PartitionedEvent>> rs = kvs.next();
          Assert.assertEquals(new Integer(4), rs.getKey());
          Iterator<PartitionedEvent> rsitr = rs.getValue().iterator();
          Assert.assertTrue(rsitr.hasNext());
          Assert.assertEquals(new PartitionedEvent(), rsitr.next());
          return null;
        });
    output.get(peventNeedHandle).apply("print2", ParDo.of(new PrintinDoFn2()));
    p.run();
  }

  private static class PrintinDoFn2 extends DoFn<KV<Integer, Iterable<PartitionedEvent>>, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn2" + c.element());
    }
  }

  private static class PrintinDoFn1 extends DoFn<KV<Integer, Iterable<PartitionedEvent>>, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn1" + c.element());
    }
  }
}
