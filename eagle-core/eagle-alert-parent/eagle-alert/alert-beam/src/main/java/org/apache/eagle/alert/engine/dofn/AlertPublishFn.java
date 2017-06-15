package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.Config;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.PublishSpec;
import org.apache.eagle.alert.engine.coordinator.PolicyDefinition;
import org.apache.eagle.alert.engine.coordinator.Publishment;
import org.apache.eagle.alert.engine.coordinator.StreamDefinition;
import org.apache.eagle.alert.engine.model.AlertPublishEvent;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.publisher.AlertPublishPlugin;
import org.apache.eagle.alert.engine.publisher.AlertPublisher;
import org.apache.eagle.alert.engine.publisher.AlertStreamFilter;
import org.apache.eagle.alert.engine.publisher.PipeStreamFilter;
import org.apache.eagle.alert.engine.publisher.impl.AlertPublishPluginsFactory;
import org.apache.eagle.alert.engine.publisher.impl.AlertPublisherImpl;
import org.apache.eagle.alert.engine.publisher.template.AlertTemplateEngine;
import org.apache.eagle.alert.engine.publisher.template.AlertTemplateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AlertPublishFn extends DoFn<KV<String, Iterable<AlertStreamEvent>>, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AlertPublishFn.class);
    private Map<String, AlertPublishPlugin> publishPluginMapping = new HashMap<>(1);
    private PCollectionView<AlertBoltSpec> alertBoltSpecView;
    private PCollectionView<PublishSpec> publishSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private Map<String, StreamDefinition> streamDefinitionMap;
    private Config config;
    private transient AlertTemplateEngine alertTemplateEngine;
    private transient AlertPublisher alertPublisher;

    public AlertPublishFn(PCollectionView<PublishSpec> publishSpecView,
                          PCollectionView<AlertBoltSpec> alertBoltSpecView,
                          PCollectionView<Map<String, StreamDefinition>> sdsView, Config config) {
        this.alertBoltSpecView = alertBoltSpecView;
        this.sdsView = sdsView;
        this.publishSpecView = publishSpecView;
        this.config = config;
    }

    @Setup
    public void prepare() {
        this.alertTemplateEngine = AlertTemplateProvider.createAlertTemplateEngine();
        this.alertTemplateEngine.init(config);
        this.alertPublisher = new AlertPublisherImpl("alertPublisherName");
        this.alertPublisher.init(config, new HashMap());
    }

    @Teardown
    public void cleanup() {
        alertPublisher.close();
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws Exception {
        PublishSpec publishSpec = c.sideInput(publishSpecView);
        List<Publishment> newPublishments = publishSpec.getPublishments();

        Map<String, Publishment> newPublishmentsMap = new HashMap<>();
        newPublishments.forEach(p -> newPublishmentsMap.put(p.getName(), p));

        AlertBoltSpec alertBoltSpec = c.sideInput(alertBoltSpecView);
        // List<String> policyToRemove = new ArrayList<>();
        Map<String, PolicyDefinition> policiesMap = new HashMap<>();
        Collection<List<PolicyDefinition>> policies = alertBoltSpec.getBoltPoliciesMap().values();
        List<PolicyDefinition> allPolicy = new ArrayList<>();
        for (List<PolicyDefinition> policyList : policies) {
            allPolicy.addAll(policyList);
        }
        allPolicy.forEach(p -> policiesMap.put(p.getName(), p));

        for (Map.Entry<String, PolicyDefinition> entry : policiesMap.entrySet()) {
            try {
                this.alertTemplateEngine.register(entry.getValue());
            } catch (Throwable throwable) {
                LOG.error("Failed to register policy {} in template engine", entry.getKey(), throwable);
            }
        }

        this.streamDefinitionMap = c.sideInput(sdsView);
        Iterable<AlertStreamEvent> itr = c.element().getValue();
        List<AlertStreamEvent> result = new ArrayList<>();
        AlertStreamFilter alertFilter = new PipeStreamFilter(
                new AlertContextEnrichFilter(policiesMap, streamDefinitionMap),
                new AlertTemplateFilter(alertTemplateEngine));
        itr.forEach(event -> {
            AlertStreamEvent filteredEvent = alertFilter.filter(event);
            if (filteredEvent != null) {
                result.add(filteredEvent);
            }
        });

        for (Publishment publishment : newPublishments) {
            LOG.debug("OnPublishmentChange : add publishment : {} ", publishment);

            AlertPublishPlugin plugin = AlertPublishPluginsFactory
                    .createNotificationPlugin(publishment, config, new HashMap());
            if (plugin != null) {
                publishPluginMapping.put(publishment.getName(), plugin);
            } else {
                LOG.error("OnPublishChange alertPublisher {} failed due to invalid format", publishment);
            }
        }

        for (AlertStreamEvent event : result) {
            notifyAlert(c.element().getKey(), event);
        }

        c.output("success alert publishID " + c.element().getKey() + "result size " + result.size());

    }

    private void notifyAlert(String publishId, AlertStreamEvent event) {
        if (!publishPluginMapping.containsKey(publishId)) {
            LOG.warn("PublishPartition {} is not found in publish plugin map", publishId);
            return;
        }
        AlertPublishPlugin plugin = publishPluginMapping.get(publishId);
        if (plugin == null) {
            LOG.warn("PublishPartition {} has problems while initializing publish plugin", publishId);
            return;
        }
        event.ensureAlertId();
        try {
            LOG.debug("Execute alert publisher {}", plugin.getClass().getCanonicalName());
            plugin.onAlert(event);
        } catch (Exception ex) {
            LOG.error("Fail invoking publisher's onAlert, continue ", ex);
        }
    }

    private class AlertContextEnrichFilter implements AlertStreamFilter {

        private final Map<String, StreamDefinition> sds;
        private final Map<String, PolicyDefinition> policiesMap;

        private AlertContextEnrichFilter(Map<String, PolicyDefinition> policiesMap,
                                         Map<String, StreamDefinition> sds) {
            this.sds = sds;
            this.policiesMap = policiesMap;
        }

        @Override
        public AlertStreamEvent filter(AlertStreamEvent event) {
            event.ensureAlertId();
            Map<String, Object> extraData = new HashMap<>();
            List<String> appIds = new ArrayList<>();
            if (policiesMap == null || sds == null) {
                LOG.warn(
                        "policyDefinitions or streamDefinitions in publisher bolt have not been initialized");
            } else {
                PolicyDefinition policyDefinition = policiesMap.get(event.getPolicyId());
                if (policiesMap != null && policyDefinition != null) {
                    for (String inputStreamId : policyDefinition.getInputStreams()) {
                        StreamDefinition sd = sds.get(inputStreamId);
                        if (sd != null) {
                            extraData.put(AlertPublishEvent.SITE_ID_KEY, sd.getSiteId());
                            appIds.add(sd.getStreamSource());
                        }
                    }
                    extraData.put(AlertPublishEvent.APP_IDS_KEY, appIds);
                    extraData
                            .put(AlertPublishEvent.POLICY_VALUE_KEY, policyDefinition.getDefinition().getValue());
                    event.setSeverity(policyDefinition.getAlertSeverity());
                    event.setCategory(policyDefinition.getAlertCategory());
                }
                event.setContext(extraData);
            }
            return event;
        }
    }

    private class AlertTemplateFilter implements AlertStreamFilter {

        private final AlertTemplateEngine alertTemplateEngine;

        private AlertTemplateFilter(AlertTemplateEngine alertTemplateEngine) {
            this.alertTemplateEngine = alertTemplateEngine;
        }

        @Override
        public AlertStreamEvent filter(AlertStreamEvent event) {
            return this.alertTemplateEngine.filter(event);
        }
    }
}
