package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
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

    TupleTag<PartitionedEvent> peventNeedHandle = new TupleTag<PartitionedEvent>(
        "peventNeedHandle") {

    };
    TupleTag<PartitionedEvent> peventNOTNeedHandle = new TupleTag<PartitionedEvent>(
        "peventNOTNeedHandle") {

    };

    PartitionedEvent pevent1 = new PartitionedEvent();
    StreamPartition streamPartition = new StreamPartition();//"StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]"
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

    List<PartitionedEvent> pevents = Arrays.asList(pevent1, new PartitionedEvent());

    PCollection<PartitionedEvent> input = p
        .apply(Create.of(pevents).withCoder(SerializableCoder.of(PartitionedEvent.class)));
    PCollectionTuple output = input//.apply("GroupByKey", GroupByKey.create())
        .apply("Find need handle",
            new FindNeedHandleEventFunction(routerSpecView, sdsView, sssView, srsView));
    output.get(peventNOTNeedHandle).apply("print1", ParDo.of(new PrintinDoFn1()));
    PAssert.that(output.get(peventNeedHandle)).containsInAnyOrder(pevent1);
    output.get(peventNeedHandle).apply("print2", ParDo.of(new PrintinDoFn2()));
    p.run();
  }

  private static class PrintinDoFn2 extends DoFn<PartitionedEvent, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn2" + c.element());
    }
  }

  private static class PrintinDoFn1 extends DoFn<PartitionedEvent, String> {

    @ProcessElement public void processElement(ProcessContext c) {
      System.out.println("PrintinDoFn1" + c.element());
    }
  }
}
