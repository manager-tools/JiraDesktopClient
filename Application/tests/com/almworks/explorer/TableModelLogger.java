package com.almworks.explorer;


import junit.framework.Assert;
import org.almworks.util.Collections15;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.List;

/**
 * @author : Dyoma
 */
public class TableModelLogger implements TableModelListener {
  private final List<TableModelEvent> myEvents = Collections15.arrayList();

  public void tableChanged(TableModelEvent e) {
    myEvents.add(e);
  }

  public void clear() {
    myEvents.clear();
  }

  public void checkSize(int expectedSize) {
    Assert.assertEquals(expectedSize, myEvents.size());
  }

  public TableModelEvent getTheOnlyEvent() {
    checkSize(1);
    return myEvents.remove(0);
  }

  public void checkTheOnlyEvent(int type, int firstRow, int lastRow, int column) {
    TableModelEvent event = getTheOnlyEvent();
    checkEvent(event, type, firstRow, lastRow, column);
  }

  private static void checkEvent(TableModelEvent event, int type, int firstRow, int lastRow, int column) {
    Assert.assertEquals(type, event.getType());
    Assert.assertEquals(firstRow, event.getFirstRow());
    Assert.assertEquals(lastRow, event.getLastRow());
    Assert.assertEquals(column, event.getColumn());
  }

  public void checkTheOnlyHeaderEvent() {
    checkHeaderEvent(getTheOnlyEvent());
  }

  public void checkEmpty() {
    Assert.assertTrue(myEvents.isEmpty());
  }

  public String toString() {
    return myEvents.size() + " events";
  }

  public TableModelEvent getFirstEvent(int eventCount) {
    checkSize(eventCount);
    return myEvents.remove(0);
  }

  public static void checkHeaderEvent(TableModelEvent event) {
    checkEvent(event, TableModelEvent.UPDATE, TableModelEvent.HEADER_ROW, TableModelEvent.HEADER_ROW,
      TableModelEvent.ALL_COLUMNS);
  }
}
