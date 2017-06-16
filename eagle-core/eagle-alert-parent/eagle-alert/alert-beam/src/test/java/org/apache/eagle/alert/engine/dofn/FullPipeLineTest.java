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

import com.google.common.collect.Lists;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.testing.TestStream;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.*;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class FullPipeLineTest implements Serializable {
    @Rule
    public final transient TestPipeline p = TestPipeline.create();
    private int numOfRouterBolts = 10;
    private final TupleTag<SpoutSpec> spoutSpecTupleTag = new TupleTag<SpoutSpec>() {

    };
    private final TupleTag<RouterSpec> routerSpecTupleTag = new TupleTag<RouterSpec>() {

    };
    private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag = new TupleTag<AlertBoltSpec>() {

    };
    private final TupleTag<PublishSpec> publishSpecTupleTag = new TupleTag<PublishSpec>() {

    };
    private final TupleTag<Map<String, StreamDefinition>> sdsTag = new TupleTag<Map<String, StreamDefinition>>() {

    };
    private final TupleTag<List<StreamPartition>> spTag = new TupleTag<List<StreamPartition>>() {

    };

    private final TupleTag<Map<StreamPartition, StreamSortSpec>> sssTag = new TupleTag<Map<StreamPartition, StreamSortSpec>>() {

    };
    private final TupleTag<Map<StreamPartition, List<StreamRouterSpec>>> srsTag = new TupleTag<Map<StreamPartition, List<StreamRouterSpec>>>() {

    };

    private final TupleTag<PartitionedEvent> needWindow = new TupleTag<PartitionedEvent>(
            "needWindow") {
    };
    private final TupleTag<PartitionedEvent> noneedWindow = new TupleTag<PartitionedEvent>(
            "noneedWindow") {
    };

    @Test
    public void testFullPipeLine() {

        long starttime = 1496638588877L;
        TestStream<KV<String, String>> source = TestStream
                .create(KvCoder.of(StringUtf8Coder.of(), StringUtf8Coder.of())).addElements(KV.of("oozie",
                        "{\"ip\":\"yyy.yyy.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":"
                                + starttime + "}")).addElements(KV.of("oozie",
                        "{\"ip\":\"yyy.xxx.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":"
                                + (starttime + 1) + "}")).addElements(KV.of("oozie",
                        "{\"ip\":\"yyy.jjj.yyy.yyy\", \"jobId\":\"140648764-oozie-oozi-W2017-06-05 04:56:28\", \"operation\":\"start\", \"timestamp\":"
                                + -1496638588877L + "}")).advanceWatermarkToInfinity();

        PCollection<KV<String, String>> rawMessage = p.apply("get config by source", source);
        PCollection<KV<String, String>> windowedRawMsg = rawMessage.apply("get config windows",
                Window.<KV<String, String>>into(new GlobalWindows()).triggering(
                        AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(1)))
                        .discardingFiredPanes().withAllowedLateness(Duration.ZERO));


        PCollectionTuple rs = windowedRawMsg.apply(
                ParDo.of(new GetConfigFromFileFn(spoutSpecTupleTag, sdsTag, spTag, routerSpecTupleTag, publishSpecTupleTag, sssTag, srsTag, alertBoltSpecTupleTag))
                        .withOutputTags(spoutSpecTupleTag, TupleTagList.of(sdsTag).and(spTag).and(routerSpecTupleTag).and(publishSpecTupleTag).and(sssTag).and(srsTag).and(alertBoltSpecTupleTag)));


        PCollection<SpoutSpec> spoutSpec = rs.get(spoutSpecTupleTag);
        PCollection<RouterSpec> routerSpec = rs.get(routerSpecTupleTag);
        PCollection<PublishSpec> publishSpec = rs.get(publishSpecTupleTag);
        PCollection<AlertBoltSpec> alertBoltSpec = rs.get(alertBoltSpecTupleTag);
        PCollection<Map<String, StreamDefinition>> sds = rs.get(sdsTag);
        PCollection<List<StreamPartition>> sp = rs.get(spTag);
        PCollection<Map<StreamPartition, StreamSortSpec>> sss = rs.get(sssTag);
        PCollection<Map<StreamPartition, List<StreamRouterSpec>>> srs = rs.get(srsTag);

        PCollectionView<SpoutSpec> specView = spoutSpec.apply("SpoutSpec Latest", Latest.globally()).apply("SpoutSpec", View.asSingleton());
        PCollectionView<RouterSpec> routerView = routerSpec.apply("RouterSpec Latest", Latest.globally()).apply("RouterSpec", View.asSingleton());
        PCollectionView<PublishSpec> publishSpecView = publishSpec.apply("PublishSpec Latest", Latest.globally()).apply("PublishSpec", View.asSingleton());
        PCollectionView<AlertBoltSpec> alertBoltSpecView = alertBoltSpec.apply("AlertBoltSpec Latest", Latest.globally()).apply("AlertBoltSpec", View.asSingleton());
        PCollectionView<Map<String, StreamDefinition>> sdsView = sds.apply("sdsView Latest", Latest.globally()).apply("sdsView", View.asSingleton());
        PCollectionView<List<StreamPartition>> spView = sp.apply("spView Latest", Latest.globally()).apply("spView", View.asSingleton());
        PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView = sss.apply("sssView Latest", Latest.globally()).apply("sssView", View.asSingleton());
        PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView = srs.apply("srsView Latest", Latest.globally()).apply("srsView", View.asSingleton());


        PCollectionList<KV<Integer, PartitionedEvent>> parts = rawMessage.apply(new CorrelationSpoutFunction(specView, sdsView, numOfRouterBolts));
        List<StreamPartition> sps = Lists.newArrayList(SpecFactory.createRouterSpec().makeSSS().keySet());
        for (int i = 0; i < numOfRouterBolts; i++) {
            PCollection<KV<Integer, PartitionedEvent>> partition = parts.get(i);
            PCollectionTuple output = partition.apply("Values " + i, Values.create())
                    .apply("Find need handle " + i,
                            new FindNeedWindowEventFunction(routerView, sdsView, sssView, srsView));
            PCollection<PartitionedEvent> directEmitPevents = output.get(noneedWindow);
            directEmitPevents.apply("direct send to siddhi windows" + i,
                    Window.<PartitionedEvent>into(new GlobalWindows()).triggering(
                            AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(1)))
                            .discardingFiredPanes().withAllowedLateness(Duration.ZERO))
                    .apply("direct send to siddhi with keys " + i, WithKeys.of(i))
                    .apply("direct send to siddhi group by key " + i, GroupByKey.create())
                    .apply("direct send to siddhi Values " + i, Values.create())
                    .apply("direct send to siddhi " + i, new AlertBoltFunction(alertBoltSpecView, sdsView, false))
                    .apply("group by publish id " + i, GroupByKey.create())
                    .apply("publish " + i,
                            new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView, ConfigFactory.load()));

            PCollectionList<KV<Integer, PartitionedEvent>> pevents = output.get(needWindow)
                    .apply("covert to (streampartition->pevent)" + i, new StreamPartitionFunction(spView, sps.size()));


            for (int j = 0; j < sps.size(); j++) {
                PCollection<KV<Integer, PartitionedEvent>> peventInPart = pevents.get(j);
                StreamPartition streamPartition = sps.get(j);
                String period = streamPartition.getSortSpec().getWindowPeriod();
                Duration flush = Duration.parse(period).plus(streamPartition.getSortSpec().getWindowMargin());
                String partitionAndBoltNum = streamPartition + "" + i;
                WindowFn<Object, IntervalWindow> windowFn = FixedWindows.of(Duration.parse(period));
                PCollection<KV<Integer, PartitionedEvent>> windowedPevents = peventInPart
                        .apply("window " + partitionAndBoltNum,
                                Window.<KV<Integer, PartitionedEvent>>into(windowFn).triggering(
                                        AfterWatermark.pastEndOfWindow().withEarlyFirings(
                                                AfterProcessingTime.pastFirstElementInPane()
                                                        .plusDelayOf(flush))).discardingFiredPanes()
                                        .withAllowedLateness(Duration.ZERO));


                windowedPevents.apply("group by partition " + partitionAndBoltNum, GroupByKey.create())
                        .apply("Values " + partitionAndBoltNum, Values.create())
                        .apply("alert bolt " + partitionAndBoltNum, new AlertBoltFunction(alertBoltSpecView, sdsView))
                        .apply("group by publish id " + partitionAndBoltNum, GroupByKey.create())
                        .apply("publish " + partitionAndBoltNum,
                                new AlertPublisherFunction(publishSpecView, alertBoltSpecView, sdsView, ConfigFactory.load()));
            }
        }
        p.run();
    }
}
