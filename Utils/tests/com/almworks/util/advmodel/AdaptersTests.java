package com.almworks.util.advmodel;

import com.almworks.util.models.ListDataSynchronizer;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author : Dyoma
 */
public class AdaptersTests extends GUITestCase {
  private final OrderListModel<String> myModel = new OrderListModel<String>();
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void testOrderModel() {
    AListLogger listener = new AListLogger();
    myModel.addListener(listener);
    myModel.insert(0, "1");
    listener.checkInsert(0, 1);
    myModel.insert(1, "2");
    listener.checkInsert(1, 1);
    assertEquals("1", myModel.getAt(0));
    assertEquals("2", myModel.getAt(1));
    myModel.swap(1, 0);
    AListModel.RearrangeEvent event = listener.popOneRearrageEvent();
    assertEquals(0, event.getNewIndex(1));
    assertEquals(1, event.getNewIndex(0));
  }

  public void testSelectionMove() {
    DefaultListSelectionModel selection = new DefaultListSelectionModel();
    ListSelectionModelAdapter.createListening(myModel, selection, true);
    myModel.insert(0, "1");
    myModel.insert(1, "2");
    selection.addSelectionInterval(0, 0);
    selection.setAnchorSelectionIndex(1);
    myModel.swap(0, 1);
    assertEquals(0, selection.getAnchorSelectionIndex());
    assertTrue(selection.isSelectedIndex(1));
    assertFalse(selection.isSelectedIndex(0));
  }

  public void testSelectionInsert() {
    DefaultListSelectionModel selection = new DefaultListSelectionModel();
    ListSelectionModelAdapter.createListening(myModel, selection, true);
    myModel.insert(0, "2");
    selection.addSelectionInterval(0, 0);
    myModel.insert(0, "1");
    assertTrue(selection.isSelectedIndex(1));
    assertFalse(selection.isSelectedIndex(0));
  }

  public void testSelectionRemove() {
    DefaultListSelectionModel selection = new DefaultListSelectionModel();
    ListSelectionModelAdapter.createListening(myModel, selection, true);
    myModel.addElement("1");
    myModel.addElement("2");
    myModel.addElement("3");
    selection.setSelectionInterval(1, 1);
    myModel.removeAt(1);
    assertTrue(selection.isSelectionEmpty());
  }

  public void testNoSelection() {
    DefaultListSelectionModel selection = new DefaultListSelectionModel();
    ListSelectionModelAdapter.createListening(myModel, selection, true);
    selection.clearSelection();
    myModel.insert(0, "2");
    myModel.insert(1, "1");
    selection.setAnchorSelectionIndex(-1);
    myModel.ensureSorted(String.CASE_INSENSITIVE_ORDER);
    assertEquals(-1, selection.getAnchorSelectionIndex());
  }

  public void testListModel() {
    ListModelAdapter list = new ListModelAdapter(myModel);
    ListDataSynchronizer synchronizer = new ListDataSynchronizer(list);
    myModel.insert(0, "3");
    CHECK.singleElement("3", synchronizer.getImageList());
    myModel.insert(0, "1");
    CHECK.order(new Object[]{"1", "3"}, synchronizer.getImageList());
    myModel.insert(1, "2");
    CHECK.order(new Object[]{"1", "2", "3"}, synchronizer.getImageList());
    myModel.swap(0, 1);
    CHECK.order(new Object[]{"2", "1", "3"}, synchronizer.getImageList());
  }

  public void testListModelUpdate() {
    ListModelAdapter list = new ListModelAdapter(myModel);
    final ListDataEvent[] lastEvent = new ListDataEvent[1];
    list.addListDataListener(new ListDataListener() {
      public void contentsChanged(ListDataEvent e) {
        assertNull(lastEvent[0]);
        lastEvent[0] = e;
      }

      public void intervalAdded(ListDataEvent e) {
      }

      public void intervalRemoved(ListDataEvent e) {
      }
    });
    myModel.insert(0, "1");
    myModel.updateAt(0);
    ListDataEvent event = lastEvent[0];
    assertEquals(0, event.getIndex1());
    assertEquals(ListDataEvent.CONTENTS_CHANGED, event.getType());
  }
}
