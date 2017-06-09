package org.apache.eagle.alert.engine.evaluator.impl;

import com.google.common.collect.ImmutableList;
import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.output.StreamCallback;

import java.util.ArrayList;
import java.util.List;

public class AlertStreamCallbackBeam extends StreamCallback {

  private static final Logger LOG = LoggerFactory.getLogger(AlertStreamCallbackBeam.class);
  private final PolicyDefinition policyDefinition;
  private final StreamDefinition streamDefinition;
  private final String outputStream;
  private final List<AlertStreamEvent> results = new ArrayList<>();

  public AlertStreamCallbackBeam(PolicyDefinition policyDefinition,StreamDefinition streamDefinition,String outputStream) {
    this.policyDefinition = policyDefinition;
    this.streamDefinition = streamDefinition;
    this.outputStream =outputStream;
  }

  @Override public void receive(Event[] events) {
    String policyName = policyDefinition.getName();
    String siteId = policyDefinition.getSiteId();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Generated {} alerts from policy '{}'", events.length, policyName);
    }
    for (Event e : events) {
      AlertStreamEvent event = new AlertStreamEvent();
      event.setSiteId(siteId);
      event.setTimestamp(e.getTimestamp());
      event.setData(e.getData());
      event.setStreamId(outputStream);
      event.setPolicyId(policyName + siteId);
      event.setCreatedBy(policyName + siteId);
      event.setCreatedTime(System.currentTimeMillis());
      event.setSchema(streamDefinition);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Generate new alert event: {}", event);
      }
      results.add(event);
    }
  }

  public List<AlertStreamEvent> getResults(){
    return ImmutableList.copyOf(results);
  }
}
