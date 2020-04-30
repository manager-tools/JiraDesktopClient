package com.almworks.util.events;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Lifespan;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ChainedFireEventSupportTest extends BaseTestCase {
  private FireEventSupport<Listener> myParent;
  private FireEventSupport<Listener> myChild;
  private int myCounter;

  protected void setUp() throws Exception {
    myParent = FireEventSupport.createSynchronized(Listener.class);
    myChild = FireEventSupport.createSynchronized(Listener.class);
    myParent.addChainedSource(myChild);
    myCounter = 0;
  }

  protected void tearDown() throws Exception {
    myParent = null;
    myChild = null;
  }

  private void test(int expectedCount) {
    assertEquals(expectedCount, myCounter);
    myCounter = 0;
  }

  public void testChainedFire() {
    Inc l1 = new Inc(1);
    Inc l100 = new Inc(100);
    myChild.addStraightListener(Lifespan.FOREVER, l1);
    test(0);
    myParent.getDispatcher().mega("x");
    test(1);
    myParent.addStraightListener(Lifespan.FOREVER, l100);
    test(0);
    myParent.getDispatcher().mega("x");
    test(101);
    myChild.removeListener(l1);
    myParent.getDispatcher().mega("x");
    test(100);
  }

  public void testChainedShapshots() {
    myChild.addStraightListener(Lifespan.FOREVER, new Inc(1));
    myParent.addStraightListener(Lifespan.FOREVER, new Inc(100));
    Listener dispatcher = myParent.getDispatcherSnapshot();
    myChild.addStraightListener(Lifespan.FOREVER, new Inc(999));
    myParent.addStraightListener(Lifespan.FOREVER, new Inc(888));
    dispatcher.mega("x");
    test(101);
  }

  public void testChainRemoval() {
    myChild.addStraightListener(Lifespan.FOREVER, new Inc(1));
    myParent.addStraightListener(Lifespan.FOREVER, new Inc(100));
    myParent.removeChainedSource(myChild);
    myParent.getDispatcher().mega("x");
    test(100);
  }

  private class Inc implements Listener {
    private final int myMultiplier;

    public Inc(int multiplier) {
      myMultiplier = multiplier;
    }

    public void mega(String mega) {
      myCounter += mega.length() * myMultiplier;
    }
  }
}
