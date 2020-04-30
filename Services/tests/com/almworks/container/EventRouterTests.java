package com.almworks.container;

import com.almworks.api.container.EventRouter;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

/**
 * :todoc:
 *
 * @author sereda
 */
public class EventRouterTests extends BaseTestCase {
  private EventRouterImpl myRouter;

  protected void setUp() throws Exception {
    super.setUp();
    myRouter = new EventRouterImpl(null);
  }

  protected void tearDown() throws Exception {
    myRouter = null;
    super.tearDown();
  }

  public void testOneOrder() {
    EventSummer summer = new EventSummer();
    EventMultiplier multiplier = new EventMultiplier();

    DetachComposite summerDetach = new DetachComposite();
    DetachComposite multiplierDetach = new DetachComposite();
    myRouter.addListener(summerDetach, ThreadGate.STRAIGHT, summer);
    myRouter.addListener(multiplierDetach, ThreadGate.STRAIGHT, multiplier);
    Listener sink = myRouter.getEventSink(Listener.class, false);

    play(sink, summer, multiplier, summerDetach, multiplierDetach);
  }

  public void testAnotherOrder() {
    EventSummer summer = new EventSummer();
    EventMultiplier multiplier = new EventMultiplier();

    Listener sink = myRouter.getEventSink(Listener.class, false);
    DetachComposite summerDetach = new DetachComposite();
    DetachComposite multiplierDetach = new DetachComposite();
    myRouter.addListener(summerDetach, ThreadGate.STRAIGHT, summer);
    myRouter.addListener(multiplierDetach, ThreadGate.STRAIGHT, multiplier);

    play(sink, summer, multiplier, summerDetach, multiplierDetach);
  }

  public void testFromParentToChild() {
    EventSummer summer = new EventSummer();
    EventMultiplier multiplier = new EventMultiplier();
    EventRouter child = new EventRouterImpl(myRouter);

    Listener sink = myRouter.getEventSink(Listener.class, false);
    DetachComposite summerDetach = new DetachComposite();
    DetachComposite multiplierDetach = new DetachComposite();
    child.addListener(summerDetach, ThreadGate.STRAIGHT, summer);
    myRouter.addListener(multiplierDetach, ThreadGate.STRAIGHT, multiplier);

    play(sink, summer, multiplier, summerDetach, multiplierDetach);
  }

  public void testFromChildToParentNO() {
    EventSummer summer = new EventSummer();
    EventMultiplier multiplier = new EventMultiplier();
    EventRouter child = new EventRouterImpl(myRouter);

    myRouter.getEventSink(Listener.class, false);
    child.addListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, summer);
    myRouter.addListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, multiplier);

    child.getEventSink(Listener.class, false).onSomething(10);
    assertEquals(10, summer.getSum());
    assertEquals(1, multiplier.getProd());
  }

  private void play(Listener sink, EventSummer summer,
    EventMultiplier multiplier, Detach summerDetach, Detach multiplierDetach) {

    sink.onSomething(3);
    assertEquals(3, summer.getSum());
    assertEquals(3, multiplier.getProd());
    summerDetach.detach();
    sink.onSomething(4);
    assertEquals(3, summer.getSum());
    assertEquals(12, multiplier.getProd());
    multiplierDetach.detach();
    sink.onSomething(42);
    assertEquals(3, summer.getSum());
    assertEquals(12, multiplier.getProd());
  }


  public static interface Listener {
    void onSomething(int something);
  }

  private static class EventProducer {
    private final Listener myEventSink;

    public EventProducer(Listener eventSink) {
      assert eventSink != null;
      myEventSink = eventSink;
    }

    public void doSomething(int something) {
      myEventSink.onSomething(something);
    }
  }

  private static class EventSummer implements Listener {
    private int sum = 0;

    public void onSomething(int something) {
      sum += something;
    }

    public int getSum() {
      return sum;
    }
  }

  private static class EventMultiplier implements Listener {
    private int prod = 1;

    public void onSomething(int something) {
      prod *= something;
    }

    public int getProd() {
      return prod;
    }
  }
}
