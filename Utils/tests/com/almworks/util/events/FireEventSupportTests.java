package com.almworks.util.events;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Lifespan;


/**
 * :todoc:
 *
 * @author sereda
 */
public class FireEventSupportTests extends BaseTestCase {
  public static final String MEGA_ARGUMENT = "mega";

  protected FireEventSupport<Listener> myFireEventSupport;
  protected Listener dispatcher;

  protected void setUp() throws Exception {
    myFireEventSupport = FireEventSupport.createSynchronized(Listener.class);
    dispatcher = myFireEventSupport.getDispatcher();
  }

  protected void tearDown() throws Exception {
    myFireEventSupport = null;
    dispatcher = null;
  }

  public void testDispatchers() {
    final int[] count = {0};
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 1;
      }
    });
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 2;
      }
    });

    Listener dispatcherSnapshot = myFireEventSupport.getDispatcherSnapshot();

    myFireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 3;
      }
    });

    assertTrue(count[0] == 0);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 6);
    dispatcherSnapshot.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 9);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 15);
    dispatcherSnapshot.mega(MEGA_ARGUMENT);
    assertEquals(18, count[0]);
  }

  public void testAddRemove() {
    final int[] count = {0};
    Listener lis1 = new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 1;
      }
    };
    Listener lis2 = new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 2;
      }
    };
    Listener lis3 = new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 3;
      }
    };

    assertTrue(count[0] == 0);
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, lis1);
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, lis2);
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, lis3);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 6);
    myFireEventSupport.removeListener(lis2);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 10);
    myFireEventSupport.addStraightListener(Lifespan.FOREVER, lis1);
    myFireEventSupport.removeListener(lis3);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 12);
    myFireEventSupport.removeListener(lis1);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 13);
    myFireEventSupport.removeListener(lis1);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 13);
    myFireEventSupport.removeListener(lis1);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 13);
  }
}
