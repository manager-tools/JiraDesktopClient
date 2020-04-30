package com.almworks.util.progress;

import com.almworks.util.tests.BaseTestCase;

import java.util.List;

public class ProgressRegressionTests extends BaseTestCase {
  public void testDoneProgressLosesErrors() {
    Progress p = new Progress();
    Progress q = p.createDelegate();
    q.addError("error!");
    p.setDone();

    List<String> errors = p.getErrors(null);
    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertEquals("error!", errors.get(0));
  }
}
