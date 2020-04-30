package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;
import util.concurrent.SynchronizedBoolean;

public class ContextRegression2Tests extends BaseTestCase {
  protected void setUp() throws Exception {
    super.setUp();
    LongEventQueue.installStatic(new LongEventQueueImpl());
  }

  protected void tearDown() throws Exception {
    LongEventQueue.installStatic(null);
    super.tearDown();
  }

  public void testStackOverloading() throws InterruptedException {
    Context.add(new InstanceProvider<ContextRegression2Tests>(this, null), "test");
//    Context.globalize();
    // exec numerous tasks consequently through the same keyed gate
    Object key = "key";
    final ThreadGate gate = ThreadGate.LONG(key);
    final int[] depth = {0};
    final int MAXDEPTH = 1000;
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    final Runnable[] r = {null};
    r[0] = new Runnable() {
      public void run() {
        try {
          if (depth[0] >= MAXDEPTH) {
            done.set(true);
            return;
          }
          ++depth[0];
//        System.out.println(": " + depth[0]);
          ContextRegression2Tests t = Context.require(ContextRegression2Tests.class);
          assertSame(":" + depth[0], ContextRegression2Tests.this, t);
          ThreadGate.AWT.execute(new Runnable() {
            public void run() {
              gate.execute(r[0]);
            }
          });
        } catch (Throwable e) {
          e.printStackTrace();
          done.set(true);
        }
      }
    };
    gate.execute(r[0]);
    done.waitForValue(true);
//    System.out.println("done");
    LongEventQueue.instance().shutdownGracefully();
  }

  public void testStackOverloading2() throws InterruptedException {
    Context.add(new InstanceProvider<ContextRegression2Tests>(this, null), "test");
//    Context.globalize();
    // exec numerous tasks consequently through the same keyed gate
    Object key = "key";
    final ThreadGate gate = ThreadGate.LONG_QUEUED(key);
    final int[] depth = {0};
    final int MAXDEPTH = 1000;
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    final Runnable[] r = {null};
    r[0] = new Runnable() {
      public void run() {
        try {
          if (depth[0] >= MAXDEPTH) {
            done.set(true);
            return;
          }
          ++depth[0];
//        System.out.println(": " + depth[0]);
          ContextRegression2Tests t = Context.require(ContextRegression2Tests.class);
          assertSame(":" + depth[0], ContextRegression2Tests.this, t);
          gate.execute(r[0]);
        } catch (Throwable e) {
          e.printStackTrace();
          done.set(true);
        }
      }
    };
    gate.execute(r[0]);
    done.waitForValue(true);
//    System.out.println("done");
    LongEventQueue.instance().shutdownGracefully();
  }
}