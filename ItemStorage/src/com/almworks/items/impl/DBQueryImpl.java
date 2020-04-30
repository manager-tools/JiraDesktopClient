package com.almworks.items.impl;

import com.almworks.integers.*;
import com.almworks.items.api.*;
import com.almworks.items.impl.dbadapter.ItemVisitor;
import com.almworks.items.impl.sqlite.ExtractionProcessor;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.LongObjFunction2;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

class DBQueryImpl implements DBQuery {
  private final DBReaderImpl myReader;
  private final BoolExpr<DP> myFilter;

  public DBQueryImpl(DBReaderImpl reader, BoolExpr<DP> filter) {
    myReader = reader;
    myFilter = filter;
  }

  public DBReader getReader() {
    return myReader;
  }

  @Override
  public BoolExpr<DP> getExpr() {
    return myFilter;
  }

  public DBQuery query(BoolExpr<DP> expr) {
    return new DBQueryImpl(myReader, myFilter.and(expr));
  }

  public long count() {
    try {
      return extractor().count(context());
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  @Override
  public LongList distributionCount(DBAttribute<?>... groupAttributes) {
    if (groupAttributes == null || groupAttributes.length == 0) return LongList.EMPTY;
    try {
      return extractor().distributionCount(context(), groupAttributes);
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  @NotNull
  public LongArray copyItemsSorted() {
    LongSetBuilder builder = copyItems(new LongSetBuilder());
    return builder.commitToArray();
  }

  @Override
  public <C extends LongCollector> C copyItems(C collector) {
    try {
      if (collector == null)
        throw new NullPointerException();
      extractor().visitItems(context(), new ItemVisitor.Collector(collector));
      return collector;
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  @Override
  public <T> T fold(T seed, final LongObjFunction2<T> f) {
    T result = seed;
    LongArray items = copyItemsSorted();
    for (int i = 0; i < items.size(); i++) {
      result = f.invoke(items.get(i), result);
    }
    return result;
  }

  public boolean contains(final long item) {
    try {
      return extractor().checkItem(context(), item);
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  /**
   * @param result
   */
  public boolean filterItems(LongIterable items, LongCollector result) {
    if (items == null || !items.iterator().hasNext()) return true;
    try {
      extractor().filterItems(context(), items, result);
      return true;
    } catch (SQLiteException e) {
      throw new DBException(e);
    } catch (DBOperationCancelledException e) {
      return false;
    }
  }

  @Override
  public LongArray filterItemsSorted(LongIterable items) {
    LongSetBuilder builder = new LongSetBuilder();
    filterItems(items, builder);
    return builder.commitToArray();
  }

  public long getItem() {
    try {
      ItemVisitor.Single v = new ItemVisitor.Single();
      extractor().visitItems(context(), v);
      return v.hasItem() ? v.getItem() : 0;
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  public <T> long getItemByKey(DBAttribute<T> attribute, T value) {
    if (attribute == null)
      throw new NullPointerException();
    if (value == null)
      return 0;
    MapCacheKey key = new MapCacheKey(myFilter, attribute);
    try {
      TransactionContext context = context();
      Map contextMap = context.getSessionContext().getSessionCache();
      ValueMapCache<T> cache = (ValueMapCache<T>) contextMap.get(key);
      if (cache == null) {
        cache = new ValueMapCache<T>(context.getDatabaseContext(), myFilter, attribute);
        contextMap.put(key, cache);
      }
      return cache.getItem(value, context);
    } catch (SQLiteException e) {
      throw new DBException(e);
    }
  }

  private ExtractionProcessor extractor() {
    // todo call once?
    return ExtractionProcessor.create(myFilter, context());
  }

  private TransactionContext context() {
    return myReader.getContext();
  }

  private static class MapCacheKey {
    public BoolExpr<DP> expr;
    public DBAttribute<?> attribute;

    private MapCacheKey(BoolExpr<DP> expr, DBAttribute<?> attribute) {
      this.expr = expr;
      this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;

      MapCacheKey that = (MapCacheKey) o;

      if (!attribute.equals(that.attribute))
        return false;
      if (!expr.equals(that.expr))
        return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = expr.hashCode();
      result = 31 * result + attribute.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "MapCache[" + expr + "," + attribute + ']';
    }
  }
}
