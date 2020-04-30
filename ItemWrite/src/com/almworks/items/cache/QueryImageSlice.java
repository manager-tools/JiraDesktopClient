package com.almworks.items.cache;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DP;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.LongSet;
import com.almworks.util.threads.Threads;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

/**
 * @author dyoma
 */
public class QueryImageSlice extends BaseImageSlice {
  private final TypedKey<LongList> ITEMS = TypedKey.create("items");
  private final BoolExpr<DP> myQuery;
  private volatile LongList myActual = LongList.EMPTY;

  QueryImageSlice(DBImage image, BoolExpr<DP> query) {
    super(image);
    myQuery = query;
  }

  @Override
  public LongList getActualItems() {
    return myActual;
  }

  @Override
  public boolean hasAllValues(long item) {
    return myActual.contains(item);
  }

  @Override
  public int getActualCount() {
    return myActual.size();
  }

  @Override
  public long getItem(int index) {
    return myActual.get(index);
  }

  @Override
  boolean doStartSlice(Lifespan life) {
    if (!super.doStartSlice(life)) return false;
    requestUpdate();
    return true;
  }

  @Override
  protected LongList earth() {
    Threads.assertAWTThread();
    LogHelper.assertError(!isRunning(), "burying alive", this);
    LongList current = myActual;
    myActual = LongList.EMPTY;
    return current;
  }

  @Override
  void updateItemSet(CacheUpdate update) {
    LongArray newItems = update.getReader().query(myQuery).copyItemsSorted();
    update.putData(ITEMS, newItems);
    setItemSet(update, newItems);
  }

  @Override
  void applyItemSetUpdate(CacheUpdate update, DataChange change) {
    Threads.assertAWTThread();
    if (!isRunning()) return;
    LongList newItems = update.getData(ITEMS);
    LongSet removed = LongSet.setDifference(myActual, newItems);
    LongSet added = LongSet.setDifference(newItems, myActual);
    myActual = newItems;
    change.setItemChange(this, added, removed);
  }

  @Override
  public String toString() {
    return "QIS " + myQuery;
  }
}
