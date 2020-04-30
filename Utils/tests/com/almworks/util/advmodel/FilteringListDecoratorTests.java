package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import com.almworks.util.tests.GUITestCase;

/**
 * @author : Dyoma
 */
public class FilteringListDecoratorTests extends GUITestCase {
  private static final Condition<String> EVEN_LENGTH = new Condition<String>() {
    public boolean isAccepted(String value) {
      return value.length() % 2 == 0;
    }
  };

  private final OrderListModel<String> mySource = new OrderListModel<String>();
  private final FilteringListDecorator<String> myModel = FilteringListDecorator.create(mySource);
  private final AListLogger myLogger = new AListLogger();

  protected void setUp() throws Exception {
    super.setUp();
    myModel.setFilter(EVEN_LENGTH);
  }

  public void testInsert() {
    mySource.addElement("1");
    assertEquals(0, myModel.getSize());
    mySource.addElement("22");
    assertTrue(myModel.checkConsistent());
    mySource.insert(0, "x");
    assertTrue(myModel.checkConsistent());
    assertEquals(1, myModel.getSize());
    assertEquals("22", myModel.getAt(0));
    mySource.insert(0, "33");
    assertEquals("33", myModel.getAt(0));
    mySource.addElement("44");
    assertEquals("44", myModel.getAt(2));
  }

  public void testChangeFilter() {
    mySource.addElement("1");
    mySource.addElement("22");
    mySource.addElement("333");
    myModel.addListener(myLogger);
    myModel.setFilter(Condition.<String>always());
    assertEquals(3, myModel.getSize());
    myLogger.checkLogSize(2);
    myLogger.clear();
    myModel.setFilter(EVEN_LENGTH.not());
    myLogger.checkRemove(1, 1);
  }

  public void testRemove() {
    mySource.addElement("11");
    mySource.addElement("2");
    mySource.addElement("33");
    mySource.addElement("4");
    mySource.removeRange(0, 1);
    assertEquals(1, myModel.getSize());
    assertEquals("33", myModel.getAt(0));
    myModel.addListener(myLogger);
    mySource.removeAt(1);
    myLogger.checkLogSize(0);
  }

  public void testUpdate() {
    mySource.addElement("1");
    mySource.addElement("22");
    myModel.addListener(myLogger);
    mySource.updateAt(0);
    myLogger.checkLogSize(0);
    mySource.updateAt(1);
    myLogger.checkLogSize(1);
    myLogger.clear();
    mySource.addElement("3");
    mySource.updateAt(1);
    myLogger.checkLogSize(1);
    mySource.swap(1, 2);
    assertTrue(myModel.checkConsistent());
    mySource.addElement("44");
    mySource.swap(2, 3);
    assertTrue(myModel.checkConsistent());
    assertEquals("44", myModel.getAt(0));
  }

  public void testResyncGroupsRemoves() {
    for (int i = 0; i < 100; i++)
      mySource.addElement("" + i);
    myModel.addListener(myLogger);
    myModel.setFilter(Condition.always());
    myLogger.clear();
    myModel.setFilter(Condition.never());
    myLogger.checkLogSize(1);
    myLogger.checkRemove(0, 100);
  }

  public void testResyncGroupsAdds() {
    for (int i = 0; i < 100; i++)
      mySource.addElement("" + i);
    myModel.addListener(myLogger);
    myModel.setFilter(Condition.never());
    myLogger.clear();
    myModel.setFilter(Condition.always());
    myLogger.checkLogSize(1);
    myLogger.checkInsert(0, 100);
  }

  // This tests fails. This is not an implementation bug but rather a problem in the contract of AModels, which should be fixed by AModels 2.0.
/*
  public void testSourceModelUpdate() {
    for (int i = 0; i < 5; ++i) {
      mySource.addElement(String.valueOf(i));
    }
    myModel.setFilter(Condition.<Object>always());
    CollectionsCompare comp = new CollectionsCompare();
    comp.order(mySource.toList(), myModel.toList());
    mySource.replaceAt(2, "239");
    comp.order(mySource.toList(), myModel.toList());
  }
*/
}

