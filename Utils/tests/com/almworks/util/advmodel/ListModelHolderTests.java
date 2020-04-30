package com.almworks.util.advmodel;

import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.detach.Detach;

public class ListModelHolderTests extends BaseTestCase {
  private ListModelHolder<String> myHolder;

  public ListModelHolderTests() {
    super(AwtTestsGate.AWT_FOR_TEST);
  }

  protected void setUp() throws Exception {
    super.setUp();
    myHolder = ListModelHolder.create();
  }

  protected void tearDown() throws Exception {
    myHolder = null;
    super.tearDown();
  }

  public void testProperNotificationsWhenModelChanges() {
    AListModel<String> model1 = FixedListModel.create("1");
    AListModel<String> model2 = FixedListModel.create("2");
    Detach detach = myHolder.setModel(model1);
    AListLogger listener = new AListLogger();
    myHolder.addListener(listener);
    RemovedElementsTestLogger rlogger = new RemovedElementsTestLogger(myHolder);
    myHolder.addRemovedElementListener(rlogger);

    detach.detach();

    myHolder.setModel(model2);
    rlogger.checkList(1, "1");
    listener.checkLogSize(2, true);

    myHolder.setModel(null);
    rlogger.checkList(1, "2");
    listener.checkRemove(0, 1);

    myHolder.setModel(FixedListModel.create("3", "4"));
    listener.checkInsert(0, 2);
  }
}
