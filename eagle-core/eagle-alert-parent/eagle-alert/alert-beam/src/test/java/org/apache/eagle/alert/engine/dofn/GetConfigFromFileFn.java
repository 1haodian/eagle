package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.eagle.alert.coordination.model.*;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.SpecFactory;
import org.joda.time.Instant;

import java.util.List;
import java.util.Map;

public class GetConfigFromFileFn extends DoFn<KV<String, String>, SpoutSpec> {

    private final TupleTag<SpoutSpec> spoutSpecTupleTag;
    private final TupleTag<Map<String, StreamDefinition>> sdsTag;
    private final TupleTag<List<StreamPartition>> spTag;
    private final TupleTag<RouterSpec> routerSpecTupleTag;
    private final TupleTag<PublishSpec> publishSpecTupleTag;
    private final TupleTag<Map<StreamPartition, StreamSortSpec>> sssTag;
    private final TupleTag<Map<StreamPartition, List<StreamRouterSpec>>> srsTag;
    private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag;
    private SpoutSpec spoutSpec;
    private List<StreamPartition> sps;
    private Map<String, StreamDefinition> sds;
    private RouterSpec routerSpec;
    private PublishSpec publishSpec;
    private AlertBoltSpec alertBoltSpec;

    public GetConfigFromFileFn(TupleTag<SpoutSpec> spoutSpecTupleTag, TupleTag<Map<String, StreamDefinition>> sdsTag,
                               TupleTag<List<StreamPartition>> spTag, TupleTag<RouterSpec> routerSpecTupleTag,
                               TupleTag<PublishSpec> publishSpecTupleTag, TupleTag<Map<StreamPartition, StreamSortSpec>> sssTag,
                               TupleTag<Map<StreamPartition, List<StreamRouterSpec>>> srsTag, TupleTag<AlertBoltSpec> alertBoltSpecTupleTag
    ) {
        this.spoutSpecTupleTag = spoutSpecTupleTag;
        this.sdsTag = sdsTag;
        this.spTag = spTag;
        this.routerSpecTupleTag = routerSpecTupleTag;
        this.publishSpecTupleTag = publishSpecTupleTag;
        this.sssTag = sssTag;
        this.srsTag = srsTag;
        this.alertBoltSpecTupleTag = alertBoltSpecTupleTag;
    }

    public void prepare() {
        this.spoutSpec = SpecFactory.createSpoutSpec();
        RouterSpec routerSpec = SpecFactory.createRouterSpec();
        this.routerSpec = routerSpec;
        this.sps = Lists.newArrayList(routerSpec.makeSSS().keySet());
        this.sds = SpecFactory.createSds();
        this.publishSpec = SpecFactory.createPublishSpec();
        this.alertBoltSpec = SpecFactory.createAlertSpec();

    }


    @ProcessElement
    public void processElement(ProcessContext c) {
        long currtmillis = System.currentTimeMillis();
        if(spoutSpec == null){
            prepare();
            c.outputWithTimestamp(spoutSpecTupleTag, spoutSpec, new Instant(currtmillis));
            c.outputWithTimestamp(sdsTag, sds, new Instant(currtmillis));
            c.outputWithTimestamp(spTag, sps, new Instant(currtmillis));
            c.outputWithTimestamp(routerSpecTupleTag, routerSpec, new Instant(currtmillis));
            c.outputWithTimestamp(sssTag, routerSpec.makeSSS(), new Instant(currtmillis));
            c.outputWithTimestamp(srsTag, routerSpec.makeSRS(), new Instant(currtmillis));
            c.outputWithTimestamp(publishSpecTupleTag, publishSpec, new Instant(currtmillis));
            c.outputWithTimestamp(alertBoltSpecTupleTag, alertBoltSpec, new Instant(currtmillis));
        }

    }

}
