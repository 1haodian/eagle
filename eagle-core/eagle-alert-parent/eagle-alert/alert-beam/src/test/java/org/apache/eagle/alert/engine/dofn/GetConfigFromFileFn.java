package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.factory.SpecFactory;

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

    public GetConfigFromFileFn(TupleTag<SpoutSpec> spoutSpecTupleTag, TupleTag<Map<String, StreamDefinition>> sdsTag,
                               TupleTag<List<StreamPartition>> spTag, TupleTag<RouterSpec> routerSpecTupleTag,
                               TupleTag<PublishSpec> publishSpecTupleTag, TupleTag<Map<StreamPartition, StreamSortSpec>> sssTag,
                               TupleTag<Map<StreamPartition, List<StreamRouterSpec>>> srsTag
    ) {
        this.spoutSpecTupleTag = spoutSpecTupleTag;
        this.sdsTag = sdsTag;
        this.spTag = spTag;
        this.routerSpecTupleTag = routerSpecTupleTag;
        this.publishSpecTupleTag = publishSpecTupleTag;
        this.sssTag = sssTag;
        this.srsTag = srsTag;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        c.output(spoutSpecTupleTag, SpecFactory.createSpoutSpec());
        c.output(sdsTag, SpecFactory.createSds());
        List<StreamPartition> sps = Lists.newArrayList(SpecFactory.createRouterSpec().makeSSS().keySet());
        c.output(spTag, sps);
        RouterSpec routerSpec = SpecFactory.createRouterSpec();
        c.output(routerSpecTupleTag, routerSpec);
        c.output(sssTag, routerSpec.makeSSS());
        c.output(srsTag, routerSpec.makeSRS());
        c.output(publishSpecTupleTag, SpecFactory.createPublishSpec());

    }
}
