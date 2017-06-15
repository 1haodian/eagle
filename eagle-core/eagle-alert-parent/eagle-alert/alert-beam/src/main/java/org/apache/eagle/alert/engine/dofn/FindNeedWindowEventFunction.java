package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.RouterSpec;
import org.apache.eagle.alert.coordination.model.StreamRouterSpec;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.coordinator.StreamPartition;
import org.apache.eagle.alert.engine.coordinator.StreamSortSpec;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class FindNeedWindowEventFunction
        extends PTransform<PCollection<PartitionedEvent>, PCollectionTuple> {

    private static final Logger LOG = LoggerFactory.getLogger(FindNeedWindowEventFunction.class);
    private PCollectionView<RouterSpec> routerSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView;
    private PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView;
    private TupleTag<PartitionedEvent> needWindow = new TupleTag<PartitionedEvent>("needWindow") {

    };
    private TupleTag<PartitionedEvent> noneedWindow = new TupleTag<PartitionedEvent>("noneedWindow") {

    };

    public FindNeedWindowEventFunction(PCollectionView<RouterSpec> routerSpecView,
                                       PCollectionView<Map<String, StreamDefinition>> sdsView,
                                       PCollectionView<Map<StreamPartition, StreamSortSpec>> sssView,
                                       PCollectionView<Map<StreamPartition, List<StreamRouterSpec>>> srsView) {
        this.routerSpecView = routerSpecView;
        this.sdsView = sdsView;
        this.sssView = sssView;
        this.srsView = srsView;
    }

    @Override
    public PCollectionTuple expand(PCollection<PartitionedEvent> events) {

        return events.apply("division window or no window",
                ParDo.of(new DoFn<PartitionedEvent, PartitionedEvent>() {

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        PartitionedEvent pevent = c.element();
                        Map<StreamPartition, StreamSortSpec> sss = c.sideInput(sssView);
                        if (!needWindowHandler(pevent, sss)) {
                            c.outputWithTimestamp(noneedWindow, pevent, new Instant(pevent.getTimestamp()));
                        } else {
                            c.outputWithTimestamp(needWindow, pevent, new Instant(pevent.getTimestamp()));
                        }
                    }
                }).withOutputTags(noneedWindow, TupleTagList.of(needWindow))
                        .withSideInputs(routerSpecView, sdsView, sssView, srsView));
    }

    private boolean needWindowHandler(PartitionedEvent event,
                                      Map<StreamPartition, StreamSortSpec> sss) {
        if (event.getTimestamp() <= 0) {
            return false;
        }

        StreamSortSpec streamSortSpec = sss.get(event.getPartition());
        if (streamSortSpec == null) {
            if (event.isSortRequired()) {
                LOG.warn("Stream sort handler required has not been loaded so emmit directly: {}", event);
            }
            return false;
        } else {
            return true;
        }
    }

}
