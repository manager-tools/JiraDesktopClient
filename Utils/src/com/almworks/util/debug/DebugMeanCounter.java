package com.almworks.util.debug;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class DebugMeanCounter extends DebugCounter {
  protected volatile int myMeasurementCount;
  protected static final NumberFormat MEAN_FORMAT = new DecimalFormat("0.000");

  public DebugMeanCounter(String name) {
    super(name);
  }

  public DebugMeanCounter add(long value) {
    myMeasurementCount++;
    return (DebugMeanCounter) super.add(value);
  }

  public DebugMeanCounter increment() {
    myMeasurementCount++;
    return (DebugMeanCounter) super.increment();
  }

  protected void output() {
    long count = myCount;
    int measures = myMeasurementCount;
    if (measures > 0) {
      double mean = ((double)count) / measures;
      output(myName + "=" + count + "; ##=" + measures + "; mean=" + MEAN_FORMAT.format(mean));
    }
  }

  public DebugMeanCounter dump(final long period) {
    return (DebugMeanCounter) super.dump(period);
  }


  public void clear() {
    super.clear();
    myMeasurementCount = 0;
  }
}
