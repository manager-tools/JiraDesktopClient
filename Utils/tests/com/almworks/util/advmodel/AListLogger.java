package com.almworks.util.advmodel;

import junit.framework.Assert;
import org.almworks.util.Util;

/**
 * @author : Dyoma
 */
class AListLogger implements AListModel.Listener {
  public static final String INSERT = "insert";
  public static final String REMOVE = "remove";
  public static final String UPDATE = "update";

  private int myIndex = -1;
  private int myLength = -1;
  private AListModel.AListEvent myLastEvent = null;
  private int myEventCounter = 0;
  private String myLastType = null;

  public void onListRearranged(AListModel.AListEvent event) {
    myLastEvent = event;
    myEventCounter++;
  }

  public void onItemsUpdated(AListModel.UpdateEvent event) {
    myLastType = UPDATE;
    storeInterval(event.getLowAffectedIndex(), event.getAffectedLength());
    myLastEvent = event;
  }

  public void onInsert(int lowIndex, int length) {
    myLastType = INSERT;
    storeInterval(lowIndex, length);
  }

  public void onRemove(int index, int length, AListModel.RemovedEvent event) {
    myLastType = REMOVE;
    storeInterval(event.getFirstIndex(), event.getLength());
  }

  public void checkInsert(int index, int length) {
    checkInterval(INSERT, index, length);
  }

  public void checkRemove(int index, int length) {
    checkInterval(REMOVE, index, length);
  }

  public void checkUpdate(int index, int length) {
    checkInterval(UPDATE, index, length);
  }

  private void checkInterval(String type, int index, int length) {
    checkLogSize(1);
    Assert.assertEquals(type, myLastType);
    Assert.assertEquals(index, myIndex);
    Assert.assertEquals(length, myLength);
    reset();
  }

  private void storeInterval(int index, int length) {
    assert index >= 0;
    assert length >= 0;
    myIndex = index;
    myLength = length;
    myEventCounter++;
  }

  public AListModel.RearrangeEvent popOneRearrageEvent() {
    return popOneEventOfType(AListModel.RearrangeEvent.class);
  }

  public AListModel.UpdateEvent popOneUpdateEvent() {
    return popOneEventOfType(AListModel.UpdateEvent.class);
  }

  public <T extends AListModel.AListEvent> T popOneEventOfType(Class<? extends T> eventClass) {
    checkLogSize(1);
    Assert.assertNotNull(myLastEvent);
    T last = Util.cast(eventClass, myLastEvent);
    reset();
    return last;
  }

  private void reset() {
    myLastEvent = null;
    myIndex = -1;
    myLength = -1;
    myEventCounter = 0;
    myLastType = null;
  }

  public void clear() {
    reset();
  }

  public void checkLogSize(int size) {
    checkLogSize(size, false);
  }

  public void checkLogSize(int size, boolean clear) {
    Assert.assertEquals(size, myEventCounter);
    if (clear)
      clear();
  }

  public void checkSilence() {
    checkLogSize(0, true);
  }

  public void checkRemoveAndInsert(int indexRemove, int lengthRemove, int indexInsert, int lengthInsert) {

  }
}
