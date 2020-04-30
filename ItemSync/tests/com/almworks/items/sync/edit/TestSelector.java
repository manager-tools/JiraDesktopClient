package com.almworks.items.sync.edit;

import com.almworks.items.api.DBReader;
import com.almworks.items.sync.AutoMergeData;
import com.almworks.items.sync.ItemAutoMerge;
import com.almworks.items.sync.ModifiableDiff;
import com.almworks.util.collections.LongSet;
import junit.framework.Assert;

import java.util.concurrent.atomic.AtomicReference;

public class TestSelector implements ItemAutoMerge.Selector, ItemAutoMerge {
  private final LongSet myResolved = new LongSet();
  private final LongSet myPreprocessed = new LongSet();
  private final AtomicReference<ItemAutoMerge> myDelegate = new AtomicReference<ItemAutoMerge>(null);

  @Override
  public ItemAutoMerge getOperations(DBReader reader, long item) {
    return this;
  }

  public void waitProcessed(long item) throws InterruptedException {
    long start = System.currentTimeMillis();
    int iteration = 0;
    while (true) {
      iteration++;
      synchronized (myResolved) {
        if (iteration > 4 && System.currentTimeMillis() - start > 300) Assert.fail("Timeout");
        if (myPreprocessed.contains(item)) return;
        else myResolved.wait(50);
      }
    }
  }

  public void checkNotProcessed(long item) {
    synchronized (myResolved) {
      Assert.assertFalse(myPreprocessed.contains(item));
    }
  }

  @Override
  public void preProcess(ModifiableDiff local) {
    ItemAutoMerge operations = myDelegate.get();
    if (operations != null) operations.preProcess(local);
    synchronized (myResolved) {
      myPreprocessed.add(local.getItem());
    }
  }

  @Override
  public void resolve(AutoMergeData data) {
    ItemAutoMerge operations = myDelegate.get();
    if (operations != null) operations.resolve(data);
    synchronized (myResolved) {
      myResolved.add(data.getItem());
      myResolved.notifyAll();
    }
  }

  public void clear() {
    synchronized (myResolved) {
      myResolved.clear();
      myPreprocessed.clear();
    }
  }

  public void checkProcessed(long item) {
    synchronized (myResolved) {
      Assert.assertTrue(myPreprocessed.contains(item));
    }
  }

  public void delegateAll(ItemAutoMerge operations) {
    myDelegate.set(operations);
  }
}
