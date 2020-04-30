package com.almworks.util.advmodel;

import com.almworks.util.commons.Condition;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

/**
 * @author : Dyoma
 */
public class OrderListModelTests extends GUITestCase {
  private final AListLogger myLogger = new AListLogger();
  private OrderListModel myModel;
  private static final CollectionsCompare CHECK = new CollectionsCompare();

  public void testRemoveAll() {
    myModel = new OrderListModel();
    myModel.addElement("1");
    myModel.addElement("2");
    myModel.addElement("3");
    myModel.addElement("4");
    myModel.addListener(myLogger);
    myModel.removeAll(new Condition() {
      public boolean isAccepted(Object value) {
        return myModel.indexOf(value) % 2 == 1;
      }
    });
    myLogger.checkLogSize(2);
    checkModel("1", "3");

    myModel.insert(1, "2");
    myModel.insert(3, "4");
    myLogger.clear();
    myModel.removeAll(new Condition() {
      public boolean isAccepted(Object value) {
        return myModel.indexOf(value) % 2 == 0;
      }
    });
    myLogger.checkLogSize(2);
    checkModel("2", "4");
  }

  public void testRemoveAllIndexes() {
    myModel = new OrderListModel();
    myModel.addAll("0", "1", "2", "3", "4");
    myModel.addListener(myLogger);
    myModel.removeAll(new int[]{0, 1, 4});
    myLogger.checkLogSize(2);
    checkModel("2", "3");
  }

  private void checkModel(Object ... expected) {
    CHECK.order(expected, myModel.toList());
  }
}
