package org.apache.eagle.alert.engine.dofn;

import org.apache.beam.sdk.coders.VarLongCoder;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFnTester;

import static org.hamcrest.Matchers.hasItems;

import org.apache.beam.sdk.transforms.windowing.GlobalWindow;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.PCollectionViews;
import org.apache.beam.sdk.values.WindowingStrategy;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class MyDoFnTest {

  @Rule public final TestPipeline p = TestPipeline.create().enableAbandonedNodeEnforcement(false);


  @Test public void testMyDoFn() throws Exception {
    PCollection<Long> inputCount = p.apply(Create.of(6L));
    PCollectionView<Long> view = PCollectionViews
        .singletonView(inputCount, WindowingStrategy.globalDefault(), true, 0L, VarLongCoder.of());
    DoFnTester<String, Integer> fnTester = DoFnTester.of(new MyDoFn(view));
    fnTester.setSideInput(view, GlobalWindow.INSTANCE, 16L);
    List<Integer> testOutputs = fnTester.processBundle("input1", "input12", "input123");
    Assert.assertThat(testOutputs, hasItems(6, 7, 8));

  }
}
