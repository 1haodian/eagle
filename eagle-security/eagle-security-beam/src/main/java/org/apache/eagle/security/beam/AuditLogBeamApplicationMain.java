package org.apache.eagle.security.beam;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.beam.runners.spark.SparkPipelineResult;
import org.apache.beam.sdk.Pipeline;
import org.apache.eagle.app.environment.impl.BeamEnviroment;

import java.util.HashMap;
import java.util.Map;

public class AuditLogBeamApplicationMain {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("appId", args[0]);
        map.put("dataSourceConfig.topic", args[1]);
        map.put("dataSourceConfig.ZkConnection", args[2]);
        map.put("dataSinkConfig.topic", args[3]);
        map.put("dataSinkConfig.brokerList", args[4]);
        map.put("autoOffsetResetConfig", args[5]);
        map.put("sparkRunner.checkpoint", args[6]);
        map.put("sparkRunner.master", args[7]);
        map.put("topology.batchInterval", args[8]);

        Config config = ConfigFactory.parseMap(map);

        Pipeline pipeline = new AuditLogBeamApplication().execute(config, new BeamEnviroment(config));
        SparkPipelineResult res = (SparkPipelineResult) pipeline.run();
        res.waitUntilFinish();
    }
}
