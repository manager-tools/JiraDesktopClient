package com.almworks.util.components;

import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.detach.Detach;

/**
 * @author dyoma
 */
public class KeepSelectionTests extends GUITestCase {
  private final CollectionsCompare CHECK = new CollectionsCompare();
  private Detach myDetach = Detach.NOTHING;
  private OrderListModel<String> myModel;
  private SelectionAccessor mySelection;
  private String myLast;
  private String myPreLast;

  protected void setUp() throws Exception {
    super.setUp();
    AList component = new AList();
    myModel = OrderListModel.<String>create();
    mySelection = component.getSelectionAccessor();
    component.setCollectionModel(myModel);
    myDetach = UIUtil.keepSelectionOnRemove(component);
    myModel.addAll(new String[] {"0", "1", "2", "3", "4", "5"});
    int size = myModel.getSize();
    myLast = myModel.getAt(size - 1);
    myPreLast = myModel.getAt(size - 2);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testRemoveNotSelected() {
    mySelection.setSelected("2");
    mySelection.addSelection("5");
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "5"});
    myModel.remove("1");
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "5"});
    myModel.remove("3");
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "5"});
    myModel.remove("0");
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "5"});
    myModel.remove("4");
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "5"});
  }

  public void testRemoveMiddle() {
    mySelection.setSelected("1");
    myModel.remove("1");
    checkSingleSelection("0");
  }

  public void testRemoveFirst() {
    mySelection.setSelected("0");
    myModel.remove("0");
    checkSingleSelection("1");
  }

  public void testRemoveLast() {
    mySelection.setSelected(myLast);
    myModel.remove(myLast);
    checkSingleSelection(myPreLast);
  }

  public void testRemoveNextToSelected() {
    mySelection.setSelected(new String[] {myLast, myPreLast});
    myModel.remove(myLast);
    checkSingleSelection(myPreLast);
  }

  public void testRemoveMultiple() {
    String[] selection = new String[] {"0", "1", "4", "5"};
    mySelection.setSelected(selection);
    myModel.removeAll(selection);
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"2", "3"});
  }

  public void testRemoveMuliple2() {
    String[] selection = new String[] {"1", "3"};
    mySelection.setSelected(selection);
    myModel.removeAll(selection);
    CHECK.unordered(mySelection.getSelectedItems(), new String[] {"0", "2"});
  }

  public void testRemoveAll() {
    mySelection.selectAll();
    myModel.clear();
    CHECK.empty(mySelection.getSelectedItems());
  }

  private void checkSingleSelection(String selected) {
    CHECK.singleElement(selected, mySelection.getSelectedItems());
  }
}
