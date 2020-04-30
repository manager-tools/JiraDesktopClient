package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.integers.LongList;
import com.almworks.integers.LongSetBuilder;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CollectingExtractionVisitor implements ExtractionVisitor {
  private List<SQLItemSelectBuilder> myBuilders;
  private LongSetBuilder myItems;
  private boolean myFinished;
  private LongList myItemList;

  @Override
  public void visitStarted(TransactionContext context) {
  }

  @Override
  public void visitSQL(TransactionContext context, SQLItemSelectBuilder sql) throws SQLiteException {
    if (myBuilders == null)
      myBuilders = Collections15.arrayList();
    myBuilders.add(sql);
  }

  @Override
  public void visitItems(TransactionContext context, LongIterable items) throws SQLiteException {
    if (myItems == null)
      myItems = new LongSetBuilder();
    myItems.addAll(items.iterator());
  }

  @Override
  public void visitFinished(TransactionContext context) throws SQLiteException {
    myFinished = true;
  }

  @Nullable
  public List<SQLItemSelectBuilder> getBuilders() {
    if (!myFinished) throw new IllegalStateException();
    return myBuilders;
  }

  @Nullable
  public LongList getItemsSorted() {
    if (!myFinished)
      throw new IllegalStateException();
    if (myItemList == null && myItems != null && !myItems.isEmpty()) {
      myItemList = myItems.commitToArray();
    }
    return myItemList;
  }
}
