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

package org.apache.eagle.alert.engine.evaluator.impl;

import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.output.StreamCallback;

import java.util.List;

public class AlertStreamCallbackBeam extends StreamCallback {

    private static final Logger LOG = LoggerFactory.getLogger(AlertStreamCallbackBeam.class);
    private final PolicyDefinition policyDefinition;
    private final StreamDefinition streamDefinition;
    private final StreamPartition sp;
    private final String outputStream;
    private final List<AlertStreamEvent> results;

    public AlertStreamCallbackBeam(PolicyDefinition policyDefinition,
                                   StreamDefinition streamDefinition, String outputStream, StreamPartition sp, List<AlertStreamEvent> results) {
        this.policyDefinition = policyDefinition;
        this.streamDefinition = streamDefinition;
        this.outputStream = outputStream;
        this.sp = sp;
        this.results = results;
    }

    @Override
    public void receive(Event[] events) {
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
            event.setPolicyId(policyName);
            event.setCreatedBy("Beam engine " + sp.toString() + siteId);
            event.setCreatedTime(System.currentTimeMillis());
            event.setSchema(streamDefinition);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Generate new alert event: {}", event);
            }
            results.add(event);
        }
    }
}