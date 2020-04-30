package com.almworks.items.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.edit.SyncFixture;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.TimeGuard;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Lifespan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ImageFixture extends SyncFixture {
  private static final DBNamespace NS = DBNamespace.moduleNs("tests");
  protected static final DBAttribute<Integer> ID = NS.integer("id");
  protected static final DBAttribute<String> VALUE = NS.string("value");
  protected static final DBItemType TYPE = NS.type();
  protected static final CollectionsCompare CHECK = new CollectionsCompare();
  protected static final AttributeLoader<String> VALUE_LOADER = AttributeLoader.create(VALUE);
  protected DBImage myImage;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myImage = new DBImage(db);
  }

  @Override
  protected void tearDown() throws Exception {
    myImage.stop();
    super.tearDown();
  }

  protected long setValue(final int id, final String value) {
    return db.writeForeground(new WriteTransaction<Long>() {
      @Override
      public Long transaction(DBWriter writer) throws DBOperationCancelledException {
        LongArray items = writer.query(DPEquals.create(ID, id).and(DPEqualsIdentified.create(DBAttribute.TYPE, TYPE))).copyItemsSorted();
        long item;
        if (items.isEmpty()) {
          item = writer.nextItem();
          writer.setValue(item, DBAttribute.TYPE, writer.materialize(TYPE));
          writer.setValue(item, ID, id);
        } else item = items.get(0);
        writer.setValue(item, VALUE, value);
        return item;
      }
    }).waitForCompletion();
  }

  protected static class Supervisor implements ImageSlice.Listener {
    private final ImageSlice mySlice;
    private final List<ImageTests.EventCopy> myEventLog = Collections15.arrayList();
    private int myClearedEventCount = 0;

    public Supervisor(ImageSlice slice) {
      mySlice = slice;
    }

    @Override
    public void onChange(ImageSliceEvent event) {
      synchronized (myEventLog) {
        myEventLog.add(EventCopy.create(event));
      }
    }

    public static Supervisor create(ImageSlice slice) {
      Supervisor supervisor = new Supervisor(slice);
      slice.addListener(Lifespan.FOREVER, supervisor);
      return supervisor;
    }

    public void waitForItem(final long item) throws InterruptedException {
      TimeGuard.waitFor(new Procedure<TimeGuard<Object>>() {
        @Override
        public void invoke(TimeGuard<Object> arg) {
          if (mySlice.getActualItems().contains(item))
            arg.setResult(null);
        }
      });
    }

    public <T> T waitForData(final long item, DBAttribute<T> attribute) throws InterruptedException {
      final AttributeLoader<T> loader = AttributeLoader.create(attribute);
      return TimeGuard.waitFor(new Procedure<TimeGuard<T>>() {
        @Override
        public void invoke(TimeGuard<T> arg) {
          if (mySlice.hasValue(item, loader)) arg.setResult(mySlice.getValue(item, loader));
        }
      });
    }

    public EventCopy waitForEvent(final int count) throws InterruptedException {
      return TimeGuard.waitFor(new Procedure<TimeGuard<ImageTests.EventCopy>>() {
        @Override
        public void invoke(TimeGuard<ImageTests.EventCopy> arg) {
          if (getEventCount() >= count) {
            arg.setResult(getLastEvent());
          }
        }
      });
    }

    private EventCopy getLastEvent() {
      synchronized (myEventLog) {
        return myEventLog.isEmpty() ? null : myEventLog.get(myEventLog.size() - 1);
      }
    }

    public int getEventCount() {
      synchronized (myEventLog) {
        return myEventLog.size() + myClearedEventCount;
      }
    }

    public void clearEvents() {
      synchronized (myEventLog) {
        myClearedEventCount += myEventLog.size();
        myEventLog.clear();
      }
    }
  }

  protected static class EventCopy {
    private final long myICN;
    private final LongList myAdded;
    private final LongList myRemoved;
    private final LongList myChange;
    private final Map<DataLoader<?>, LongSet> myChangedData;

    private EventCopy(long ICN, LongList added, LongList removed, LongList change,
      Map<DataLoader<?>, LongSet> changedData) {
      myICN = ICN;
      myAdded = added;
      myRemoved = removed;
      myChange = change;
      myChangedData = changedData;
    }

    public long getICN() {
      return myICN;
    }

    public static EventCopy create(ImageSliceEvent event) {
      Threads.assertAWTThread();
      HashMap<DataLoader<?>,LongSet> changedData = Collections15.hashMap();
      ImageSlice slice = event.getSlice();
      LongList changedItems = event.getChanged();
      for (DataLoader<?> loader : slice.getActualData()) {
        LongSet items = new LongSet();
        for (int i = 0; i < changedItems.size(); i++) {
          long item = changedItems.get(i);
          if (event.isChanged(item, loader)) items.add(item);
        }
        if (!items.isEmpty()) changedData.put(loader, items);
      }
      return new EventCopy(event.getIcn(), event.getAdded(), event.getRemoved(), changedItems, changedData);
    }

    public long[] getAdded() {
      return myAdded.toNativeArray();
    }

    public long[] getRemoved() {
      return myRemoved.toNativeArray();
    }

    public long[] getChange() {
      return myChange.toNativeArray();
    }

    public boolean isChanged(long item, AttributeLoader<String> loader) {
      LongSet items = myChangedData.get(loader);
      return items != null && items.contains(item);
    }
  }
}
