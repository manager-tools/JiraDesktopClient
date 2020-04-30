package com.almworks.util;

public interface RunnableE<E extends Throwable> {
  public void run() throws E;
}
