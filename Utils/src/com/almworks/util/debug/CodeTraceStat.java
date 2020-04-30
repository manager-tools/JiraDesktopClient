package com.almworks.util.debug;

/**
 * :todoc:
 *
 * @author sereda
 */
public class CodeTraceStat {
  private final String myCodeName;
  private int myRunCount = 0;
  private long myTotalTime = 0;

  public CodeTraceStat(String codeName) {
    myCodeName = codeName;
  }

  public synchronized void update(Trace trace) {
    myRunCount++;
    long dif = trace.getFinishTime() - trace.getStartTime();
    myTotalTime += dif;
  }

  public String getCodeName() {
    return myCodeName;
  }


  public int getRunCount() {
    return myRunCount;
  }

  public String getMeanTime() {
    if (myRunCount == 0)
      return "0";
    long meanTime = myTotalTime / myRunCount;
    return Long.toString(meanTime / 1000000) + "." + Long.toString((meanTime / 1000) % 1000);
  }

  public String getTotalTime() {
    return Long.toString(myTotalTime / 1000000);
  }
}