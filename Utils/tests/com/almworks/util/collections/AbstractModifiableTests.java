package com.almworks.util.collections;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import java.util.Arrays;

/**
 * @author dyoma
 */
public class AbstractModifiableTests extends BaseTestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final SimpleModifiable myModifiable = new MyModifiable();
  private final StringBuffer myOrder = new StringBuffer();

  public void testOneListener() {
    SampleListener listener = listener("1");
    DetachComposite life = new DetachComposite();
    myModifiable.addChangeListener(life, listener, listener);
    fireAndCheck(listener);
    life.detach();
    myModifiable.fireChanged();
    assertEquals("", myOrder.toString());
  }

  public void testSingleGate() {
    SampleListener gate = listener("gate");
    SampleListener l1 = listener("1");
    Lifecycle detach1 = new Lifecycle();
    myModifiable.addChangeListener(detach1.lifespan(), gate, l1);
    SampleListener l2 = listener("2");
    Lifecycle detach2 = new Lifecycle();
    myModifiable.addChangeListener(detach2.lifespan(), gate, l2);
    fireAndCheck(gate, new SampleListener[]{l1, l2});
    detach1.cycle();
    fireAndCheck(gate, l2);

    detach2.cycle();
    myModifiable.fireChanged();
    assertEquals("", myOrder.toString());

    myModifiable.addChangeListener(detach1.lifespan(), gate, l1);
    fireAndCheck(gate, l1);

    myModifiable.addChangeListener(detach2.lifespan(), gate, l2);
    fireAndCheck(gate, new SampleListener[]{l1, l2});

    detach2.cycle();
    fireAndCheck(gate, l1);

    detach1.cycle();
    myModifiable.fireChanged();
    assertEquals("", myOrder.toString());
  }

  private SampleListener listener(String name) {
    return new SampleListener(myOrder, name);
  }

  public void testDifferentGates() {
    SampleListener gate = listener("gate");
    SampleListener l1 = listener("1");
    SampleListener l2 = listener("2");
    SampleListener l3 = listener("3");
    myModifiable.addChangeListener(Lifespan.FOREVER, gate, l1);
    DetachComposite detach2 = new DetachComposite();
    myModifiable.addChangeListener(detach2, gate, l2);
    myModifiable.addChangeListener(Lifespan.FOREVER, l3, l3);
    fireAndCheck(new SampleListener[]{gate, gate, l3}, new SampleListener[]{l1, l2, l3});
    detach2.detach();
    fireAndCheck(new SampleListener[]{gate, l3}, new SampleListener[]{l1, l3});
  }


  private void fireAndCheck(SampleListener[] gates, SampleListener[] listeners) {
    assertEquals(gates.length , listeners.length);
    myModifiable.fireChanged();
    StringBuffer expectedBuffer = new StringBuffer();
    for (int i = 0; i < listeners.length; i++)
      expectedBuffer.append(gates[i].getName() + ":" + listeners[i].getName() + " ");
    assertEquals(expectedBuffer.toString(), myOrder.toString());
    myOrder.setLength(0);
  }

  private void fireAndCheck(SampleListener gate, SampleListener[] listeners) {
    SampleListener[] gates = new SampleListener[listeners.length];
    Arrays.fill(gates, gate);
    fireAndCheck(gates, listeners);
  }


  private void fireAndCheck(SampleListener gate, SampleListener listener) {
    fireAndCheck(new SampleListener[]{gate}, new SampleListener[]{listener});
  }

  private void fireAndCheck(SampleListener single) {
    fireAndCheck(single, single);
  }

  private static class SampleListener extends ThreadGate implements ChangeListener {
    private final StringBuffer myOrder;
    private final String myName;
    private boolean myExecuting = false;

    public SampleListener(StringBuffer order, String name) {
      myOrder = order;
      myName = name;
    }

    public synchronized void gate(Runnable command) {
      assertFalse(myExecuting);
      myExecuting = true;
      myOrder.append(myName + ":");
      command.run();
      myExecuting = false;
    }

    public synchronized void onChange() {
      myOrder.append(myName + " ");
    }

    public String toString() {
      return myName;
    }

    public String getName() {
      return myName;
    }

    protected Target getTarget() {
      return Target.STRAIGHT;
    }

    protected Type getType() {
      return Type.IMMEDIATE;
    }
  }

  private static class MyModifiable extends SimpleModifiable { }
}
