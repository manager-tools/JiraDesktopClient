package com.almworks.api.explorer;

import com.almworks.api.application.ItemsCollector;
import com.almworks.api.application.util.sources.AbstractItemSource;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.util.progress.Progress;
import org.almworks.util.TypedKey;

import java.util.Collection;
import java.util.Iterator;

public abstract class SimpleCollection extends AbstractItemSource {
  private final TypedKey<Boolean> STOP_KEY = key(Boolean.class);
  private final Database myDb;

  protected SimpleCollection(Database db) {
    super(SimpleCollection.class.getName());
    myDb = db;
  }

  public static SimpleCollection create(Database db, final Collection<Long> items) {
    return new SimpleCollection(db) {
      @Override
      protected Iterator<Long> getItems() {
        return items.iterator();
      }
    };
  }

  public static SimpleCollection create(Database db, LongList items) {
    return create(db, items.toList());
  }

  public void stop(ItemsCollector collector) {
    collector.putValue(STOP_KEY, Boolean.TRUE);
  }

  public void reload(final ItemsCollector collector) {
    Boolean prevValue = collector.putValue(STOP_KEY, Boolean.FALSE);
    assert prevValue == null : prevValue;

    getProgressDelegate(collector).delegate(new Progress(this.toString()));

    myDb.readForeground(new ReadTransaction<Object>() {
      public Object transaction(DBReader reader) {
        Iterator<Long> items = getItems();
        while (items.hasNext()) {
          Long item = items.next();
          collector.addItem(item, reader);
          if (collector.getValue(STOP_KEY).booleanValue())
            return null;
        }
        getProgressDelegate(collector).setDone();
        return null;
      }
    });
  }

  protected abstract Iterator<Long> getItems();
}
