package com.almworks.util;

/**
 * Special exception to use in cycles with extracted method.
 */
public class ContinueOrBreak extends CheapCheckedException {
  private final boolean myContinue;

  public ContinueOrBreak(boolean aContinue) {
    myContinue = aContinue;
  }

  public boolean isContinue() {
    return myContinue;
  }

  public boolean isBreak() {
    return !myContinue;
  }

  public static void throwBreak() throws ContinueOrBreak {
    throw new ContinueOrBreak(false);
  }
}
