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
 * Copied from BasicScalarModelTest
 */
public class LightScalarModelTest extends BaseTestCase {
  private static final String VALUE1 = "haba";
  private LightScalarModel<String> myModel;


  public void testBasicContracts() {
    //null value failure
    myModel = LightScalarModel.create();
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(true, myModel.isContentKnown());
    assertNull(myModel.getValue());
    // invariant properties
    myModel.setValue("x");
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(true, myModel.isContentKnown());
    myModel.setValue("y");
    assertEquals(true, myModel.isContentChangeable());
    assertEquals(true, myModel.isContentKnown());

    myModel = LightScalarModel.create("xxx");
    myModel.setValue("y");
    myModel.setValue("z");

    // modification
    myModel = LightScalarModel.create("");
    assertEquals("", myModel.getValue());
    myModel.setValue("x");
    assertEquals("x", myModel.getValue());
  }

  public void testKnownConstant() {
    myModel = LightScalarModel.create(LightScalarModelTest.VALUE1);
    final boolean[] notified = {false};
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        assertTrue(event.getSource() == myModel);
        assertTrue(event.getNewValue().equals(LightScalarModelTest.VALUE1));
        notified[0] = true;
      }
    });
    assertTrue(notified[0]);
  }

  public void testAnotherThreadReentrancy() throws InterruptedException {
    final int COUNT = 10;
    myModel = LightScalarModel.create();
    myModel.getEventSource().addListener(Lifespan.FOREVER, ThreadGate.NEW_THREAD, new ScalarModel.Adapter<String>() {
      public void onScalarChanged(ScalarModelEvent<String> event) {
        String value = event.getNewValue() + "x";
        if (value.length() > COUNT)
          return;
        //System.out.println(value);
        myModel.setValue(value);
      }
    });
    myModel.setValue(LightScalarModelTest.VALUE1);
    String v = LightScalarModelTest.VALUE1;
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
    myModel = LightScalarModel.create();
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
    for (Thread thread : threads)
      thread.start();
    final CollectionsCompare compare = new CollectionsCompare();
    assertAsync(2000, null, new Asserter() {
      public void assertAttempt() throws AssertionFailedError {
        synchronized (myGotStrings) {
          compare.unordered(myPutStrings, myGotStrings);
        }
      }
    });
  }

  public void testWaitValue() throws InterruptedException {
    myModel = LightScalarModel.create();
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
}
