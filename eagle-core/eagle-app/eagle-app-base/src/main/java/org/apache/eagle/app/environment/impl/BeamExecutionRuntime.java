package org.apache.eagle.app.environment.impl;

import com.typesafe.config.Config;
import org.apache.beam.runners.spark.SparkPipelineResult;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.eagle.app.Application;
import org.apache.eagle.app.environment.ExecutionRuntime;
import org.apache.eagle.app.environment.ExecutionRuntimeProvider;
import org.apache.eagle.metadata.model.ApplicationEntity;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class BeamExecutionRuntime implements ExecutionRuntime<BeamEnviroment, Pipeline> {

    private static final Logger LOG = LoggerFactory.getLogger(BeamExecutionRuntime.class);

    private BeamEnviroment environment;
    private static final String TOPOLOGY_MAINCLASS = "topology.mainclass";
    private static final String TOPOLOGY_SPARKHOME = "topology.sparkhome";
    private static final String TOPOLOGY_VERBOSE = "topology.verbose";
    private static final String TOPOLOGY_SPARKUIPORT = "topology.sparkuiport";
    private static final String TOPOLOGY_APPRESOURCE = "topology.appresource";
    private static final String TOPOLOGY_YARNQUEUE = "topology.yarnqueue";
    private static final String TOPOLOGY_APPJARS = "topology.appjars";
    private static final String TOPOLOGY_SPARKCONFFILEPATH = "topology.sparkconffilepath";
    private SparkAppHandle appHandle;
    private static final BeamExecutionRuntime INSTANCE = new BeamExecutionRuntime();
    private static final String SPARK_EXECUTOR_CORES = "topology.core";
    private static final String SPARK_EXECUTOR_MEMORY = "topology.memory";
    private static final String TOPOLOGY_MASTER = "topology.master";
    private static final String DRIVER_MEMORY = "topology.driverMemory";
    private static final String DRIVER_CORES = "topology.driverCores";
    private static final String DEPLOY_MODE = "topology.deployMode";
    private static final String TOPOLOGY_NAME = "topology.name";
    private static final String TOPOLOGY_DYNAMICALLOCATION = "topology.dynamicAllocation";

    @Override
    public void prepare(BeamEnviroment environment) {
        this.environment = environment;
    }

    @Override
    public BeamEnviroment environment() {
        return this.environment;
    }

    private SparkLauncher prepareSparkConfig(Config config) {
        String master = config.hasPath(TOPOLOGY_MASTER) ? config.getString(TOPOLOGY_MASTER) : "local[*]";
        String sparkExecutorCores = config.getString(SPARK_EXECUTOR_CORES);
        String sparkExecutorMemory = config.getString(SPARK_EXECUTOR_MEMORY);
        String driverMemory = config.getString(DRIVER_MEMORY);
        String driverCore = config.getString(DRIVER_CORES);
        String deployMode = config.getString(DEPLOY_MODE);
        String enable = config.getString(TOPOLOGY_DYNAMICALLOCATION);
        boolean verbose = config.getBoolean(TOPOLOGY_VERBOSE);
        String mainClass = config.getString(TOPOLOGY_MAINCLASS);
        String sparkHome = config.getString(TOPOLOGY_SPARKHOME);
        String uiport = config.getString(TOPOLOGY_SPARKUIPORT);
        String appResource = config.getString(TOPOLOGY_APPRESOURCE);
        String yarnqueue = config.getString(TOPOLOGY_YARNQUEUE);
        SparkLauncher sparkLauncher = new SparkLauncher();
        sparkLauncher.setMaster(master);
        sparkLauncher.setMainClass(mainClass);
        sparkLauncher.setSparkHome(sparkHome);
        //sparkLauncher.setJavaHome(TOPOLOGY_JAVAHOME);
        sparkLauncher.setDeployMode(deployMode);
        sparkLauncher.setVerbose(verbose);
        sparkLauncher.setAppResource(appResource);
        sparkLauncher.setAppName(config.getString(TOPOLOGY_NAME));
        sparkLauncher.setConf("spark.yarn.queue", yarnqueue);
        sparkLauncher.setConf("spark.executor.cores", sparkExecutorCores);
        sparkLauncher.setConf("spark.executor.memory", sparkExecutorMemory);
        sparkLauncher.setConf("spark.driver.memory", driverMemory);
        sparkLauncher.setConf("spark.driver.cores", driverCore);
        sparkLauncher.setConf("spark.streaming.dynamicAllocation.enable", enable);
        sparkLauncher.setConf("spark.ui.port", uiport);
        String jars = config.getString(TOPOLOGY_APPJARS);
        for (String jar : jars.split(",")) {
            sparkLauncher.addJar(jar);
        }

        String path = config.getString(TOPOLOGY_SPARKCONFFILEPATH);
        if (StringUtil.isNotBlank(path)) {
            sparkLauncher.setPropertiesFile(path);
        }

        String appId = config.getString("appId");
        String dataSourceConfigTopic = config.getString("dataSourceConfig.topic");
        String dataSourceConfigZkConnection = config.getString("dataSourceConfig.ZkConnection");
        String dataSinkConfigTopic = config.getString("dataSinkConfig.topic");
        String dataSinkConfigBrokerList = config.getString("dataSinkConfig.brokerList");
        String autoOffsetResetConfig = config.getString("autoOffsetResetConfig");
        String sparkRunnerCheckpoint = config.getString("sparkRunner.checkpoint");
        String sparkRunnerMaster = config.getString("sparkRunner.master");

        sparkLauncher.addAppArgs(appId, dataSourceConfigTopic, dataSourceConfigZkConnection, dataSinkConfigTopic,
                dataSinkConfigBrokerList, autoOffsetResetConfig, sparkRunnerCheckpoint, sparkRunnerMaster);
        return sparkLauncher;
    }

    @Override
    public void start(Application<BeamEnviroment, Pipeline> executor, Config config) {
        BeamExecutionRuntime.SparkAppListener sparkAppListener = new BeamExecutionRuntime.SparkAppListener();
        try {
            appHandle = prepareSparkConfig(config).startApplication(sparkAppListener);
            LOG.info("Starting Spark Streaming");
            appHandle.addListener(new BeamExecutionRuntime.SparkAppListener());
            Thread sparkAppListenerThread = new Thread(sparkAppListener);
            sparkAppListenerThread.start();
        } catch (IOException e) {
            LOG.error("SparkLauncher().startApplication IOException", e);
        }
    }

    @Override
    public void stop(Application<BeamEnviroment, Pipeline> executor, Config config) {
        try {
            LOG.error("try to call SparkLauncher().stop");
            appHandle.stop();
        } catch (Exception e) {
            LOG.error("SparkLauncher().stop exception", e);
        } finally {
            LOG.error("try to call SparkLauncher().kill");
            appHandle.kill();
        }
    }

    @Override
    public ApplicationEntity.Status status(Application<BeamEnviroment, Pipeline> executor, Config config) {
        if (appHandle == null) {
            return ApplicationEntity.Status.INITIALIZED;
        }
        SparkAppHandle.State state = appHandle.getState();
        LOG.info("Alert engine spark topology  status is " + state.name());
        ApplicationEntity.Status status;
        if (state.isFinal()) {
            LOG.info("Spark Streaming is STOPPED");
            status = ApplicationEntity.Status.REMOVED;
        } else if (state == SparkAppHandle.State.RUNNING) {
            return ApplicationEntity.Status.RUNNING;
        } else if (state == SparkAppHandle.State.CONNECTED || state == SparkAppHandle.State.SUBMITTED) {
            return ApplicationEntity.Status.INITIALIZED;
        } else {
            LOG.error("Alert engine spark topology status unknow");
            status = ApplicationEntity.Status.INITIALIZED;
        }
        return status;
    }

    public static class Provider implements ExecutionRuntimeProvider<BeamEnviroment, Pipeline> {
        @Override
        public ExecutionRuntime<BeamEnviroment, Pipeline> get() {
            return INSTANCE;
        }
    }

    private static class SparkAppListener implements
            SparkAppHandle.Listener, Runnable {
        private static final Logger LOG = LoggerFactory.getLogger(BeamExecutionRuntime.SparkAppListener.class);

        SparkAppListener() {

        }

        @Override
        public void stateChanged(SparkAppHandle handle) {
            String sparkAppId = handle.getAppId();
            SparkAppHandle.State appState = handle.getState();
            if (sparkAppId != null) {
                LOG.info("Spark job with app id: " + sparkAppId + ",State changed to: " + appState);
            } else {
                LOG.info("Spark job's state changed to: " + appState);
            }
            if (appState != null && appState.isFinal()) {
                LOG.info("Spark Streaming is ended");
            }
        }

        @Override
        public void infoChanged(SparkAppHandle handle) {
        }

        @Override
        public void run() {
        }
    }
}
