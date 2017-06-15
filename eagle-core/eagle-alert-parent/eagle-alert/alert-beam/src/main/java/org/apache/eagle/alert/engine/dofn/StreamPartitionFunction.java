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
import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

import java.util.List;

public class StreamPartitionFunction extends
        PTransform<PCollection<PartitionedEvent>, PCollectionList<KV<Integer, PartitionedEvent>>> {

    private PCollectionView<List<StreamPartition>> spView;
    private int partNum;

    public StreamPartitionFunction(PCollectionView<List<StreamPartition>> spView, int partNum) {
        this.spView = spView;
        this.partNum = partNum;
    }


    @Override
    public PCollectionList<KV<Integer, PartitionedEvent>> expand(
            PCollection<PartitionedEvent> input) {
        return input.apply("add sp to key",
                ParDo.of(new AssignPartitionFn(spView)).withSideInputs(spView)).apply(Partition.of(partNum, new StreamPartitionFn()));/*.apply("Extract stream partition", MapElements.into(
        TypeDescriptors.kvs(TypeDescriptors.strings(), new TypeDescriptor<PartitionedEvent>() {

        })).via((PartitionedEvent pevent) -> KV.of(pevent.getPartition().toString(), pevent)))
        .apply(GroupByKey.create()).apply(Partition.of(partitionNum, new StreamPartitionFn(
            Lists.newArrayList())));*/
    }
}
