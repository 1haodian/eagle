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
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.Map;

public class CorrelationSpoutFunction1 extends
        PTransform<PCollection<KV<String, String>>, PCollection<KV<Integer, PartitionedEvent>>> {

    private PCollectionView<SpoutSpec> spoutSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private int numOfRouterBolts;

    public CorrelationSpoutFunction1(PCollectionView<SpoutSpec> spoutSpecView,
                                     PCollectionView<Map<String, StreamDefinition>> sdsView, int numOfRouterBolts) {
        this.spoutSpecView = spoutSpecView;
        this.sdsView = sdsView;
        this.numOfRouterBolts = numOfRouterBolts;
    }

    @Override
    public PCollection<KV<Integer, PartitionedEvent>> expand(
            PCollection<KV<String, String>> messages) {
        return messages.apply(ParDo.of(new ConvertToPeventFn(spoutSpecView, sdsView, numOfRouterBolts))
                .withSideInputs(spoutSpecView, sdsView));
    }
}
