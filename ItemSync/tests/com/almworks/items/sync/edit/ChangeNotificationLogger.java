package com.almworks.items.sync.edit;

import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.exec.ThreadGate;
import junit.framework.Assert;
import org.almworks.util.detach.Lifespan;

import java.util.concurrent.atomic.AtomicInteger;

public class ChangeNotificationLogger implements ChangeListener {
  private final AtomicInteger myCounter = new AtomicInteger();

  @Override
  public void onChange() {
    myCounter.incrementAndGet();
  }

  public static ChangeNotificationLogger listen(Modifiable modifiable) {
    ChangeNotificationLogger logger = new ChangeNotificationLogger();
    modifiable.addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, logger);
    return logger;
  }

  public void checkEmpty() {
    Assert.assertEquals(0, myCounter.get());
  }

  public void reset() {
    myCounter.set(0);
  }

  public void checkNotEmptyAndReset() {
    if (myCounter.get() > 0) {
      myCounter.set(0);
      return;
    }
    Assert.fail("Expected not empty");
  }

  public void checkCountAndReset(int expected) {
    if (!myCounter.compareAndSet(expected, 0)) Assert.fail("Expected " + expected + " but was " + myCounter.get());
  }
}
