package com.almworks.util.collections;

/**
 * @author dyoma
 */
public abstract class JointChangeListener implements ChangeListener {
  private final boolean[] myUpdateFlag;

  protected JointChangeListener() {
    this(new boolean[1]);
  }

  protected JointChangeListener(boolean[] updateFlag) {
    myUpdateFlag = updateFlag;
  }

  public boolean[] getUpdateFlag() {
    return myUpdateFlag;
  }

  public final void onChange() {
    if (!startUpdate())
      return;
    try {
      processChange();
    } finally {
      myUpdateFlag[0] = false;
    }
  }

  protected abstract void processChange();

  public boolean startUpdate() {
    if (myUpdateFlag[0])
      return false;
    myUpdateFlag[0] = true;
    return true;
  }

  public void stopUpdate() {
    assert myUpdateFlag[0];
    myUpdateFlag[0] = false;
  }
}
