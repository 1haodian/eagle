/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.PeventFactory;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FindNeedHandleEventTest implements Serializable {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testFindNeedHandleEvent() {

    PCollectionView<RouterSpec> routerSpecView = p.apply("getRouterSpec", Create.of(SpecFactory.createRouterSpec()))
        .apply(View.asSingleton());

    PCollectionView<Map<String, StreamDefinition>> sdsView = p.apply("getSds", Create
        .of(KV.of("oozieStream", SpecFactory.createSds().get("oozieStream")), KV.of("hdfs", new StreamDefinition())))
        .apply("viewTags", View.asMap());

    PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView = p
        .apply("getSSS", Create.of(SpecFactory.createRouterSpec().makeSSS())).apply("ViewSSSAsMap", View.asMap());
    PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView = p
        .apply("getSRS", Create.of(SpecFactory.createRouterSpec().makeSRS())).apply("ViewSRSAsMap", View.asMap());

    TupleTag<PartitionedEvent> needWindow = new TupleTag<PartitionedEvent>(
        "needWindow") {

    };
    TupleTag<PartitionedEvent> noneedWindow = new TupleTag<PartitionedEvent>(
        "noneedWindow") {

    };


    List<PartitionedEvent> pevents = Arrays.asList(PeventFactory.createPevent1(), new PartitionedEvent());

    PCollection<PartitionedEvent> input = p
        .apply(Create.of(pevents).withCoder(SerializableCoder.of(PartitionedEvent.class)));
    PCollectionTuple output = input//.apply("GroupByKey", GroupByKey.create())
        .apply("Find need handle",
            new FindNeedWindowEventFunction(routerSpecView, sdsView, sssView, srsView));
    output.get(noneedWindow).apply("print1", ParDo.of(new PrintinDoFn1()));
    PAssert.that(output.get(needWindow)).containsInAnyOrder(PeventFactory.createPevent1());
    output.get(needWindow).apply("print2", ParDo.of(new PrintinDoFn2()));
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
