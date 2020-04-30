package com.almworks.items.cache;

import org.almworks.util.detach.DetachComposite;

public class ImageTests extends ImageFixture {

  public void testSimpleCase() throws InterruptedException {
    long item = setValue(1, "a");
    ManualImageSlice slice = myImage.manualSlice(new DetachComposite());
    Supervisor supervisor = Supervisor.create(slice);
    slice.addItems(item);
    supervisor.waitForItem(item);
    CHECK.unordered(slice.getActualItems().toNativeArray(), item);
    slice.addAttributes(ID, VALUE);
    Integer id = supervisor.waitForData(item, ID);
    assertEquals(1, id.intValue());
    assertEquals("a", slice.getValue(item, VALUE_LOADER));

    int count = supervisor.getEventCount();
    setValue(1, "b");
    supervisor.waitForEvent(count + 1);
    assertEquals("b", slice.getValue(item, VALUE_LOADER));
  }

  public void testManualSliceEvents() throws InterruptedException {
    DetachComposite life = new DetachComposite();
    ManualImageSlice slice = myImage.manualSlice(life);
    Supervisor supervisor = Supervisor.create(slice);

    long item = setValue(1, "a");
    EventCopy event = supervisor.waitForEvent(1);
    assertEquals(event.getICN(), myImage.getICN());
    assertEquals(1, supervisor.getEventCount());

    slice.addItems(item);
    event = supervisor.waitForEvent(2);
    assertEquals(2, supervisor.getEventCount());
    CHECK.unordered(event.getAdded(), item);

    slice.addData(VALUE_LOADER);
    event = supervisor.waitForEvent(3);
    assertEquals(3, supervisor.getEventCount());
    CHECK.unordered(event.getChange(), item);
    assertTrue(event.isChanged(item, VALUE_LOADER));

    life.detach();
    event = supervisor.waitForEvent(4);
    assertEquals(4, supervisor.getEventCount());
    CHECK.unordered(event.getRemoved(), item);
  }
}
