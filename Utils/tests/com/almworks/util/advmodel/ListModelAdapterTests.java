package com.almworks.util.advmodel;


import com.almworks.util.tests.GUITestCase;
import org.almworks.util.Collections15;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.List;

/**
 * @author dyoma
 */
public class ListModelAdapterTests extends GUITestCase {
  private final List<ListDataEvent> myEvents = Collections15.arrayList();
  private final OrderListModel<String> myModel = OrderListModel.create();
  private final ListModelAdapter myAdapter = new ListModelAdapter(myModel);

  protected void setUp() throws Exception {
    super.setUp();
    myAdapter.addListDataListener(new ListDataListener() {
      public void contentsChanged(ListDataEvent e) {
        myEvents.add(e);
      }

      public void intervalAdded(ListDataEvent e) {
        myEvents.add(e);
      }

      public void intervalRemoved(ListDataEvent e) {
        myEvents.add(e);
      }
    });
  }

  public void testAdd() {
    myModel.addElement("0");
    ListDataEvent event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_ADDED, 0, 0);
    myModel.addAll(new String[]{"1", "2"});
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_ADDED, 1, 2);
    myModel.addAll(new String[]{"3", "4"});
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_ADDED, 3, 4);
  }

  public void testRemove() {
    myModel.addAll(new String[]{"0", "1", "2", "3", "4", "5"});
    clearEvents();
    myModel.removeAt(5);
    ListDataEvent event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_REMOVED, 5, 5);
    myModel.removeRange(2, 3);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_REMOVED, 2, 3);
    myModel.removeRange(0, 1);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.INTERVAL_REMOVED, 0, 1);
    assertEquals(1, myModel.getSize());
    assertEquals("4", myModel.getAt(0));
  }

  public void testUpdate() {
    myModel.addAll(new String[]{"0", "1", "2"});
    clearEvents();
    myModel.updateAt(1);
    ListDataEvent event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 1, 1);
    myModel.updateRange(0, 1);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 0, 1);
    myModel.updateRange(1, 2);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 1, 2);
  }

  public void testRearrange() {
    myModel.addAll(new String[]{"1", "0", "2"});
    clearEvents();
    myModel.swap(0, 1);
    ListDataEvent event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 0, 1);
    myModel.swap(0, 2);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 0, 2);
    myModel.sort(String.CASE_INSENSITIVE_ORDER);
    event = getSingleEvent();
    checkEvent(event, ListDataEvent.CONTENTS_CHANGED, 0, 2);
  }

  private void checkEvent(ListDataEvent event, int type, int index0, int index1) {
    assertEquals(type, event.getType());
    assertEquals(index0, event.getIndex0());
    assertEquals(index1, event.getIndex1());
  }

  private ListDataEvent getSingleEvent() {
    assertEquals(1, myEvents.size());
    return myEvents.remove(0);
  }

  private void clearEvents() {
    myEvents.clear();
  }
}
