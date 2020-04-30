package com.almworks.util.model;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import junit.framework.AssertionFailedError;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;

import java.util.Collections;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class BasicScalarModelTest extends BaseTestCase {
  private static final String VALUE1 = "haba";
  private BasicScalarModel<String> myModel;


  public void testBasicContracts() {
    //immutable
    myModel = BasicScalarModel.createConstant("x");
    try {
      myModel.setValue("y");
      fail("successfully set read-only value");
    } catch (ValueAlreadySetException e) {
      // normal
    }
    //unknown value failure
    myModel = BasicScalarModel.create(true, true);
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(false, myModel.isContentKnown());
    try {
      myModel.getValue();
      fail("successfully got unknown value");
    } catch (NoValueException e) {
      // normal
    }
    // invariant properties
    myModel.setValue("x");
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(true, myModel.isContentKnown());
    myModel.setValue("y");
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(true, myModel.isContentKnown());

    // WORM
    myModel = BasicScalarModel.create(false, false);
    String value = myModel.getValue();
    assertEquals(null, value);
    myModel.setValue("x");
    value = myModel.getValue();
    assertEquals("x", value);
    try {
      myModel.setValue("y");
      fail("successfully set read-only value");
    } catch (ValueAlreadySetException e) {
      // normal
    }

    myModel = BasicScalarModel.createWithValue("xxx", true);
    myModel.setValue("y");
    myModel.setValue("z");

    // modification
    myModel = BasicScalarModel.create(false, "");
    assertEquals("", myModel.getValue());
    myModel.setValue("x");
    assertEquals("x", myModel.getValue());
  }

  public void testKnownConstant() {
    myModel = BasicScalarModel.createConstant(VALUE1);
    final boolean[] notified = {false};
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        assertTrue(event.getSource() == myModel);
        assertTrue(event.getNewValue().equals(VALUE1));
        notified[0] = true;
      }
    });
    assertTrue(notified[0]);
  }

  public void testReentrancy() {
    myModel = BasicScalarModel.create(true, false);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        try {
          String value = event.getNewValue() + "x";
          myModel.setValue(value);
          fail("successfully reentrantly set value");
        } catch (IllegalStateException e) {
          // normal
        }
      }
    });
    myModel.setValue(VALUE1);
    assertEquals(VALUE1, myModel.getValue());
  }

  public void testReentrancyFromAddListener() {
    myModel = BasicScalarModel.createWithValue("X", true);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        try {
          String value = event.getNewValue() + "x";
          myModel.setValue(value);
          fail("successfully reentrantly set value");
        } catch (IllegalStateException e) {
          // normal
        }
      }
    });
  }

  public void testAnotherThreadReentrancy() throws InterruptedException {
    final int COUNT = 10;
    myModel = BasicScalarModel.create(true, false);
    myModel.getEventSource().addListener(Lifespan.FOREVER, ThreadGate.NEW_THREAD, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        String value = event.getNewValue() + "x";
        if (value.length() > COUNT)
          return;
        //System.out.println(value);
        myModel.setValue(value);
      }
    });
    myModel.setValue(VALUE1);
    String v = VALUE1;
    while (v.length() < COUNT)
      v += "x";
    final String sample = v;
    assertAsync(1000, null, new Asserter() {
      public void assertAttempt() throws AssertionFailedError {
        assertEquals(sample, myModel.getValue());
      }
    });
  }

  public void testMassiveConcurrency() throws InterruptedException {
    myModel = BasicScalarModel.create(true, false);
    final List<String> myPutStrings = Collections15.arrayList();
    final List<String> myGotStrings = Collections15.arrayList();
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        synchronized (myGotStrings) {
          myGotStrings.add(event.getNewValue());
        }
      }

      public void onContentKnown(ScalarModelEvent<String> event) {
      }
    });
    List<Thread> threads = Collections15.arrayList(25);
    for (int i = 0; i < 25; i++) {
      final String prevValue = i > 0 ? "x" + (i - 1) : null;
      final String value = "x" + i;
      final int count = i;
      myPutStrings.add(value);
      threads.add(new Thread(value) {
        public void run() {
          try {
            if (prevValue != null) {
              for (int i = 0; i < 20 + count; i++) {
                String value = myModel.getValue();
                if (prevValue.equals(value))
                  break;
                Thread.sleep(10);
                Thread.yield();
              }
            }
            myModel.setValue(value);
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          }
        }
      });
    }
    Collections.shuffle(threads);
    for (Thread thread : threads) {
      thread.start();
    }
    final CollectionsCompare compare = new CollectionsCompare();
//    Thread.sleep(1000);
    assertAsync(2000, null, new Asserter() {
      public void assertAttempt() throws AssertionFailedError {
        synchronized (myGotStrings) {
          compare.unordered(myPutStrings, myGotStrings);
        }
      }
    });
  }

  public void testWaitValue() throws InterruptedException {
    myModel = BasicScalarModel.create(true);
    new Thread() {
      public void run() {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
        myModel.setValue("x");
      }
    }.start();
    assertEquals("x", myModel.getValueBlocking());
  }

  public void testKnownEvent() {
    myModel = BasicScalarModel.createConstant(VALUE1);
    final boolean[] notified = {false};
    final ScalarModel.Adapter<String> listener = new ScalarModel.Adapter<String>() {
      public void onContentKnown(ScalarModelEvent<String> event) {
        assertEquals(myModel, event.getSource());
        assertFalse(Thread.holdsLock(myModel.getLock()));
        notified[0] = true;
      }
    };

    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertTrue(notified[0]);

    notified[0] = false;
    myModel = BasicScalarModel.create();
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    myModel.setValue(VALUE1);
    assertTrue(notified[0]);

    notified[0] = false;
    myModel = BasicScalarModel.create();
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    myModel.setContentKnown();
    assertTrue(notified[0]);
  }

  public void testAllowedReentrantAddListeners() {
    myModel = BasicScalarModel.createConstant("X");
    final String[] value = {null};
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
          public void onScalarChanged(ScalarModelEvent<String> event) {
            value[0] = event.getNewValue();
          }
        });
      }
    });

    assertEquals("X", value[0]);
  }
}
