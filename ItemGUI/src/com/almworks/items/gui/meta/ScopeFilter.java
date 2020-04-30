package com.almworks.items.gui.meta;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.util.model.ValueModel;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ScopeFilter {

  public static ValueModel<? extends ScopeFilter> refersAny(Lifespan life, Database db, ItemAttribute attribute, final ItemReference ... objects) {
    ItemIdScope listener = new ItemIdScope(db, attribute) {
      @Override
      protected boolean collectItems(DBReader reader, LongArray items) {
        boolean all = true;
        for (ItemReference object : objects) {
          long item = object.findItem(reader);
          if (item <= 0)
            all = false;
          items.add(item);
        }
        return all;
      }
    };
    return listener.start(life);
  }

  public abstract Collection<? extends DataLoader<?>> getLoaders();

  public abstract boolean isAccepted(ImageSlice slice, long item);

  private static abstract class ItemIdScope implements ReadTransaction<Object>, DBListener {
    private final DataLoader<Long> myLoader;
    private final ValueModel<MyScopeFilter> myModel = new ValueModel<MyScopeFilter>();
    private final Database myDb;
    private final DetachComposite myLife = new DetachComposite();
    private final AtomicLong myLastICN = new AtomicLong(-1);

    protected ItemIdScope(Database db, ItemAttribute attribute) {
      myLoader = attribute;
      myDb = db;
    }

    protected abstract boolean collectItems(DBReader reader, LongArray items);

    public ValueModel<? extends ScopeFilter> start(Lifespan life) {
      myDb.readBackground(this);
      myDb.addListener(myLife, this);
      life.add(myLife);
      return myModel;
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      updateModel(reader);
      return null;
    }

    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      updateModel(reader);
    }

    private void updateModel(DBReader reader) {
      long icn = reader.getTransactionIcn();
      while (true) {
        long last = myLastICN.get();
        if (last >= icn) return;
        if (myLastICN.compareAndSet(last, icn)) break;
      }
      LongArray items = new LongArray();
      if (collectItems(reader, items)) myLife.detach();
      items.sortUnique();
      MyScopeFilter current = myModel.getValue();
      boolean empty = items.isEmpty();
      if ((current == null && empty) || (current != null && current.myExpectedValues.equals(items))) return;
      myModel.setValue(empty ? null : new MyScopeFilter(items));
    }

    private class MyScopeFilter extends ScopeFilter {
      private final LongList myExpectedValues;

      public MyScopeFilter(LongList expectedValues) {
        myExpectedValues = expectedValues;
      }

      @Override
      public Collection<? extends DataLoader<?>> getLoaders() {
        return Collections.singleton(myLoader);
      }

      @Override
      public boolean isAccepted(ImageSlice slice, long item) {
        Long actual = slice.getValue(item, myLoader);
        return actual != null && myExpectedValues.contains(actual);
      }
    }
  }
}
