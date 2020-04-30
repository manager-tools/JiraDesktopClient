package com.almworks.items.cache.util;

import com.almworks.integers.*;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.ImageSliceEvent;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AbstractAListModel;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.detach.Lifespan;

import static java.util.Collections.singletonList;

public class ItemSetModel<T> extends AbstractAListModel<T> {
  private final ImageSlice mySlice;
  private final ItemWrapperFactory<? extends T> myFactory;
  private LongList myCurrentItems = LongList.EMPTY;
  private boolean myInitialized = false;

  public ItemSetModel(ImageSlice slice, ItemWrapperFactory<? extends T> factory) {
    mySlice = slice;
    myFactory = factory;
  }

  public static <T> ItemSetModel<T> create(Lifespan life, ImageSlice slice, ItemWrapperFactory<? extends T> factory) {
    final ItemSetModel<T> model = new ItemSetModel<T>(slice, factory);
    model.start(life);
    return model;
  }

  public DBImage getImage() {
    return mySlice.getImage();
  }

  public int indexOfItem(long item) {
    return mySlice.getActualItems().indexOf(item);
  }

  public <V> T findImageByValue(DataLoader<V> loader, V value) {
    int index = getSlice().findIndexByValue(0, loader, value);
    return index < 0 ? null : getAt(index);
  }

  public void start(Lifespan life) {
    mySlice.addListener(life, new ImageSlice.Listener() {
      @Override
      public void onChange(ImageSliceEvent event) {
        onSliceChanged(event);
      }
    });
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        doInit();
      }
    });
  }

  @Override
  public int getSize() {
    Threads.assertAWTThread();
    return myCurrentItems.size();
  }

  @Override
  public T getAt(int index) {
    Threads.assertAWTThread();
    long item = myCurrentItems.get(index);
    return myFactory.getForItem(mySlice, item);
  }

  public ImageSlice getSlice() {
    return mySlice;
  }

  private void doInit() {
    Threads.assertAWTThread();
    if (myInitialized) return;
    myInitialized = true;
    myCurrentItems = mySlice.getActualItems();
    if (myCurrentItems.size() > 0) fireInsert(0, myCurrentItems.size());
  }

  private void onSliceChanged(ImageSliceEvent event) {
    Threads.assertAWTThread();
    if (!myInitialized) {
      doInit();
      return;
    }
    LongList actualItems = event.getSlice().getActualItems();
    LongArray workCopy = LongArray.copy(myCurrentItems);
    myCurrentItems = workCopy;

    LongList removed = event.getRemoved();
    LongList added = event.getAdded();
    LongList changed = event.getChanged();

    processRemove(workCopy, removed);
    processAdd(workCopy, added);
    assert checkEqualLists(actualItems, workCopy);
    myCurrentItems = actualItems;

    IntList changedIdxs = processUpdate(workCopy, changed);
    myFactory.afterChange(removed, changed, added);
    // Update is fired after the factory is notified so that client get up-to-date values. Note that we don't have to do the same with add/remove.
    for (IntIterator i = changedIdxs.iterator(); i.hasNext(); ) {
      fireUpdate(i.nextValue());
    }
  }

  private void processRemove(LongArray workCopy, LongList removed) {
    for (LongListIterator i = removed.iterator(); i.hasNext(); ) {
      long item = i.nextValue();
      int index = workCopy.indexOf(item);
      RemovedEvent<T> removeEvent =
        fireBeforeElementsRemoved(index, singletonList(myFactory.getForItem(mySlice, item)));
      workCopy.removeAt(index);
      fireRemoved(removeEvent);
    }
  }

  private void processAdd(LongArray workCopy, LongList added) {
    for (LongListIterator i = added.iterator(); i.hasNext(); ) {
      long item = i.nextValue();
      int index = workCopy.binarySearch(item);
      if (index >= 0) LogHelper.error("already added", item, index);
      else {
        index = -index - 1;
        workCopy.insert(index, item);
        fireInsert(index, 1);
      }
    }
  }

  private static IntList processUpdate(LongArray workCopy, LongList updated) {
    IntArray updatedIdxs = new IntArray(updated.size());
    for (LongListIterator i = updated.iterator(); i.hasNext(); ) {
      long item = i.nextValue();
      int index = workCopy.indexOf(item);
      if (index < 0) LogHelper.error("Missing", item);
      else updatedIdxs.add(index);
    }
    return updatedIdxs;
  }

  private static boolean checkEqualLists(LongList list1, LongArray list2) {
    int size1 = list1.size();
    int size2 = list2.size();
    LogHelper.assertError(size1 == size2, size1, "!=", size2);
    for (int i = 0; i < Math.min(size1, size2); i++) {
      long item1 = list1.get(i);
      long item2 = list2.get(i);
      if (item1 != item2) {
        LogHelper.error("not equal at", i, item1, item2);
        break;
      }
    }
    return true;
  }

  public static interface ItemWrapperFactory<T> {
    @ThreadAWT
    T getForItem(ImageSlice slice, long item);

    @ThreadAWT
    void afterChange(LongList removed, LongList changed, LongList added);
  }
}
