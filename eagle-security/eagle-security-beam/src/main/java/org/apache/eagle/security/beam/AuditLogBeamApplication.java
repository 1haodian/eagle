package org.apache.eagle.security.beam;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import org.apache.beam.runners.spark.SparkPipelineOptions;
import org.apache.beam.runners.spark.SparkPipelineResult;
import org.apache.beam.runners.spark.SparkRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.kafka8.Kafka8IO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.eagle.app.BeamApplication;
import org.apache.eagle.app.environment.impl.BeamEnviroment;
import org.apache.eagle.security.beam.parser.HBaseAuditLogKafkaDeseralizer;
import org.apache.eagle.security.beam.parser.HdfsAuditLogKafkaDeserializer;
import org.apache.eagle.security.beam.parser.OozieAuditLogKafkaDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.joda.time.Duration;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @Since 4/20/17
 */
public class AuditLogBeamApplication extends BeamApplication {
    private static Logger LOG = LoggerFactory.getLogger(AuditLogBeamApplication.class);

    private static final String DEFAULT_CONFIG_PREFIX = "dataSourceConfig";
    private String configPrefix = DEFAULT_CONFIG_PREFIX;
    private static String DEFAULT_CONSUMER_GROUP_ID = "eagleConsumers";

    private static final String OOZIE_SYMBOL = "oozie_log";
    private static final String HDFS_SYMBOL = "hdfs_log";
    private static final String HBASE_SYMBOL = "hbase_log";

    private SparkPipelineResult res;
    public SparkPipelineResult getRes() {
        return res;
    }
    public void setRes(SparkPipelineResult res) {
        this.res = res;
    }

    @Override
    public Pipeline execute(Config config, BeamEnviroment environment) {
        Config context = config;
        if (this.configPrefix != null) {
            context = config.getConfig(configPrefix);
        }
        Map<String, String> consumerProps = ImmutableMap.<String, String>of(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.getString("autoOffsetResetConfig"),
            "metadata.broker.list", config.getString("dataSourceConfig.ZkConnection"),
            "group.id", context.hasPath("consumerGroupId") ? context.getString("consumerGroupId") : DEFAULT_CONSUMER_GROUP_ID
        );
        String topic = context.getString("topic");
        String zkConnString = context.getString("ZkConnection");


        Kafka8IO.Read<String, String> read = Kafka8IO.<String, String>read()
            .withBootstrapServers(zkConnString)
            .withTopics(Collections.singletonList(topic))
            .withKeyCoder(StringUtf8Coder.of())
            .withValueCoder(StringUtf8Coder.of())
            .updateKafkaClusterProperties(consumerProps);

        SparkPipelineOptions options = PipelineOptionsFactory.as(SparkPipelineOptions.class);
        Duration batchIntervalDuration = Duration.standardSeconds(config.getInt("topology.batchInterval"));
        options.setBatchIntervalMillis(batchIntervalDuration.getMillis());
        options.setMinReadTimeMillis(batchIntervalDuration.minus(1).getMillis());
        options.setMaxRecordsPerBatch(5000L);
        options.setRunner(SparkRunner.class);
        options.setAppName(config.getString("appId"));
        options.setCheckpointDir(config.getString("sparkRunner.checkpoint"));
        options.setSparkMaster(config.getString("sparkRunner.master"));
        Pipeline p = Pipeline.create(options);

        String sinkBrokerList = config.getString("dataSinkConfig.brokerList");
        String sinkTopic = config.getString("dataSinkConfig.topic");

        PCollection<KV<String, String>> deduped =
            p.apply(read.withoutMetadata()).apply(ParDo.of(new CleanLogFn())).apply(ParDo.of(new ExtractLogFn(config)));
        deduped.apply(Kafka8IO.<String, String>write()
                .withBootstrapServers(sinkBrokerList)
                .withTopic(sinkTopic)
                .withKeyCoder(StringUtf8Coder.of())
                .withValueCoder(StringUtf8Coder.of()).updateProducerProperties(ImmutableMap.of("bootstrap.servers", sinkBrokerList)));
        return p;
    }

    private static class CleanLogFn extends  DoFn<KV<String, String>, KV<String, String>> {
        @ProcessElement
        public void processElement(ProcessContext c) throws UnsupportedEncodingException {
            String log = c.element().getValue();
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(log);
                String emitKey = jsonObject.getString("type");
                String emitValue = jsonObject.getString("message").replaceAll("\n", "");
                c.output(KV.of(emitKey, emitValue));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ExtractLogFn extends DoFn<KV<String, String>, KV<String, String>> {
        private Config config;

        public ExtractLogFn(Config config) {
            this.config = config;
        }

        @ProcessElement
        public void processElement(ProcessContext c) throws UnsupportedEncodingException {
            String logType = c.element().getKey();
            String log = c.element().getValue();
            LOG.info("-------------------- " + logType + "-----------" + log);
            Map<String, String> map;
            if (logType.equals(OOZIE_SYMBOL)) {
                map = (Map<String, String>) new OozieAuditLogKafkaDeserializer().deserialize(log.getBytes("UTF-8"));
                if (map != null) {
                    c.output(KV.of(map.get("user"), map.toString()));
                }
            } else if (logType.equals(HDFS_SYMBOL)) {
                map = (Map<String, String>) new HdfsAuditLogKafkaDeserializer(config).deserialize(log.getBytes("UTF-8"));
                if (map != null) {
                    c.output(KV.of(map.get("user"), map.toString()));
                }
            } else if (logType.equals(HBASE_SYMBOL)) {
                map = (Map<String, String>) new HBaseAuditLogKafkaDeseralizer().deserialize(log.getBytes("UTF-8"));
                if (map != null) {
                    c.output(KV.of(map.get("user"), map.toString()));
                }
            } else {
                LOG.error("Fail to parse log: " + log);
            }
        }
    }

}
