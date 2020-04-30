package com.almworks.util.exec;

class GoodThread extends Thread {
  private ThreadContext myThreadContext;

  public GoodThread(ThreadGroup group, Runnable runnable, String name, ContextFrame topFrame) {
    super(group, runnable, name);
    myThreadContext = new ThreadContext(topFrame, this);
  }

  public void run() {
    super.run();
  }

  ThreadContext getContextData() {
    return myThreadContext;
  }
}
