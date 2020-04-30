package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;
import util.concurrent.SynchronizedBoolean;

public class LongThreadGateTests extends BaseTestCase {
  private static final String TEST = "test";

  private int myContextSavepoint;

  protected void setUp() throws Exception {
    super.setUp();
    myContextSavepoint = Context.savepoint();
    Context.add(InstanceProvider.instance(TEST), "");
    assertFalse(Context.isAWT());
    Context.add(InstanceProvider.instance(new LongEventQueueImpl()), "");
  }

  protected void tearDown() throws Exception {
    LongEventQueue.instance().shutdownGracefully();
    Context.restoreSavepoint(myContextSavepoint);
    super.tearDown();
  }

  public void testImmediate() {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final boolean[] r = {false};
        ThreadGate.LONG_IMMEDIATE.execute(true, new Runnable() {
          public void run() {
            assertFalse(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            r[0] = true;
          }
        });
        assertTrue(r[0]);
      }
    });
  }

  public void testDoubleImmediate() {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final boolean[] r = {false};
        ThreadGate.LONG_IMMEDIATE.execute(true, new Runnable() {
          public void run() {
            final Thread thread = Thread.currentThread();
            ThreadGate.LONG_IMMEDIATE.execute(true, new Runnable() {
              public void run() {
                assertEquals(thread, Thread.currentThread());
                assertFalse(Context.isAWT());
                assertEquals(TEST, Context.get(String.class));
                r[0] = true;
              }
            });
          }
        });
        assertTrue(r[0]);
      }
    });
  }

  public void testQueued() {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final SyncBool bool = new SyncBool();
        ThreadGate.LONG_QUEUED.execute(true, new Runnable() {
          public void run() {
            assertFalse(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            bool.waitAndSet();
          }
        });
        assertFalse(bool.value());
        bool.go();
        assertTrue(bool.value());
      }
    });
  }

  public void testDoubleQueued() throws InterruptedException {
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    // if we use just LONG_QUEUED, the test would stall - because the second event will wait for the
    // completion of the first (same key)
    ThreadGate.LONG_QUEUED("a").execute(true, new Runnable() {
      public void run() {
        final SyncBool bool = new SyncBool();
        final Thread thread = Thread.currentThread();
        ThreadGate.LONG_QUEUED("b").execute(true, new Runnable() {
          public void run() {
            assertNotSame(thread, Thread.currentThread());
            assertFalse(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            bool.waitAndSet();
          }
        });
        assertFalse(bool.value());
        bool.go();
        assertTrue(bool.value());
        done.set(true);
      }
    });
    done.waitForValue(true);
  }

  public void testOptimal() {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final SyncBool bool = new SyncBool();
        ThreadGate.LONG_OPTIMAL.execute(true, new Runnable() {
          public void run() {
            assertFalse(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            bool.waitAndSet();
          }
        });
        assertFalse(bool.value());
        bool.go();
        assertTrue(bool.value());
      }
    });
  }

  public void testDoubleOptimal() {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final SyncBool bool = new SyncBool();
        ThreadGate.LONG_OPTIMAL.execute(true, new Runnable() {
          public void run() {
            final Thread thread = Thread.currentThread();
            ThreadGate.LONG_OPTIMAL.execute(true, new Runnable() {
              public void run() {
                assertEquals(thread, Thread.currentThread());
                assertFalse(Context.isAWT());
                assertEquals(TEST, Context.get(String.class));
                bool.waitAndSet();
              }
            });
          }
        });
        assertFalse(bool.value());
        bool.go();
        assertTrue(bool.value());
      }
    });
  }

  public void testOptimalOnDifferentKeys() throws InterruptedException {
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    // do not violate queue contract!
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        ThreadGate.LONG_OPTIMAL(1).execute(true, new Runnable() {
          public void run() {
            final SyncBool bool = new SyncBool();
            final Thread thread = Thread.currentThread();
            ThreadGate.LONG_OPTIMAL(2).execute(true, new Runnable() {
              public void run() {
                assertNotSame(thread, Thread.currentThread());
                assertFalse(Context.isAWT());
                assertEquals(TEST, Context.get(String.class));
                bool.waitAndSet();
              }
            });
            assertFalse(bool.value());
            bool.go();
            assertTrue(bool.value());
            done.set(true);
          }
        });
      }
    });
    done.waitForValue(true);
  }
  public void testImmediateOnDifferentKeys() throws InterruptedException {
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    // do not violate queue contract!
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        ThreadGate.LONG_IMMEDIATE(1).execute(true, new Runnable() {
          public void run() {
            final Thread thread = Thread.currentThread();
            final boolean[] r = {false};
            ThreadGate.LONG_IMMEDIATE(2).execute(true, new Runnable() {
              public void run() {
                assertNotSame(thread, Thread.currentThread());
                assertFalse(Context.isAWT());
                assertEquals(TEST, Context.get(String.class));
                r[0] = true;
              }
            });
            assertTrue(r[0]);
            done.set(true);
          }
        });
      }
    });
    done.waitForValue(true);
  }
}
