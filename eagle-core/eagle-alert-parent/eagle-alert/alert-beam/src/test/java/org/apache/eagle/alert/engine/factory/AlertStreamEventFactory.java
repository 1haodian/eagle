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
