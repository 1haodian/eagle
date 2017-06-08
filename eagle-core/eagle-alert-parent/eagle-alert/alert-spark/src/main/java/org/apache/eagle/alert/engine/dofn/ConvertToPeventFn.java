package org.apache.eagle.alert.engine.dofn;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.eagle.alert.coordination.model.*;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertToPeventFn extends DoFn<KV<String, String>, KV<Integer, PartitionedEvent>> {

  private static final Logger LOG = LoggerFactory.getLogger(CorrelationSpoutFunction.class);
  private PCollectionView<SpoutSpec> spoutSpecView;
  private PCollectionView<Map<String, StreamDefinition>> sdsView;
  private int numOfRouterBolts;
  private ObjectMapper mapper;
  private TypeReference<HashMap<String, Object>> typeRef;

  public ConvertToPeventFn(PCollectionView<SpoutSpec> spoutSpecView,
      PCollectionView<Map<String, StreamDefinition>> sdsView, int numOfRouterBolts) {
    this.spoutSpecView = spoutSpecView;
    this.sdsView = sdsView;
    this.numOfRouterBolts = numOfRouterBolts;
  }

  @Setup public void prepare() {
    mapper = new ObjectMapper();
    typeRef = new TypeReference<HashMap<String, Object>>() {

    };
  }

  @ProcessElement public void processElement(ProcessContext c) {

    Map<String, Object> value;
    KV<String, String> msg = c.element();
    try {
      value = mapper.readValue(msg.getValue(), typeRef);
    } catch (IOException e) {
      LOG.info("covert tuple value to map error");
      return;
    }

    List<Object> tuple = new ArrayList<>(2);
    String topic = msg.getKey();
    tuple.add(0, topic);
    tuple.add(1, value);
    SpoutSpec spoutSpec = c.sideInput(spoutSpecView);
    Tuple2StreamMetadata metadata = spoutSpec.getTuple2StreamMetadataMap().get(topic);
    if (metadata == null) {
      LOG.info(
          "tuple2StreamMetadata is null spout collector for topic {} see monitored metadata invalid, is this data source removed! ",
          topic);
    }
    Tuple2StreamConverter converter = new Tuple2StreamConverter(metadata);
    List<Object> tupleContent = converter.convert(tuple);

    List<StreamRepartitionMetadata> streamRepartitionMetadataList = spoutSpec
        .getStreamRepartitionMetadataMap().get(topic);
    if (streamRepartitionMetadataList == null) {
      LOG.info(
          "streamRepartitionMetadataList is nullspout collector for topic {} see monitored metadata invalid, is this data source removed! ",
          topic);
    }
    Map<String, Object> messageContent = (Map<String, Object>) tupleContent.get(3);
    Object streamId = tupleContent.get(1);
    Map<String, StreamDefinition> sds = c.sideInput(sdsView);
    StreamDefinition sd = sds.get(streamId);
    if (sd == null) {
      LOG.info("StreamDefinition {} is not found within {}, ignore this message", streamId, sds);
    }

    Long timestamp = (Long) tupleContent.get(2);
    StreamEvent event = convertToStreamEventByStreamDefinition(timestamp, messageContent,
        sds.get(streamId));

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
        if (mod >= groupingStrategy.startSequence
            && mod < groupingStrategy.startSequence + numOfRouterBolts) {
          PartitionedEvent pEvent = new PartitionedEvent(event, groupingStrategy.partition, hash);
          c.output(KV.of(mod, pEvent));
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Message filtered with mod {} not within range {} and {} for message {}", mod,
                groupingStrategy.startSequence, groupingStrategy.startSequence + numOfRouterBolts,
                tuple);
          }
        }
      }
    }
  }

  @SuppressWarnings("rawtypes") private int getRoutingHashByGroupingStrategy(Map data,
      StreamRepartitionStrategy gs) {
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

  @SuppressWarnings({ "rawtypes",
      "unchecked" }) private StreamEvent convertToStreamEventByStreamDefinition(long timestamp,
      Map messageContent, StreamDefinition sd) {
    return StreamEvent.builder().timestamep(timestamp).attributes(messageContent, sd).build();
  }
}
