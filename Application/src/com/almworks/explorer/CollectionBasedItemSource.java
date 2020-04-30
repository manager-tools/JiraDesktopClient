package com.almworks.explorer;

import com.almworks.api.application.ItemSource;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.ItemsCollector;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CollectionBasedItemSource<T extends ItemWrapper> implements ItemSource {
  private final Progress myProgress = new Progress();
  private final Database myDb;
  private final LongList myItems;

  private boolean myFirstRun = true;

  private CollectionBasedItemSource(Database db, LongList items) {
    myDb = db;
    myItems = items;
  }

  @ThreadAWT
  public void stop(@NotNull ItemsCollector collector) {
    //ignore
  }

  @ThreadAWT
  public void reload(@NotNull final ItemsCollector collector) {
    if (myFirstRun) {
      myDb.readForeground(new ReadTransaction<Void>() {
        @Override
        public Void transaction(DBReader reader) throws DBOperationCancelledException {
          myProgress.setStarted();
          for (LongIterator cursor : myItems) collector.addItem(cursor.value(), reader);
          myProgress.setDone();
          return null;
        }
      });
      myFirstRun = false;
    }
  }

  public ProgressSource getProgress(ItemsCollector collector) {
    return myProgress;
  }

  public static CollectionBasedItemSource create(ItemWrapper wrapper) {
    Database db = wrapper.services().getEngine().getDatabase();
    return new CollectionBasedItemSource(db, LongArray.create(wrapper.getItem()));
  }

  public static ItemSource create(Collection<? extends ItemWrapper> itemWrappers) {
    if (itemWrappers == null || itemWrappers.isEmpty()) return EMPTY;
    Database db = null;
    LongArray items = new LongArray();
    for (ItemWrapper itemWrapper : itemWrappers) {
      if (db == null) db = itemWrapper.services().getEngine().getDatabase();
      items.add(itemWrapper.getItem());
    }
    if (db == null) return EMPTY;
    return new CollectionBasedItemSource(db, items);
  }
}
