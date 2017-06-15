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

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.Map;

public class AlertBoltFunction extends
        PTransform<PCollection<Iterable<PartitionedEvent>>, PCollection<KV<String, AlertStreamEvent>>> {

    private PCollectionView<AlertBoltSpec> alertBoltSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private boolean returnMergedResult = Boolean.TRUE;

    public AlertBoltFunction(PCollectionView<AlertBoltSpec> alertBoltSpecView,
                             PCollectionView<Map<String, StreamDefinition>> sdsView,
                             boolean returnMergedResult) {
        this.alertBoltSpecView = alertBoltSpecView;
        this.sdsView = sdsView;
        this.returnMergedResult = returnMergedResult;
    }

    public AlertBoltFunction(PCollectionView<AlertBoltSpec> alertBoltSpecView,
                             PCollectionView<Map<String, StreamDefinition>> sdsView
    ) {
        this(alertBoltSpecView, sdsView, true);
    }

    @Override
    public PCollection<KV<String, AlertStreamEvent>> expand(
            PCollection<Iterable<PartitionedEvent>> input) {
        return input.apply(ParDo.of(new SiddhiFn(alertBoltSpecView, sdsView, returnMergedResult))
                .withSideInputs(alertBoltSpecView, sdsView));
    }
}
