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
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.model.PartitionedEvent;

public class FilterPartitionAndWindowFunction extends
        PTransform<PCollection<KV<Integer, PartitionedEvent>>, PCollection<KV<Integer, PartitionedEvent>>> {

    private PCollectionView<StreamPartition> sssView;
    private String windowStr = "";

    public FilterPartitionAndWindowFunction(PCollectionView<StreamPartition> sssView,
                                            String windowStr) {
        this.sssView = sssView;
        this.windowStr = windowStr;
    }

    @Override
    public PCollection<KV<Integer, PartitionedEvent>> expand(
            PCollection<KV<Integer, PartitionedEvent>> input) {
        return input.apply("Filter Partition",
                ParDo.of(new DoFn<KV<Integer, PartitionedEvent>, KV<Integer, PartitionedEvent>>() {

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<Integer, PartitionedEvent> event = c.element();
                        if (event.getValue().getPartition().equals(c.sideInput(sssView))) {
                            c.output(event);
                        }
                    }
                }).withSideInputs(sssView));
        /*.apply("windowing", Window.into(FixedWindows.of(Duration.parse((windowStr)))))*/
    }

}
