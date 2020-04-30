package com.almworks.util.rt;

/**
 * @author Vasya
 */
public class DefaultMemoryStateGetter implements MemoryStateGetter {
  public MemoryState getMemoryState() {
    return new MemoryState(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
      Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory());
  }
}
