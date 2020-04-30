package com.almworks.util.model;

import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ArrayListCollectionModelTest extends BaseTestCase {
  private ArrayListCollectionModel<String> myModel;
  private CollectionsCompare compare = new CollectionsCompare();

  public void testBasicContract() {
    myModel = ArrayListCollectionModel.create();
    assertTrue(myModel.isContentChangeable());
    assertFalse(myModel.isContentKnown());
    compare.empty(myModel.copyCurrent());
    myModel.getWritableCollection().add("x");
    compare.singleElement("x", myModel.copyCurrent());
    assertTrue(myModel.isContentChangeable());
    assertFalse(myModel.isContentKnown());
    myModel.getWritableCollection().add("z");
    try {
      myModel.getWritableCollection().remove("x");
      fail("removed element before collection is known");
    } catch (IllegalStateException e) {
      // normal
    }
    myModel.setContentKnown();
    assertTrue(myModel.isContentChangeable());
    assertTrue(myModel.isContentKnown());

    myModel.getWritableCollection().remove("x");
    compare.singleElement("z", myModel.copyCurrent());

    List<String> list = Arrays.asList(new String[]{"x", "y", "z"});
    myModel = ArrayListCollectionModel.create(list);
    assertFalse(myModel.isContentChangeable());
    assertTrue(myModel.isContentKnown());
    compare.unordered(list, (List) myModel.copyCurrent());

    try {
      myModel.getWritableCollection().add("A");
      fail("successfully added to readonly collection");
    } catch (IllegalStateException e) {
      // normal
    }
    try {
      myModel.getWritableCollection().remove("x");
      fail("successfully removed from readonly collection");
    } catch (IllegalStateException e) {
      // normal
    }
  }

  public void testBlockingGet() throws InterruptedException {
    myModel = ArrayListCollectionModel.create(true, false);
    new Thread("unblocker") {
      public void run() {
        try {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          }
          Collection<String> writableCollection = myModel.getWritableCollection();
          writableCollection.addAll(Arrays.asList(new String[]{"x", "y"}));
          //writableCollection.add("z");
          writableCollection.add("Z");
          //writableCollection.remove("z");
          myModel.setContentKnown();
// this doesn't work:          
//          writableCollection.remove("x");
//          writableCollection.add("YYY");
        } catch (RuntimeException e) {
          e.printStackTrace();
          throw e;
        }
      }
    }.start();
    final List expected = Arrays.asList(new String[]{"x", "y", "Z"});
    Collection<String> result = myModel.getFullCollectionBlocking();
    compare.unordered(expected, result);
  }

  public void testLegacy() throws InterruptedException {
    myModel = ArrayListCollectionModel.create(true, true);
    Collection<String> collection = myModel.getWritableCollection();
    collection.add("one");
    collection.add("two");
    final String[] container = {null, null};
    Tester tester = new Tester(container);
    myModel.getEventSource().addStraightListener(tester);
    collection.add("three");
    tester.test(0, "three");
    collection.remove("one");
    tester.test(1, "one");
    container[1] = "x";
    collection.remove("one");
    Thread.sleep(100);
    tester.test(1, "x");
  }

  public void testContentKnown() {
    final boolean[] notified = {false};
    final CollectionModel.Adapter<String> listener = new CollectionModel.Adapter<String>() {
      public void onContentKnown(CollectionModelEvent<String> event) {
        assertEquals(myModel, event.getSource());
        assertFalse(Thread.holdsLock(myModel.getLock()));
        notified[0] = true;
      }
    };

    myModel = ArrayListCollectionModel.create(true, true);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertTrue(notified[0]);

    notified[0] = false;
    myModel = ArrayListCollectionModel.create(true, false);
    myModel.getEventSource().addStraightListener(Lifespan.FOREVER, listener);
    assertFalse(notified[0]);
    myModel.setContentKnown();
    assertTrue(notified[0]);
  }


  private static class Tester extends CollectionModel.Adapter<String> {
    private final String[] myContainer;
    private static final int ATTEMPTS = 10;
    private static final long PERIOD = 100;

    public Tester(String[] container) {
      myContainer = container;
    }

    public synchronized void onScalarsAdded(CollectionModelEvent<String> event) {
      Object[] scalars = event.getScalars();
      assertTrue("" + scalars.length, scalars.length > 0);
      myContainer[0] = (String) scalars[scalars.length - 1];
    }

    public synchronized void onScalarsRemoved(CollectionModelEvent<String> event) {
      Object[] scalars = event.getScalars();
      assertTrue("" + scalars.length, scalars.length > 0);
      myContainer[1] = (String) scalars[scalars.length - 1];
    }

    public synchronized void test(int index, String sample) throws InterruptedException {
      for (int i = 0; i < ATTEMPTS; i++) {
        if (!Util.equals(myContainer[index], sample))
          wait(PERIOD);
      }
      assertEquals(sample, myContainer[index]);
      myContainer[index] = null;
    }
  }
}
