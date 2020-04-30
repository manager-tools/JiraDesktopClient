package com.almworks.util.debug;

public class DebugDichotomy extends RuntimeDebug {
  protected final int myOutputCount;
  private final String myAName;
  private final String myBName;

  private volatile int myA = 0;
  private volatile int myB = 0;

  public DebugDichotomy(String aName, String bName, int outputCount) {
    myOutputCount = outputCount;
    myAName = aName;
    myBName = bName;
  }

  public final void a() {
    myA = myA + 1;
    output();
  }

  public final void b() {
    myB = myB + 1;
    output();
  }

  protected final void output() {
    output(myAName + "=" + myA + "; " + myBName + "=" + myB);
  }

  public int getA() {
    return myA;
  }

  public int getB() {
    return myB;
  }

  public DebugDichotomy dump(final long period) {
    return (DebugDichotomy) super.dump(period);
  }

  protected boolean hasStats() {
    return myA > 0 || myB > 0;
  }

  public void clear() {
    myA = 0;
    myB = 0;
  }


  public String toString() {
    return myAName + ":" + myBName;
  }
}
