package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.coders.*;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFnTester;

import static org.hamcrest.Matchers.hasItems;

import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.*;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.coder.MyType;
import org.apache.eagle.alert.engine.coder.MyTypeCoder;
import org.apache.eagle.alert.engine.utils.MetadataSerDeser;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.Serializable;
import java.util.List;

public class MyDoFnTest implements Serializable {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);

  @Test public void testMyDoFn() throws Exception {
   /* PCollection<Long> inputCount = p.apply(Create.of(6L));
    PCollectionView<Long> view = PCollectionViews
        .singletonView(inputCount, WindowingStrategy.globalDefault(), true, 0L, VarLongCoder.of());
    DoFnTester<String, Integer> fnTester = DoFnTester.of(new MyDoFn(view));
    fnTester.setSideInput(view, GlobalWindow.INSTANCE, 16L);
    List<Integer> testOutputs = fnTester.processBundle("input1", "input12", "input123");
    Assert.assertThat(testOutputs, hasItems(6, 7, 8));*/
    //GameStats
  }

  @Test public void testGetOffsetAndConfigs() throws Exception {

    SpoutSpec newSpec = MetadataSerDeser
        .deserialize(getClass().getResourceAsStream("/spark/testSpoutSpec.json"), SpoutSpec.class);
   /* p.getCoderRegistry()
        .registerCoderForClass(SpoutSpec.class, SerializableCoder.of(SpoutSpec.class));
    p.getCoderRegistry()
        .registerCoderForClass(MyType.class, MyTypeCoder.of());*/
    MyType myType = new MyType();
    myType.setValue(2L);
    myType.setType("test");
    PCollection<SpoutSpec> inputSpec = p.apply(Create.of(newSpec));
    PCollectionView<SpoutSpec> view = PCollectionViews
        .singletonView(inputSpec, WindowingStrategy.globalDefault(), true, newSpec,
            SerializableCoder.of(SpoutSpec.class));

    DoFnTester<String, Integer> fnTester = DoFnTester.of(new MyDoFn(view));
  //  fnTester.setSideInput(view, GlobalWindow.INSTANCE, myType);
    List<Integer> testOutputs = fnTester.processBundle("input1", "input12", "input123");
  }

}
