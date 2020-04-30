package com.almworks.container;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.tests.BaseTestCase;

public abstract class PicoTestCase extends BaseTestCase {
  private RootContainerImpl myRootContainer;

  public PicoTestCase() {
    super();
  }

  protected PicoTestCase(int timeout) {
    super(timeout);
  }

  protected final void tearDown() throws Exception {
    picoTearDown();
    clearContainer();
    super.tearDown();
  }

  protected final void setUp() throws Exception {
    super.setUp();
    createContainer();
    picoSetUp();
    if (myRootContainer != null)
      myRootContainer.start();
  }

  protected MutableComponentContainer getContainer() {
    return myRootContainer.getMainContainer();
  }

  protected void picoTearDown() throws Exception {
  }

  protected void picoSetUp() throws Exception {
  }

  protected ComponentContainer createContainer() {
    clearContainer();
    myRootContainer = new RootContainerImpl();
    return myRootContainer.getMainContainer();
  }

  private synchronized void clearContainer() {
    if (myRootContainer != null) {
      // myRootContainer.stop(); - no stop() now
      myRootContainer = null;
    }
  }
}
