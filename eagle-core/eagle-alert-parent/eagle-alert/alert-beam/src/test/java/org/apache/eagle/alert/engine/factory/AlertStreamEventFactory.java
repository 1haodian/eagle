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

import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;

public class AlertStreamEventFactory {

    public static AlertStreamEvent createAlertEvent() {
        AlertStreamEvent alertStreamEvent = new AlertStreamEvent();
        alertStreamEvent.setStreamId("testAlertStream");
        alertStreamEvent.setSiteId("yhd");
        alertStreamEvent.setTimestamp(13);
        alertStreamEvent.setData(
                new Object[]{"140648764-oozie-oozi-W2017-06-05 04:56:28", 2, "yyy.yyy.yyy.yyy",
                        "start"});
        StreamDefinition schema = SpecFactory.createSds().get("oozieStream");
        alertStreamEvent.setSchema(schema);
        alertStreamEvent.setPolicyId("policy4");
        alertStreamEvent.setCreatedBy(
                "StreamPartition[streamId=oozieStream,type=GROUPBY,columns=[operation],sortSpec=[StreamSortSpec[windowPeriod=PT4S,windowMargin=1000]]]yhd, metaVersion=null");
        return alertStreamEvent;
    }
}
