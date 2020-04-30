package com.almworks.util.io.persist;

import com.almworks.util.tests.BaseTestCase;

import java.io.IOException;

/**
 * @author Vasya
 */
public class PersistableContainerTests extends BaseTestCase {
  public void testBaseContarct() throws IOException {
    MyPersistableContainer container = new MyPersistableContainer();
    container.persist(new PersistableBoolean(true));
    container.getChildren();
    try {
      container.persist(new PersistableInteger(1));
      fail("No IllegalStateException");
    } catch (IllegalStateException e) {
      //normal
    }
  }

  public static class MyPersistableContainer extends PersistableContainer {
    public MyPersistableContainer() {
      super();
    }
  }

}
