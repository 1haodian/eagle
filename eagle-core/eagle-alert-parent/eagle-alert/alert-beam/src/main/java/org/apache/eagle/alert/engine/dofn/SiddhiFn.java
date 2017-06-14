package org.apache.eagle.alert.engine.dofn;

import com.google.common.collect.Lists;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.engine.coordinator.*;
import org.apache.eagle.alert.engine.evaluator.impl.AlertStreamCallbackBeam;
import org.apache.eagle.alert.engine.evaluator.impl.SiddhiDefinitionAdapter;
import org.apache.eagle.alert.engine.model.AlertStreamEvent;
import org.apache.eagle.alert.engine.model.PartitionedEvent;
import org.apache.eagle.alert.engine.model.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.stream.input.InputHandler;

import java.util.*;

public class SiddhiFn extends DoFn<Iterable<PartitionedEvent>, KV<String, AlertStreamEvent>> {

    private static final Logger LOG = LoggerFactory.getLogger(SiddhiFn.class);

    private PCollectionView<AlertBoltSpec> alertBoltSpecView;
    private PCollectionView<Map<String, StreamDefinition>> sdsView;
    private transient ExecutionPlanRuntime executionRuntime;
    private transient SiddhiManager siddhiManager;
    private transient AlertStreamCallbackBeam streamCallback;
    private PolicyDefinition activedPolicy;
    private StreamPartition sp;
    private List<AlertStreamEvent> results = new ArrayList<>();
    private Set<PublishPartition> publishPartitions;
    private boolean returnMergedResult = Boolean.TRUE;

    public SiddhiFn(PCollectionView<AlertBoltSpec> alertBoltSpecView,
                    PCollectionView<Map<String, StreamDefinition>> sdsView,
                    boolean returnMergedResult) {
        this.alertBoltSpecView = alertBoltSpecView;
        this.sdsView = sdsView;
        this.returnMergedResult = returnMergedResult;

    }

    public SiddhiFn(PCollectionView<AlertBoltSpec> alertBoltSpecView,
                    PCollectionView<Map<String, StreamDefinition>> sdsView
    ) {
        this(alertBoltSpecView, sdsView, true);

    }

    @Setup
    public void prepare() {
        this.siddhiManager = new SiddhiManager();
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws Exception {
        AlertBoltSpec alertBoltSpec = c.sideInput(alertBoltSpecView);
        Map<String, StreamDefinition> sds = c.sideInput(sdsView);

        Collection<List<PolicyDefinition>> policies = alertBoltSpec.getBoltPoliciesMap().values();
        List<PolicyDefinition> allPolicy = new ArrayList<>();
        for (List<PolicyDefinition> policyList : policies) {
            allPolicy.addAll(policyList);
        }

        this.publishPartitions = new HashSet<>();
        alertBoltSpec.getPublishPartitions().forEach(p -> {
            if (allPolicy.stream().filter(o -> o.getName().equals(p.getPolicyId())).count() > 0) {
                publishPartitions.add(p);
            }
        });

        Map<String, PolicyDefinition> policiesMap = new HashMap<>();
        allPolicy.forEach(p -> policiesMap.put(p.getName(), p));
        for (PartitionedEvent partitionedEvent : c.element()) {
            if (activedPolicy == null) {
                PolicyDefinition choosedPolicy = choosePolicy(partitionedEvent, allPolicy);
                if (choosedPolicy == null) {
                    LOG.info("drop in choosedPolicy " + partitionedEvent.getEvent());
                    continue;
                } else {
                    activedPolicy = choosedPolicy;
                    sp = partitionedEvent.getPartition();
                    LOG.info("prepeareExecutionRuntime");
                    prepeareExecutionRuntime(activedPolicy, sds);

                }
            }
            if (isAcceptedByPolicy(partitionedEvent, activedPolicy)) {
                LOG.info("emit to siddhi " + partitionedEvent.getEvent());
                send(partitionedEvent.getEvent());
            } else {
                LOG.info("drop " + partitionedEvent.getEvent());
            }

        }
        if (streamCallback != null) {
            if (!results.isEmpty()) {

                if (returnMergedResult) {
                    results = Lists.newArrayList(results.get(results.size() - 1));
                }
                for (AlertStreamEvent alert : results) {
                    LOG.info("emit final result from siddhi " + alert);
                    List<KV<String, AlertStreamEvent>> rs = emit(alert);
                    for (KV<String, AlertStreamEvent> eachRs : rs) {
                        c.output(eachRs);//return final reduced value
                    }
                }

            }
        }
    }

    public List<KV<String, AlertStreamEvent>> emit(AlertStreamEvent event) {
        Set<PublishPartition> clonedPublishPartitions = new HashSet<>(publishPartitions);
        List<KV<String, AlertStreamEvent>> result = new ArrayList<>();
        for (PublishPartition publishPartition : clonedPublishPartitions) {
            // skip the publish partition which is not belong to this policy and also check streamId
            PublishPartition cloned = publishPartition.clone();
            Optional.ofNullable(event).filter(
                    x -> x != null && x.getSchema() != null && cloned.getPolicyId()
                            .equalsIgnoreCase(x.getPolicyId()) && (
                            cloned.getStreamId().equalsIgnoreCase(x.getSchema().getStreamId()) || cloned
                                    .getStreamId().equalsIgnoreCase(Publishment.STREAM_NAME_DEFAULT)))
                    .ifPresent(x -> {
                        cloned.getColumns().stream().filter(y -> event.getSchema().getColumnIndex(y) >= 0
                                && event.getSchema().getColumnIndex(y) < event.getSchema().getColumns().size())
                                .map(y -> event.getData()[event.getSchema().getColumnIndex(y)])
                                .filter(y -> y != null).forEach(y -> cloned.getColumnValues().add(y));
                        result.add(KV.of(cloned.getPublishId(), event));
                    });
        }
        return result;
    }

    private void prepeareExecutionRuntime(PolicyDefinition policy, Map<String, StreamDefinition> sds)
            throws StreamNotDefinedException {
        String plan = generateExecutionPlan(policy, sds);

        try {
            this.executionRuntime = siddhiManager.createExecutionPlanRuntime(plan);
            LOG.info("Created siddhi runtime {}", executionRuntime.getName());
        } catch (Exception parserException) {
            LOG.error("Failed to create siddhi runtime for policy: {}, siddhi plan: \n\n{}\n",
                    policy.getName(), plan, parserException);
            throw parserException;
        }

        // add output stream callback
        List<String> outputStreams = getOutputStreams(policy);
        if (!outputStreams.isEmpty()) {
            //TODO support mutilple outputstreams
            String outputStream = outputStreams.get(0);
            if (executionRuntime.getStreamDefinitionMap().containsKey(outputStream)) {
                StreamDefinition streamDefinition = SiddhiDefinitionAdapter.convertFromSiddiDefinition(
                        executionRuntime.getStreamDefinitionMap().get(outputStream));
                this.streamCallback = new AlertStreamCallbackBeam(policy, streamDefinition, outputStream,
                        sp, results);
                this.executionRuntime.addCallback(outputStream, streamCallback);
            } else {
                throw new IllegalStateException("Undefined output stream " + outputStream);
            }
            this.executionRuntime.start();
        }

    }

    public void send(StreamEvent event) throws Exception {
        String streamId = event.getStreamId();
        InputHandler inputHandler = executionRuntime.getInputHandler(streamId);
        if (inputHandler != null) {
            inputHandler.send(event.getTimestamp(), event.getData());

            if (LOG.isDebugEnabled()) {
                LOG.debug("sent event to siddhi stream {} ", streamId);
            }
        } else {
            LOG.warn("No input handler found for stream {}", streamId);
        }
    }

    private PolicyDefinition choosePolicy(PartitionedEvent partitionedEvent,
                                          List<PolicyDefinition> allPolicy) {
        for (PolicyDefinition policy : allPolicy) {
            if (isAcceptedByPolicy(partitionedEvent, policy)) {
                return policy;
            }
        }
        return null;
    }

    @Teardown
    public void teardown() {
        if (this.executionRuntime != null) {
            this.executionRuntime.shutdown();
            LOG.info("Shutdown siddhi runtime {}", this.executionRuntime.getName());
        }
        this.siddhiManager.shutdown();
        LOG.info("Shutdown siddhi manager {}", this.siddhiManager);
        if (activedPolicy != null) {
            LOG.info("Closed handler for policy {}", this.activedPolicy.getName());
        }
    }

    private boolean isAcceptedByPolicy(PartitionedEvent event, PolicyDefinition policy) {
        return policy.getPartitionSpec().contains(event.getPartition()) && (
                policy.getInputStreams().contains(event.getEvent().getStreamId()) || policy.getDefinition()
                        .getInputStreams()
                        .contains(event.getEvent().getStreamId()));//TODO partition should consider inputstreams
    }

    protected List<String> getOutputStreams(PolicyDefinition policy) {
        return policy.getOutputStreams().isEmpty()
                ? policy.getDefinition().getOutputStreams() :
                policy.getOutputStreams();
    }

    protected String generateExecutionPlan(PolicyDefinition policyDefinition,
                                           Map<String, StreamDefinition> sds) throws StreamNotDefinedException {
        return SiddhiDefinitionAdapter.buildSiddhiExecutionPlan(policyDefinition, sds);
    }
}
