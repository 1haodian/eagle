/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.eagle.alert.engine.evaluator.impl;

import java.io.Serializable;
import java.util.*;

import org.apache.eagle.alert.engine.AlertStreamCollector;
import org.apache.eagle.alert.engine.StreamContext;
import org.apache.eagle.alert.engine.coordinator.PublishPartition;
import org.apache.eagle.alert.engine.coordinator.Publishment;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.router.StreamOutputCollector;
import org.apache.eagle.alert.engine.router.impl.SparkOutputCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AlertBoltOutputCollectorWrapper implements AlertStreamCollector, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(AlertBoltOutputCollectorWrapper.class);
    private static final long serialVersionUID = -6208313761267299408L;

    private final StreamOutputCollector delegate;
    private final transient Object outputLock;
    private final StreamContext streamContext;

    private volatile Set<PublishPartition> publishPartitions;

    public AlertBoltOutputCollectorWrapper(StreamOutputCollector outputCollector, Object outputLock,
                                           StreamContext streamContext) {
        this.delegate = outputCollector;
        this.outputLock = outputLock;
        this.streamContext = streamContext;

        this.publishPartitions = new HashSet<>();
    }

    public AlertBoltOutputCollectorWrapper(StreamOutputCollector outputCollector, StreamContext streamContext, Set<PublishPartition> publishPartitions) {
        this.delegate = outputCollector;
        this.outputLock = new Object();
        this.streamContext = streamContext;

        this.publishPartitions = publishPartitions;
    }

    @Override
    public void emit(AlertStreamEvent event) {
        if (event == null) {
            return;
        }
        event.ensureAlertId();
        Set<PublishPartition> clonedPublishPartitions = new HashSet<>(publishPartitions);
        for (PublishPartition publishPartition : clonedPublishPartitions) {
            // skip the publish partition which is not belong to this policy and also check streamId
            PublishPartition cloned = publishPartition.clone();
            Optional.ofNullable(event)
                    .filter(x -> x != null
                            && x.getSchema() != null
                            && cloned.getPolicyId().equalsIgnoreCase(x.getPolicyId())
                            && (cloned.getStreamId().equalsIgnoreCase(x.getSchema().getStreamId())
                            || cloned.getStreamId().equalsIgnoreCase(Publishment.STREAM_NAME_DEFAULT)))
                    .ifPresent(x -> {
                        cloned.getColumns().stream()
                                .filter(y -> event.getSchema().getColumnIndex(y) >= 0
                                        && event.getSchema().getColumnIndex(y) < event.getSchema().getColumns().size())
                                .map(y -> event.getData()[event.getSchema().getColumnIndex(y)])
                                .filter(y -> y != null)
                                .forEach(y -> cloned.getColumnValues().add(y));
                        synchronized (outputLock) {
                            streamContext.counter().incr("alert_count");
                            delegate.emit(Arrays.asList(cloned, event));
                        }
                    });
        }
    }

    @Override
    public void flush() {
        // do nothing
    }

    @Override
    public void close() {
    }

    public List emitAll() {
        if (this.delegate instanceof SparkOutputCollector) {
            SparkOutputCollector sparkOutputCollector = (SparkOutputCollector) delegate;
            return sparkOutputCollector.flushAlertStreamEvent();
        }
        return Collections.emptyList();
    }


    public synchronized void onAlertBoltSpecChange(Collection<PublishPartition> addedPublishPartitions,
                                                   Collection<PublishPartition> removedPublishPartitions,
                                                   Collection<PublishPartition> modifiedPublishPartitions) {
        Set<PublishPartition> clonedPublishPartitions = new HashSet<>(publishPartitions);
        clonedPublishPartitions.addAll(addedPublishPartitions);
        clonedPublishPartitions.removeAll(removedPublishPartitions);
        clonedPublishPartitions.addAll(modifiedPublishPartitions);
        publishPartitions = clonedPublishPartitions;
    }

    public Set<PublishPartition> getPublishPartitions() {
        return this.publishPartitions;
    }
}