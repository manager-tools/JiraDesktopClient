package com.almworks.api.application;

public class ReplaceTabKey implements TabKey {
  private final String myDebugName;

  public ReplaceTabKey(String debugName) {
    myDebugName = debugName;
  }

  public boolean isReplaceTab(TabKey tabKey) {
    return true;
  }

  public String toString() {
    return "QueryContextKey[" + myDebugName + "]";
  }
}
