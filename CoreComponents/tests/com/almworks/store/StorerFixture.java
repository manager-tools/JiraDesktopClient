package com.almworks.store;

import com.almworks.misc.TestWorkArea;
import com.almworks.util.tests.BaseTestCase;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class StorerFixture extends BaseTestCase {
  protected FileStorer myFile;
  protected TestWorkArea myWorkArea;
  private long myTiming;

  protected void setUp() throws Exception {
    super.setUp();
    myWorkArea = new TestWorkArea();
    myFile = new FileStorer(myWorkArea);
    myTiming = 0;
  }

  protected void tearDown() throws Exception {
    myFile = null;
    myWorkArea.cleanUp();
    super.tearDown();
  }

  protected void timing() {
    long time = System.currentTimeMillis();
    if (myTiming > 0) {
      // System.out.println((time - myTiming) + "ms");
    }
    myTiming = time;
  }
}

