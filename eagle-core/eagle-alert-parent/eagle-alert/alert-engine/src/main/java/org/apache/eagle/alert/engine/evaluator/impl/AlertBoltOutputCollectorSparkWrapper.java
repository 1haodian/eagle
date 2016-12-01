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

package org.apache.eagle.alert.engine.evaluator.impl;

import org.apache.eagle.alert.engine.AlertStreamCollector;
import org.apache.eagle.alert.engine.coordinator.PublishPartition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import scala.Tuple2;

import java.io.Serializable;
import java.util.*;

public class AlertBoltOutputCollectorSparkWrapper implements AlertStreamCollector, Serializable {
    private final LinkedList<Tuple2<PublishPartition, AlertStreamEvent>> collector = new LinkedList<>();

    private Set<PublishPartition> publishPartitions;

    public AlertBoltOutputCollectorSparkWrapper(Set<PublishPartition> publishPartitions) {
        this.publishPartitions = publishPartitions;
    }

    @Override
    public void emit(AlertStreamEvent event) {

        Set<PublishPartition> clonedPublishPartitions = new HashSet<>(publishPartitions);
        for (PublishPartition publishPartition : clonedPublishPartitions) {
            PublishPartition cloned = publishPartition.clone();
            for (String column : cloned.getColumns()) {
                int columnIndex = event.getSchema().getColumnIndex(column);
                if (columnIndex < 0) {
                    continue;
                }
                cloned.getColumnValues().add(event.getData()[columnIndex]);
            }

            collector.add(new Tuple2(cloned, event));
        }
    }

    public List<Tuple2<PublishPartition, AlertStreamEvent>> emitResult() {
        if (collector.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedList<Tuple2<PublishPartition, AlertStreamEvent>> result = new LinkedList<>();
        result.addAll(collector);
        return result;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {

    }


    public void onAlertBoltSpecChange(Collection<PublishPartition> addedPublishPartitions,
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
