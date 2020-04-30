package com.almworks.util.debug;

public abstract class RuntimeDebug {
  protected abstract void output();

  protected abstract boolean hasStats();

  public abstract void clear();

  public RuntimeDebug dump(final long period) {
    Thread thread = new Thread("RD." + this) {
      public void run() {
        try {
          while (!Thread.currentThread().isInterrupted()) {
            sleep(period);
            if (hasStats())
              output();
          }
        } catch (InterruptedException e) {
          // exit
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
    return this;
  }

  protected void output(String message) {
    System.out.println("*** RD ***      " + message);
  }
}
