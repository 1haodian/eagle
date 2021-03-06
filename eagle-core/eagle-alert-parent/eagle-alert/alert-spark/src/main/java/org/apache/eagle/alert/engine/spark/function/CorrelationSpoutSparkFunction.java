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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.eagle.alert.coordination.model.*;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CorrelationSpoutSparkFunction implements PairFlatMapFunction<Tuple2<String, String>, Integer, PartitionedEvent> {

    private static final long serialVersionUID = -5281723341236671580L;
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationSpoutSparkFunction.class);

    private int numOfRouterBolts;
    private AtomicReference<SpoutSpec> spoutSpecRef;
    private AtomicReference<Map<String, StreamDefinition>> sdsRef;

    public CorrelationSpoutSparkFunction(int numOfRouter, AtomicReference<SpoutSpec> spoutSpecRef, AtomicReference<Map<String, StreamDefinition>> sdsRef) {
        this.numOfRouterBolts = numOfRouter;
        this.spoutSpecRef = spoutSpecRef;
        this.sdsRef = sdsRef;
    }

    @Override
    public Iterator<Tuple2<Integer, PartitionedEvent>> call(Tuple2<String, String> message) {

        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        Map<String, Object> value;
        try {
            value = mapper.readValue(message._2, typeRef);
        } catch (IOException e) {
            LOG.info("covert tuple value to map error");
            return Collections.emptyIterator();
        }
        List<Object> tuple = new ArrayList<>(2);
        String topic = message._1;
        tuple.add(0, topic);
        tuple.add(1, value);
        SpoutSpec spoutSpec = spoutSpecRef.get();
        Tuple2StreamMetadata metadata = spoutSpec.getTuple2StreamMetadataMap().get(topic);
        if (metadata == null) {
            LOG.info(
                    "tuple2StreamMetadata is null spout collector for topic {} see monitored metadata invalid, is this data source removed! ", topic);
            return Collections.emptyIterator();
        }
        Tuple2StreamConverter converter = new Tuple2StreamConverter(metadata);
        List<Object> tupleContent = converter.convert(tuple);

        List<StreamRepartitionMetadata> streamRepartitionMetadataList = spoutSpec.getStreamRepartitionMetadataMap().get(topic);
        if (streamRepartitionMetadataList == null) {
            LOG.info(
                    "streamRepartitionMetadataList is nullspout collector for topic {} see monitored metadata invalid, is this data source removed! ", topic);
            return Collections.emptyIterator();
        }
        Map<String, Object> messageContent = (Map<String, Object>) tupleContent.get(3);
        Object streamId = tupleContent.get(1);
        Map<String, StreamDefinition> sds = sdsRef.get();
        StreamDefinition sd = sds.get(streamId);
        if (sd == null) {
            LOG.info("StreamDefinition {} is not found within {}, ignore this message", streamId, sds);
            return Collections.emptyIterator();
        }
        List<Tuple2<Integer, PartitionedEvent>> outputTuple2s = new ArrayList<>(5);

        Long timestamp = (Long) tupleContent.get(2);
        StreamEvent event = convertToStreamEventByStreamDefinition(timestamp, messageContent, sds.get(streamId));

        for (StreamRepartitionMetadata md : streamRepartitionMetadataList) {
            if (!event.getStreamId().equals(md.getStreamId())) {
                continue;
            }
            // one stream may have multiple group-by strategies, each strategy is for a specific group-by
            for (StreamRepartitionStrategy groupingStrategy : md.groupingStrategies) {
                int hash = 0;
                if (groupingStrategy.getPartition().getType().equals(StreamPartition.Type.GROUPBY)) {
                    hash = getRoutingHashByGroupingStrategy(messageContent, groupingStrategy);
                } else if (groupingStrategy.getPartition().getType().equals(StreamPartition.Type.SHUFFLE)) {
                    hash = Math.abs((int) System.currentTimeMillis());
                }
                int mod = hash % groupingStrategy.numTotalParticipatingRouterBolts;
                // filter out message
                if (mod >= groupingStrategy.startSequence && mod < groupingStrategy.startSequence + numOfRouterBolts) {
                    PartitionedEvent pEvent = new PartitionedEvent(event, groupingStrategy.partition, hash);
                    outputTuple2s.add(new Tuple2<>(mod, pEvent));
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Message filtered with mod {} not within range {} and {} for message {}", mod, groupingStrategy.startSequence,
                                groupingStrategy.startSequence + numOfRouterBolts, tuple);
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(outputTuple2s)) {
            return Collections.emptyIterator();
        }
        return outputTuple2s.iterator();
    }

    @SuppressWarnings("rawtypes")
    private int getRoutingHashByGroupingStrategy(Map data, StreamRepartitionStrategy gs) {
        // calculate hash value for values from group-by fields
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        for (String groupingField : gs.partition.getColumns()) {
            if (data.get(groupingField) != null) {
                hashCodeBuilder.append(data.get(groupingField));
            } else {
                LOG.warn("Required GroupBy fields {} not found: {}", gs.partition.getColumns(), data);
            }
        }
        int hash = hashCodeBuilder.toHashCode();
        hash = Math.abs(hash);
        return hash;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private StreamEvent convertToStreamEventByStreamDefinition(long timestamp, Map messageContent, StreamDefinition sd) {
        return StreamEvent.builder().timestamep(timestamp).attributes(messageContent, sd).build();
    }

}
