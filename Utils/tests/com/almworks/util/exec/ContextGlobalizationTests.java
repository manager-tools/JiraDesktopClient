package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public class ContextGlobalizationTests extends BaseTestCase {
  public void testGlobalization() throws InvocationTargetException, InterruptedException {
    Context.add(InstanceProvider.instance(new LongEventQueueImpl()), "x");
    Context.add(InstanceProvider.instance("hi"), "y");
    int token = Context.globalize();
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        ThreadGate gate = ThreadGate.LONG(0);
        assertNotSame(gate, ThreadGate.STRAIGHT);
        assertEquals("hi", Context.get(String.class));
      }
    });
    Context.pop();
    Context.unglobalize(token);
  }
}
