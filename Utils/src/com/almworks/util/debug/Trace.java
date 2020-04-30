package com.almworks.util.debug;

/**
 * :todoc:
 *
 * @author sereda
 */
public class Trace {
  private String myCodeName;
  private Object mySyncObject;
  private long myStartTime;
  private long myFinishTime;

  Trace() {
  }

  public String getCodeName() {
    return myCodeName;
  }

  public void setCodeName(String codeName) {
    myCodeName = codeName;
  }

  public Object getSyncObject() {
    return mySyncObject;
  }

  public void setSyncObject(Object syncObject) {
    mySyncObject = syncObject;
  }

  public long getStartTime() {
    return myStartTime;
  }

  public void setStartTime(long startTime) {
    myStartTime = startTime;
  }

  public long getFinishTime() {
    return myFinishTime;
  }

  public void setFinishTime(long finishTime) {
    myFinishTime = finishTime;
  }

  public static Trace trace(String codeName) {
    return Stat.INSTANCE.createTrace(codeName);
  }

  public static void endTrace(Trace trace) {
    Stat.INSTANCE.countTrace(trace);
  }

  public void done() {
    endTrace(this);
  }
}
