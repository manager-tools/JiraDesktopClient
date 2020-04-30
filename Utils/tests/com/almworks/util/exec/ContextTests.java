package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;

import java.awt.*;

public class ContextTests extends BaseTestCase {
  public ContextTests() {
    super(HEADLESS_AWT_IMMEDIATE_GATE);
  }

  protected void setUp() throws Exception {
    super.setUp();
    Context.clear();
  }

  protected void tearDown() throws Exception {
    Context.pop();
    super.tearDown();
  }

  public void test() {
    assertNull(Context.get(String.class));
    assertEquals("A", Context.get(String.class, "A"));
    Context.add(InstanceProvider.instance("hello!"), "x");
    try {
      assertEquals("hello!", Context.require(String.class));
    } finally {
      Context.pop();
    }
    assertNull(Context.get(String.class));
    assertEquals("A", Context.get(String.class, "A"));
  }

  ThreadLocal<Boolean> X = new ThreadLocal<Boolean>() {
    protected Boolean initialValue() {
      return EventQueue.isDispatchThread();
    }
  };

  public void perfIsDispatchThreadIsSlowerThanThreadLocal() {
    int COUNT = 50000000;

    long start = System.currentTimeMillis();
    int a = 0;
    for (int i = 0; i < COUNT; i++) {
      a += X.get() ? 1 : 2;
    }
    long threadLocal = System.currentTimeMillis() - start;
    System.out.println("threadLocal = " + threadLocal);

    start = System.currentTimeMillis();
    a = 0;
    for (int i = 0; i < COUNT; i++) {
      a += EventQueue.isDispatchThread() ? 1 : 2;
    }
    long isDispatchThread = System.currentTimeMillis() - start;
    System.out.println("isDispatchThread = " + threadLocal);
  }

  public void perfIsDispatchThreadIsSlowerThanThreadLocalNonAWT() throws InterruptedException {
    Thread thread = new Thread(new Runnable() {
      public void run() {
        perfIsDispatchThreadIsSlowerThanThreadLocal();
      }
    });
    thread.start();
    thread.join();
  }
}
