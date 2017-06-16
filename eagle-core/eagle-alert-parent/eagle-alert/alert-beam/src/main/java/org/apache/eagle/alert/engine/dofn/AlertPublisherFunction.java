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

import com.typesafe.config.Config;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;

import java.util.Map;

public class AlertPublisherFunction
        extends PTransform<PCollection<KV<String, Iterable<AlertStreamEvent>>>, PCollection<String>> {

    private PCollectionView<AlertBoltSpec> alertBoltSpecView;
    private PCollectionView<PublishSpec> publishSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private Config config;

    public AlertPublisherFunction(PCollectionView<PublishSpec> publishSpecView,
                                  PCollectionView<AlertBoltSpec> alertBoltSpecView,
                                  PCollectionView<Map<String, StreamDefinition>> sdsView, Config config) {
        this.publishSpecView = publishSpecView;
        this.alertBoltSpecView = alertBoltSpecView;
        this.sdsView = sdsView;
        this.config = config;
    }

    @Override
    public PCollection<String> expand(
            PCollection<KV<String, Iterable<AlertStreamEvent>>> input) {
        return input.apply(
                ParDo.of(new AlertPublishFn(publishSpecView, alertBoltSpecView, sdsView, config))
                        .withSideInputs(publishSpecView, alertBoltSpecView, sdsView));
    }
}