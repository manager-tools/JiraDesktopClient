package com.almworks.items.cache.util;

import com.almworks.integers.LongArray;
import com.almworks.items.cache.ImageFixture;
import com.almworks.items.cache.ManualImageSlice;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Computable;
import org.almworks.util.detach.Lifespan;

import java.util.Comparator;

public class ImageListModelTests extends ImageFixture {
  public void test() throws InterruptedException {
    ManualImageSlice slice = myImage.manualSlice(Lifespan.FOREVER);
    Supervisor supervisor = Supervisor.create(slice);
    AListModel<CachedItem> model = createModel(slice);
    long item1 = setValue(1, "b");
    long item2 = setValue(2, "c");
    slice.addData(VALUE_LOADER);
    slice.addItems(item1, item2);
    supervisor.waitForData(item1, VALUE);
    checkModel(model, item1, item2);
    int count = supervisor.getEventCount();

    setValue(2, "a");
    supervisor.waitForEvent(count + 1);
    checkModel(model, item2, item1);

    long item3 = setValue(3, "e");
    slice.addItems(item3);
    supervisor.waitForData(item3, VALUE);
    checkModel(model, item2, item1, item3);
    count = supervisor.getEventCount();

    slice.removeItems(item2);
    supervisor.waitForEvent(count + 1);
    checkModel(model, item1, item3);
  }

  private AListModel<CachedItem> createModel(final ManualImageSlice slice) {
    return ThreadGate.AWT_IMMEDIATE.compute(new Computable<AListModel<CachedItem>>() {
      @Override
      public AListModel<CachedItem> compute() {
        return SortedListDecorator.create(Lifespan.FOREVER, CachedItemImpl.createModel(Lifespan.FOREVER, slice),
          new Comparator<CachedItem>() {
            @Override
            public int compare(CachedItem o1, CachedItem o2) {
              String v1 = o1.getValue(VALUE_LOADER);
              String v2 = o2.getValue(VALUE_LOADER);
              return v1.compareTo(v2);
            }
          });
      }
    });
  }

  private void checkModel(final AListModel<CachedItem> model, final long ... items) {
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      @Override
      public void run() {
        LongArray actual = new LongArray();
        for (int i = 0; i < model.getSize(); i++) {
          long item = model.getAt(i).getItem();
          assertFalse(actual.contains(item));
          actual.add(item);
        }
        CHECK.unordered(actual.toNativeArray(), items);
      }
    });
  }
}
