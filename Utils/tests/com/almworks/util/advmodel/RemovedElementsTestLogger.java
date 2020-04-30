package com.almworks.util.advmodel;

import com.almworks.util.tests.CollectionsCompare;
import junit.framework.Assert;

import java.util.List;

class RemovedElementsTestLogger implements AListModel.RemovedElementsListener<String> {
  private final AListModel<String> myModel;
  private List<String> myLastList;
  private int myLastSize;

  public RemovedElementsTestLogger(AListModel<String> model) {
    myModel = model;
  }

  public void onBeforeElementsRemoved(AListModel.RemoveNotice<String> elements) {
    myLastList = elements.getList();
    myLastSize = myModel.getSize();
    for (String e : myLastList) {
      Assert.assertTrue(myModel.contains(e));
    }
  }

  public void checkList(int count, String ... list) {
    Assert.assertEquals(myLastSize, count);
    new CollectionsCompare().order(list, myLastList);
  }
}
