package com.almworks.util.fx;

import com.almworks.util.exec.ThreadGate;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class AwtFxWindow {
  /**
   * Convenient method to create Awt-FX windows in a thread-safe manner. This is done in the following stages:
   * <ol>
   *   <li>Instantiate this class and start UI creation process - any thread</li>
   *   <li>Build FX object - runs in FX thread</li>
   *   <li>Complete Awt UI creation - runs in Awt thread</li>
   *   <li><code>doneStage</code> is invoke with result of AWT stage. This step is done with null argument if failed on any previous stage</li>
   * </ol>
   *
   * @param fxStage called on FX thread, builds FX object
   * @param awtStage called on AWT thread, builds AWT object
   * @param doneStage  Executed in AWT thread. When everything is done (parameter has AWT result), or when any stage has failed (parameter is null).
   * @param <F> type of an object to create in FX thread.
   * @param <A> final result - the Awt object
   */
  public static <F, A> void start(Supplier<F> fxStage, Function<F, A> awtStage, Consumer<A> doneStage) {
    ThreadGate.executeLong(() -> {
      AtomicReference<F> fxResult = new AtomicReference<>();
      AtomicReference<Throwable> exception = new AtomicReference<>();
      ThreadGate.FX_IMMEDIATE.execute(() -> {
        try {
          fxResult.set(fxStage.get());
        } catch (Throwable e) {
          exception.set(e);
          throw e;
        }
      });
      ThreadGate.AWT_OPTIMAL.execute(() -> {
        Throwable throwable = exception.get();
        if (throwable != null) doneStage.accept(null);
        else {
          A awtResult;
          try {
            awtResult = awtStage.apply(fxResult.get());
          } catch (Throwable e) {
            //noinspection finally
            try {
              doneStage.accept(null);
            } finally {
              throw e;
            }
          }
          doneStage.accept(awtResult);
        }
      });
    });
  }
}
