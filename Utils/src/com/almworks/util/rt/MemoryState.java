package com.almworks.util.rt;

/**
 * @author Vasya
 */
public class MemoryState {
  private final long myUsedMemory;
  private final long myMaxMemory;
  private final long myPeakMemory;

  public MemoryState(long usedMemory, long maxMemory, long peakMemory) {
    this.myUsedMemory = usedMemory;
    this.myMaxMemory = maxMemory;
    this.myPeakMemory = peakMemory;
  }

  public long getUsedMemory() {
    return myUsedMemory;
  }

  public long getMaxMemory() {
    return myMaxMemory;
  }

  public long getPeakMemory() {
    return myPeakMemory;
  }

  public float getUsedRatio() {
    return myMaxMemory == 0 ? 0 : ((float) myUsedMemory) / myMaxMemory;
  }

  public float getPeakRatio() {
    return myMaxMemory == 0 ? 0 : ((float) myPeakMemory) / myMaxMemory;
  }
}
