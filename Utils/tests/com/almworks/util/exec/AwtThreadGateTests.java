package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;

public class AwtThreadGateTests extends BaseTestCase {
  private static final String TEST = "test";

  private int myContextSavepoint;

  protected void setUp() throws Exception {
    super.setUp();
    myContextSavepoint = Context.savepoint();
    Context.add(InstanceProvider.instance(TEST), "test setup");
    assertFalse(Context.isAWT());
  }

  protected void tearDown() throws Exception {
    Context.restoreSavepoint(myContextSavepoint);
    super.tearDown();
  }

  public void testImmediate() {
    final boolean[] r = {false};
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        assertTrue(Context.isAWT());
        assertEquals(TEST, Context.get(String.class));
        r[0] = true;
      }
    });
    assertTrue(r[0]);
  }

  public void testDoubleImmediate() {
    final boolean[] r = {false};
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
          public void run() {
            assertTrue(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            r[0] = true;
          }
        });
      }
    });
    assertTrue(r[0]);
  }

  public void testQueued() throws InterruptedException {
    final SyncBool bool = new SyncBool();
    ThreadGate.AWT_QUEUED.execute(true, new Runnable() {
      public void run() {
        assertTrue(Context.isAWT());
        assertEquals(TEST, Context.get(String.class));
        bool.waitAndSet();
      }
    });
    assertFalse(bool.value());
    bool.go();
    assertTrue(bool.value());
  }

  public void testDoubleQueued() throws InterruptedException {
    final SynchronizedBoolean done = new SynchronizedBoolean(false);
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final boolean[] r = {false};
        ThreadGate.AWT_QUEUED.execute(true, new Runnable() {
          public void run() {
            assertTrue(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            r[0] = true;
          }
        });

        // the runnable is waiting
        assertFalse(r[0]);

        // this runnable will be invoked after the runnable above
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            assertTrue(r[0]);
            done.set(true);
          }
        });
      }
    });
    done.waitForValue(true);
  }

  public void testOptimal() throws InterruptedException {
    final SyncBool bool = new SyncBool();
    ThreadGate.AWT_OPTIMAL.execute(true, new Runnable() {
      public void run() {
        assertTrue(Context.isAWT());
        assertEquals(TEST, Context.get(String.class));
        bool.waitAndSet();
      }
    });
    assertFalse(bool.value());
    bool.go();
    assertTrue(bool.value());
  }

  public void testDoubleOptimal() throws InterruptedException {
    ThreadGate.AWT_IMMEDIATE.execute(true, new Runnable() {
      public void run() {
        final boolean[] r = {false};
        ThreadGate.AWT_OPTIMAL.execute(true, new Runnable() {
          public void run() {
            assertTrue(Context.isAWT());
            assertEquals(TEST, Context.get(String.class));
            r[0] = true;
          }
        });

        // the runnable is immediately executed because we are in AWT
        assertTrue(r[0]);
      }
    });
  }
}
