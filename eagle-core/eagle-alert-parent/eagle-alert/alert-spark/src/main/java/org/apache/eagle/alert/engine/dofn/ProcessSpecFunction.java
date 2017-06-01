package org.apache.eagle.alert.engine.dofn;

import com.typesafe.config.Config;
import org.apache.beam.sdk.io.kafka.KafkaRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.apache.eagle.alert.coordination.model.AlertBoltSpec;
import org.apache.eagle.alert.coordination.model.SpoutSpec;
import org.apache.eagle.alert.engine.spark.manager.SpecManager;

public class ProcessSpecFunction
    extends DoFn<KafkaRecord<String, String>, KafkaRecord<String, String>> {

  private final TupleTag<KafkaRecord<String, String>> kafkaRecordTupleTag;
  private final TupleTag<SpoutSpec> spoutSpecTupleTag;

  private final TupleTag<AlertBoltSpec> alertBoltSpecTupleTag;
  private int numOfAlertBolts;
  private Config config;

  public ProcessSpecFunction(Config config, int numOfAlertBolts,
      TupleTag<KafkaRecord<String, String>> kafkaRecordTupleTag,
      TupleTag<SpoutSpec> spoutSpecTupleTag, TupleTag<AlertBoltSpec> alertBoltSpecTupleTag) {
    this.numOfAlertBolts = numOfAlertBolts;
    this.config = config;
    this.kafkaRecordTupleTag = kafkaRecordTupleTag;
    this.spoutSpecTupleTag = spoutSpecTupleTag;
    this.alertBoltSpecTupleTag = alertBoltSpecTupleTag;
  }

  @ProcessElement public void processElement(ProcessContext c) {

    SpecManager specManager = new SpecManager(config, numOfAlertBolts);
    SpoutSpec spoutSpec = specManager.generateSpout();
    c.output(spoutSpecTupleTag, null);
    AlertBoltSpec alertBoltSpec = specManager.generateAlertBoltSpec();
    c.output(alertBoltSpecTupleTag, null);
    c.output(kafkaRecordTupleTag, c.element());

  }
}
/*
  PCollection<String> words = ...;
// Select words whose length is below a cut off,
// plus the lengths of words that are above the cut off.
// Also select words starting with "MARKER".
final int wordLengthCutOff = 10;
// Create tags to use for the main and additional outputs.
final TupleTag<String> wordsBelowCutOffTag =
    new TupleTag<String>(){};
final TupleTag<Integer> wordLengthsAboveCutOffTag =
    new TupleTag<Integer>(){};
final TupleTag<String> markedWordsTag =
    new TupleTag<String>(){};
    PCollectionTuple results =
    words.apply(
    ParDo
    .of(new DoFn<String, String>() {
// Create a tag for the unconsumed output.
final TupleTag<String> specialWordsTag =
    new TupleTag<String>(){};
    { @}ProcessElement
public void processElement(ProcessContext c) {
    String word = c.element();
    if (word.length() <= wordLengthCutOff) {
    // Emit this short word to the main output.
    c.output(word);
    } else {
    // Emit this long word's length to a specified output.
    c.output(wordLengthsAboveCutOffTag, word.length());
    }
    if (word.startsWith("MARKER")) {
    // Emit this word to a different specified output.
    c.output(markedWordsTag, word);
    }
    if (word.startsWith("SPECIAL")) {
    // Emit this word to the unconsumed output.
    c.output(specialWordsTag, word);
    }
    }})
    // Specify the main and consumed output tags of the
    // PCollectionTuple result:
    .withOutputTags(wordsBelowCutOffTag,
    TupleTagList.of(wordLengthsAboveCutOffTag)
    .and(markedWordsTag)));
    // Extract the PCollection results, by tag.
    PCollection<String> wordsBelowCutOff =
    results.get(wordsBelowCutOffTag);
    PCollection<Integer> wordLengthsAboveCutOff =
    results.get(wordLengthsAboveCutOffTag);
    PCollection<String> markedWords =
    results.get(markedWordsTag);*/

