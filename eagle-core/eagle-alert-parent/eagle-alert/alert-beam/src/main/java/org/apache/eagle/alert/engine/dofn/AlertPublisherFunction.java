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