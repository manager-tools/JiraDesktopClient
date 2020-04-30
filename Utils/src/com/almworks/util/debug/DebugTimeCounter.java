package com.almworks.util.debug;

public class DebugTimeCounter extends DebugMeanCounter {
  private long myStarted;

  public DebugTimeCounter(String name) {
    super(name);
  }

  public DebugTimeCounter start() {
    myStarted = System.currentTimeMillis();
    return this;
  }

  public DebugTimeCounter stop() {
    if (myStarted > 0) {
      add(System.currentTimeMillis() - myStarted);
      myStarted = 0;
    }
    return this;
  }

  protected void output() {
    long count = myCount;
    int measures = myMeasurementCount;
    if (measures > 0) {
      double mean = ((double)count) / measures;
      output(myName + "=" + count + "ms; ##=" + measures + "; mean=" + MEAN_FORMAT.format(mean) + "ms");
    }
  }


  public DebugTimeCounter dump(final long period) {
    return (DebugTimeCounter) super.dump(period);
  }


  public void clear() {
    super.clear();
    myStarted = 0;
  }
}
