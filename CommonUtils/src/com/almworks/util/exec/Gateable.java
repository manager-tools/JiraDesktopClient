package com.almworks.util.exec;

public abstract class Gateable implements Runnable {
  private ContextDataProvider myTransferredProvider;

  public abstract void runGated();

  public final void run() {
    ContextDataProvider transferredProvider;
    synchronized (this) {
      transferredProvider = myTransferredProvider;
      myTransferredProvider = null;
    }
    if (shouldTransfer(transferredProvider)) {
      Context.add(transferredProvider, "gate");
      try {
        runGated();
      } finally {
        Context.pop();
      }
    } else {
      runGated();
    }
  }

  /**
   * In calling thread
   */
  void enterGate(ContextDataProvider transferredProvider) {
    synchronized (this) {
      if (myTransferredProvider != null) {
        assert false : "second gating " + myTransferredProvider + " " + transferredProvider;
      }
      myTransferredProvider = transferredProvider;
    }
  }

  private boolean shouldTransfer(ContextDataProvider transferredProvider) {
    if (transferredProvider == null)
      return false;
    if (!(transferredProvider instanceof ContextFrameDataProvider))
      return false;
    ContextFrame topFrame = ((ContextFrameDataProvider) transferredProvider).getTopFrame();
    return topFrame != Context.getTopFrame();
  }
}
