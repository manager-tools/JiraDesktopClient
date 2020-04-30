package com.almworks.util.threads;

public class EscalatingBottleneck {
  // postponed
//  private static final int SEQUENCE_LENGTH = 10;
//
//  private final long myMinimum;
//  private final long myStep;
//  private final long myMaximum;
//  private final float myDropFactor;
//  private final ThreadGate myGate;
//  private final Runnable myRunnable;
//  private final Bottleneck myBottleneck;
//
//  private final int[] myExecutionTimeSequence = new int[SEQUENCE_LENGTH];
//  private int myExecutionTimeSequenceIndex = 0;
//  private int myMeanExecutionTime = 0;
//  private DetachComposite myExecutionLifespan;
//  private long myLastRequestTime;
//
//
//  public EscalatingBottleneck(long minimum, long step, long maximum, float dropFactor, ThreadGate gate,
//    final Procedure<Lifespan> runnable)
//  {
//    if (minimum < 10)
//      throw new IllegalArgumentException("minimum " + minimum);
//    if (step < 10)
//      throw new IllegalArgumentException("step " + step);
//    if (dropFactor < 0 || dropFactor > 1)
//      throw new IllegalArgumentException("dropFactor " + dropFactor);
//    myMinimum = minimum;
//    myStep = step;
//    myMaximum = maximum;
//    myDropFactor = dropFactor;
//    myGate = gate;
//    myRunnable = new Runnable() {
//      public void run() {
//        execute(runnable);
//      }
//    };
//    myBottleneck = new Bottleneck(minimum, gate, myRunnable) {
//      protected void runInGate() {
//
//        super.runInGate();
//      }
//    };
//  }
//
//  public void request() {
//    long now = System.currentTimeMillis();
//    long sinceLastRequest = now - myLastRequestTime;
//
//    if (myInFlow) {
//
//    }
//    if (!myInFlow) {
//      myBottleneck.request();
//    }
//    long exeTime = myMeanExecutionTime;
//    if (exeTime > 0) {
//
//    }
//    myLastRequestTime = now;
//  }
//
//  private void execute(Procedure<Lifespan> runnable) {
//    // gated
//    Lifespan life = ...;
//    long start = System.currentTimeMillis();
//    try {
//      runnable.invoke();
//    } catch (Exception e) {
//      Log.error(e);
//    } finally {
//      long end = System.currentTimeMillis();
//      recordExecutionTime(end - start);
//    }
//  }
//
//  private synchronized void recordExecutionTime(int duration) {
//    myExecutionTimeSequence[myExecutionTimeSequenceIndex++] = duration;
//    if (myExecutionTimeSequenceIndex >= myExecutionTimeSequence.length)
//      myExecutionTimeSequenceIndex = 0;
//    long total = 0;
//    int count = 0;
//    for (int d : myExecutionTimeSequence) {
//      if (d < 10)
//        continue;
//      total += d;
//      count++;
//    }
//    if (count < 1)
//      myMeanExecutionTime = 0;
//    else
//      myMeanExecutionTime = (int) total / count;
//  }
}
