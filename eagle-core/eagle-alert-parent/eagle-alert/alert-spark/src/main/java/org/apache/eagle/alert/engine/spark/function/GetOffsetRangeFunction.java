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

package org.apache.eagle.alert.engine.spark.function;

import com.typesafe.config.Config;
import kafka.message.MessageAndMetadata;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.spark.manager.SpecManager;
import org.apache.eagle.alert.engine.spark.model.*;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.streaming.kafka.HasOffsetRanges;
import org.apache.spark.streaming.kafka.KafkaRDD;
import org.apache.spark.streaming.kafka.OffsetRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Array;
import scala.collection.JavaConversions;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


public class GetOffsetRangeFunction implements Function<OffsetRange[], OffsetRange[]> {

    private static final Logger LOG = LoggerFactory.getLogger(GetOffsetRangeFunction.class);
    private AtomicReference<OffsetRange[]> offsetRangesRef;

    public GetOffsetRangeFunction(AtomicReference<OffsetRange[]> offsetRangesRef) {
        this.offsetRangesRef = offsetRangesRef;
    }

    @Override
    public OffsetRange[] call(OffsetRange[] offsetRangeArray) throws Exception {
        offsetRangesRef.set(offsetRangeArray);
        return offsetRangeArray;
    }

}