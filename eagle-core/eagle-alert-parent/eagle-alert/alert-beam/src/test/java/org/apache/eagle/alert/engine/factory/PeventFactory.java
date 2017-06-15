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

package org.apache.eagle.alert.engine.factory;

import com.google.common.collect.Lists;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;

import java.util.List;

public class PeventFactory {

    public static PartitionedEvent createPevent1() {
        PartitionedEvent pevent1 = new PartitionedEvent();
        StreamPartition streamPartition = new StreamPartition();
        StreamSortSpec streamSortSpec = new StreamSortSpec();
        streamSortSpec.setWindowMargin(1000);
        streamSortSpec.setWindowPeriod("PT4S");
        streamPartition.setStreamId("oozieStream");
        streamPartition.setType(StreamPartition.Type.GROUPBY);
        streamPartition.setColumns(Lists.newArrayList("operation"));
        streamPartition.setSortSpec(streamSortSpec);
        StreamEvent streamEvent1 = new StreamEvent();
        streamEvent1.setStreamId("oozieStream");
        streamEvent1.setTimestamp(1496638588877L);
        streamEvent1.setData(
                new Object[]{"yyy.yyy.yyy.yyy", "140648764-oozie-oozi-W2017-06-05 04:56:28", "start",
                        1496638588877L});
        pevent1.setEvent(streamEvent1);

        pevent1.getEvent().setTimestamp(1);
        pevent1.setPartition(streamPartition);
        return pevent1;
    }

    public static PartitionedEvent createPevent2() {
        PartitionedEvent pevent2 = new PartitionedEvent();
        StreamPartition streamPartition2 = new StreamPartition();
        StreamSortSpec streamSortSpec2 = new StreamSortSpec();
        streamSortSpec2.setWindowMargin(2000);
        streamSortSpec2.setWindowPeriod("PT5S");
        streamPartition2.setStreamId("oozieStream");
        streamPartition2.setType(StreamPartition.Type.GROUPBY);
        streamPartition2.setColumns(Lists.newArrayList("operation"));
        streamPartition2.setSortSpec(streamSortSpec2);
        pevent2.setEvent(new StreamEvent());
        pevent2.getEvent().setTimestamp(13);
        pevent2.setPartition(streamPartition2);
        return pevent2;
    }

    public static List<PartitionedEvent> createPevents() {

        List<PartitionedEvent> events = Lists.newArrayList(createPevent1(), createPevent2());
        return events;
    }
}
