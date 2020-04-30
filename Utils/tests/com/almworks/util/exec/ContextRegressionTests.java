package com.almworks.util.exec;

import com.almworks.util.tests.BaseTestCase;

public class ContextRegressionTests extends BaseTestCase {
  // it's not a test of that bug....
  public void testLoop() {
    Context.add(InstanceProvider.instance("1"), "1");
    Context.add(InstanceProvider.instance("2"), "2");
    Context.add(InstanceProvider.instance("3"), "3");
    ContextFrame f3 = Context.getTopFrame();
    Context.pop();
    ContextFrame f2 = Context.getTopFrame();
    Context.pop();
    Context.add(new ContextFrameDataProvider(f3), "+3");
    Context.add(new ContextFrameDataProvider(f2), "+2");

    Integer c = Context.get(Integer.class);
  }
}
