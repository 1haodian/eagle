/**
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
 */

package org.apache.eagle.alert.engine.spark.function;

import com.typesafe.config.Config;
import kafka.common.TopicAndPartition;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.PublishPartition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.spark.model.KafkaClusterInfo;
import org.apache.eagle.alert.engine.spark.model.PublishState;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.kafka.KafkaCluster;
import org.apache.spark.streaming.kafka.OffsetRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Predef;
import scala.Tuple2;
import scala.collection.JavaConversions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class Publisher4MultiKafka implements VoidFunction<JavaPairRDD<PublishPartition, Iterable<AlertStreamEvent>>> {

    private static final long serialVersionUID = 5514589101211710289L;
    private String alertPublishBoltName;
    private AtomicReference<PublishSpec> publishSpecRef;
    private String groupId;
    private PublishState publishState;
    private AtomicReference<Map<KafkaClusterInfo, OffsetRange[]>> offsetRangesClusterMapRef;
    private Config config;
    private static final Logger LOG = LoggerFactory.getLogger(Publisher4MultiKafka.class);

    public Publisher4MultiKafka(String alertPublishBoltName, String groupId, AtomicReference<Map<KafkaClusterInfo, OffsetRange[]>> offsetRangesClusterMapRef,
                                PublishState publishState, AtomicReference<PublishSpec> publishSpecRef, Config config) {
        this.alertPublishBoltName = alertPublishBoltName;
        this.groupId = groupId;
        this.offsetRangesClusterMapRef = offsetRangesClusterMapRef;
        this.publishSpecRef = publishSpecRef;
        this.publishState = publishState;
        this.config = config;
    }

    @Override
    public void call(JavaPairRDD<PublishPartition, Iterable<AlertStreamEvent>> rdd) throws Exception {
        rdd.foreachPartition(new AlertPublisherBoltFunction(publishSpecRef, alertPublishBoltName, publishState, config));
        updateOffset();
    }

    private void updateOffset() {
        Map<KafkaClusterInfo, OffsetRange[]> offsetRangesClusterMap = offsetRangesClusterMapRef.get();
        for (KafkaClusterInfo kafkaClusterInfo : offsetRangesClusterMap.keySet()) {
            OffsetRange[] offsetRanges = offsetRangesClusterMap.get(kafkaClusterInfo);
            KafkaCluster kafkaCluster = kafkaClusterInfo.getKafkaCluster();
            if (offsetRanges != null) {
                for (OffsetRange offsetRange : offsetRanges) {
                    TopicAndPartition topicAndPartition = new TopicAndPartition(offsetRange.topic(), offsetRange.partition());
                    Map<TopicAndPartition, Object> topicAndPartitionObjectMap = new HashMap<>();
                    topicAndPartitionObjectMap.put(topicAndPartition, offsetRange.untilOffset());
                    scala.collection.mutable.Map<TopicAndPartition, Object> map = JavaConversions.mapAsScalaMap(topicAndPartitionObjectMap);
                    scala.collection.immutable.Map<TopicAndPartition, Object> scalatopicAndPartitionObjectMap =
                        map.toMap(new Predef.$less$colon$less<Tuple2<TopicAndPartition, Object>, Tuple2<TopicAndPartition, Object>>() {
                            public Tuple2<TopicAndPartition, Object> apply(Tuple2<TopicAndPartition, Object> v1) {
                                return v1;
                            }
                        });
                    LOG.info("Updating offsets: {}", scalatopicAndPartitionObjectMap);
                    kafkaCluster.setConsumerOffsets(groupId, scalatopicAndPartitionObjectMap);
                }
            }
        }
    }
}
