package com.almworks.util.events;

import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.DetachComposite;

public class LifespanDetachTests extends BaseTestCase {

  public void testDetachOnceIsReallyOnce() {
    final DetachComposite detach = new DetachComposite(true);
    BasicScalarModel<Integer> model = BasicScalarModel.createConstant(10);
    model.getEventSource().addStraightListener(detach, new ScalarModel.Adapter<Integer>() {
      public void onScalarChanged(ScalarModelEvent<Integer> event) {
        if (event.getNewValue() == 10)
          detach.detach();
      }
    });
    assertTrue(detach.isDetached());
    assertEquals(0, detach.count());
  }

}
