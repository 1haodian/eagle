package org.apache.eagle.alert.engine.io;

import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;

/**
 * Print to console.
 */
public final class ConsoleIO {

  private ConsoleIO() {
  }

  /**
   * Write on the console.
   */
  public static final class Write {

    private Write() {
    }

    public static <T> Unbound<T> out() {
      return new Unbound<>(10);
    }

    public static <T> Unbound<T> out(int num) {
      return new Unbound<>(num);
    }

    /**
     * {@link PTransform} writing {@link PCollection} on the console.
     * @param <T> the type of the elements in the {@link PCollection}
     */
    public static class Unbound<T> extends PTransform<PCollection<T>, PDone> {

      private final int num;

      Unbound(int num) {
        this.num = num;
      }

      public int getNum() {
        return num;
      }

      @Override
      public PDone expand(PCollection<T> input) {
        return PDone.in(input.getPipeline());
      }
    }
  }
}