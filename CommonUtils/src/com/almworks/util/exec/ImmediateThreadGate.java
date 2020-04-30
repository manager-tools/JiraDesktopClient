package com.almworks.util.exec;

import com.almworks.util.threads.Computable;
import util.concurrent.Synchronized;

public abstract class ImmediateThreadGate extends ThreadGate {
  protected final Type getType() {
    return Type.IMMEDIATE;
  }

  public <T> T compute(final Computable<T> computable) {
    return compute(true, computable);
  }

  public <T> T compute(boolean transferContext, final Computable<T> computable) {
    final Synchronized<T> result = new Synchronized<T>(null);
    execute(transferContext, new Gateable() {
      public void runGated() {
        result.set(computable.compute());
      }
    });
    return result.get();
  }
}
