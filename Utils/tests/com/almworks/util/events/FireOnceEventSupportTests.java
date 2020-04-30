package com.almworks.util.events;

import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Failure;
import org.almworks.util.detach.Lifespan;

/**
 * :todoc:
 *
 * @author sereda
 */
public class FireOnceEventSupportTests extends BaseTestCase {
  public static final String MEGA_ARGUMENT = "mega";

  protected FireEventSupport<Listener> fireEventSupport;
  protected Listener dispatcher;

  protected void setUp() throws Exception {
    fireEventSupport = FireEventSupport.createFireOnce(Listener.class);
    dispatcher = fireEventSupport.getDispatcher();
  }

  protected void tearDown() throws Exception {
    fireEventSupport = null;
    dispatcher = null;
  }

  public void testNormalDispatch() {
    final int[] count = {0};
    boolean added;

    //assertTrue(!fireEventSupport.());

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 1;
      }
    });
    assertTrue(added);

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 2;
      }
    });
    assertTrue(added);

    assertTrue(count[0] == 0);
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 3);

    // after single firing
    assertTrue(fireEventSupport.isNoMoreEvents());

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
      }
    });
    assertTrue(!added);

    try {
      dispatcher.mega(MEGA_ARGUMENT);
    } catch (Failure e) {
      //okay - assertions are on
    }
    assertTrue(count[0] == 3);
  }

  public void testDispatchSnapshot() {
    final int[] count = {0};
    boolean added;

    assertTrue(!fireEventSupport.isNoMoreEvents());

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 1;
      }
    });
    assertTrue(added);

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 2;
      }
    });
    assertTrue(added);

    dispatcher = fireEventSupport.getDispatcherSnapshot();

    // after single firing
    assertTrue(count[0] == 0);
    assertTrue(fireEventSupport.isNoMoreEvents());

    added = fireEventSupport.addStraightListener(Lifespan.FOREVER, new Listener() {
      public void mega(String mega) {
        assertTrue(mega.equals(MEGA_ARGUMENT));
        count[0] += 3;
      }
    });
    assertTrue(!added);

    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 3);

    // dispatcher works only once too
    dispatcher.mega(MEGA_ARGUMENT);
    assertTrue(count[0] == 3);
  }
}
