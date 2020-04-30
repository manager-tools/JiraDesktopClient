package com.almworks.util.exec;

class DefaultThreadFactory extends ThreadFactory {
  public Thread createThread(ThreadGroup group, String name, Runnable runnable) {
    ContextFrame topFrame = Context.getTopFrame();
    return new GoodThread(group, runnable, name, topFrame);
  }
}
