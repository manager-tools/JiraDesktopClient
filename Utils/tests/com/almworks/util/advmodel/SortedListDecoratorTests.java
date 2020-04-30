package com.almworks.util.advmodel;

import com.almworks.util.collections.Containers;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;

import java.util.List;

/**
 * @author : Dyoma
 */
public class SortedListDecoratorTests extends GUITestCase {
  private final OrderListModel<String> mySource = new OrderListModel<String>();
  private final SortedListDecorator<String> myModel = SortedListDecorator.createWithoutComparator(mySource);
  private final AListLogger myLogger = new AListLogger();
  private final CollectionsCompare CHECK = new CollectionsCompare();

  public void testNoComparator() {
    myModel.addListener(myLogger);
    mySource.addElement("1");
    myLogger.clear();
    mySource.addElement("2");
    myLogger.checkInsert(1, 1);
    assertEquals(2, myModel.getSize());
    assertEquals("1", myModel.getAt(0));
    assertEquals("2", myModel.getAt(1));
  }

  public void testSortingElements() {
    myModel.addListener(myLogger);
    mySource.addElement("3");
    mySource.addElement("1");
    myLogger.clear();
    assertEquals("3", myModel.getAt(0));
    assertEquals("1", myModel.getAt(1));
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    AListModel.RearrangeEvent event = myLogger.popOneRearrageEvent();
    assertEquals(1, event.getNewIndex(0));
    assertEquals(0, event.getNewIndex(1));
    assertEquals("1", myModel.getAt(0));
    mySource.addElement("2");
    assertEquals("2", myModel.getAt(1));
    assertEquals("3", myModel.getAt(2));
    myLogger.checkInsert(1, 1);
  }

  public void testSortingAfterSwap() {
    mySource.addElement("1");
    mySource.addElement("2");
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    mySource.swap(0, 1);
    assertEquals("1", myModel.getAt(0));
    assertEquals("2", myModel.getAt(1));
  }

  public void testKeepingSameElement() {
    String first = new String("1");
    String second = new String("1");
    mySource.addElement(first);
    mySource.addElement(second);
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    myModel.addListener(myLogger);
    assertSame(first, myModel.getAt(0));
    mySource.swap(0, 1);
    assertSame(first, myModel.getAt(1));
    AListModel.RearrangeEvent event = myLogger.popOneRearrageEvent();
    assertEquals(1, event.getNewIndex(0));
    assertEquals(0, event.getNewIndex(1));
  }

  public void testUpdate() {
    mySource.addElement("1");
    mySource.addElement("2");
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    myModel.addListener(myLogger);
    mySource.updateAt(0);
    AListModel.UpdateEvent event = myLogger.popOneUpdateEvent();
    assertEquals(0, event.getLowAffectedIndex());
    assertTrue(event.isUpdated(0));
    mySource.updateRange(0, 1);
    event = myLogger.popOneUpdateEvent();
    assertEquals(0, event.getLowAffectedIndex());
    assertEquals(1, event.getHighAffectedIndex());
  }

  public void testRemove() {
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    mySource.addElement("1");
    mySource.addElement("2");
    myModel.addListener(myLogger);
    mySource.removeAt(1);
    myLogger.checkRemove(1, 1);
    assertEquals(1, myModel.getSize());
    assertEquals("1", myModel.getAt(0));
    myModel.setComparator(Containers.reverse(String.CASE_INSENSITIVE_ORDER));
    mySource.addElement("2");
    myLogger.clear();
    mySource.removeAt(1);
    myLogger.checkRemove(0, 1);
    assertEquals("1", myModel.getAt(0));
  }

  public void testRemove2() {
    myModel.setComparator(String.CASE_INSENSITIVE_ORDER);
    mySource.addElement("1");
    mySource.addElement("2");
    CHECK.order(new String[]{"1", "2"}, myModel.toList());
    mySource.removeAt(0);
    CHECK.singleElement("2", myModel.toList());
    mySource.removeAt(0);
    CHECK.empty(myModel.toList());
  }

  public void testInsertToSourceBeginning() {
    mySource.addElement("2");
    mySource.insert(0, "1");
    assertTrue(myModel.verifyIntegrity());
  }

  public void testRemoveNotice() {
    mySource.addElement("1");
    mySource.addElement("2");
    myModel.addListener(new AListModel.Adapter() {
      public void onRemove(int index, int length, AListModel.RemovedEvent event) {
        List<String> removed = event.getAllRemoved();
        for (String r : removed) {
          assertEquals("1", r);
        }
      }
    });
    mySource.remove("1");
  }
}
