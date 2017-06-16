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

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Instant;

import java.util.List;

public class AssignPartitionFn extends DoFn<PartitionedEvent, KV<Integer, PartitionedEvent>> {
    private PCollectionView<List<StreamPartition>> spView;

    public AssignPartitionFn(PCollectionView<List<StreamPartition>> spView) {
        this.spView = spView;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        PartitionedEvent pevent = c.element();
        List<StreamPartition> sps = c.sideInput(spView);
        for (int i = 0; i < sps.size(); i++) {
            if (sps.get(i).equals(pevent.getPartition())) {
                //c.output(KV.of(i, pevent));
                c.outputWithTimestamp(KV.of(i, pevent),new Instant(pevent.getTimestamp()));
                break;
            }
        }
    }
}
