package com.almworks.jira.provider3.app.sync;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntList;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.ReadTransaction;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.util.model.ScalarModel;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

class AdditionalDownload {
  private final DBFilter myView;
  private LongList myLocalResult;

  public AdditionalDownload(@NotNull DBFilter view, @Nullable LongList localResult) {
    myView = view;
    myLocalResult = localResult;
  }

  public Collection<String> loadLeftoverKeys(final Set<Integer> loadedIds, final ScalarModel<Boolean> cancelFlag) {
    final IntArray ids = IntArray.create(loadedIds);
    ids.sortUnique();
    return loadLeftover(cancelFlag, ids);
  }

  public List<String> loadLeftoverKeys(IntList loadedIds, ScalarModel<Boolean> cancelFlag) {
    IntArray ids = new IntArray(loadedIds);
    ids.sortUnique();
    return loadLeftover(cancelFlag, ids);
  }

  private List<String> loadLeftover(final ScalarModel<Boolean> cancelFlag, final IntArray ids) {
    return myView.getDatabase().readBackground(new ReadTransaction<List<String>>() {
      @Override
      public List<String> transaction(DBReader reader) throws DBOperationCancelledException {
        final Collection<String> result = Collections15.hashSet();
        LongList localResult = getLocalResult(reader);
        if (localResult == null) return null;
        for (LongIterator cursor : localResult) {
          if (cancelFlag.getValue() == Boolean.TRUE) throw new DBOperationCancelledException();
          final long item = cursor.value();
          final Integer id = Issue.ID.getValue(item, reader);
          final String key = Issue.KEY.getValue(item, reader);
          if(id != null && (ids.binarySearch(id) < 0) && key != null) {
            result.add(key);
          }
        }
        return Collections15.arrayList(result);
      }
    }).waitForCompletion();
  }

  private LongList getLocalResult(DBReader reader) {
    if (myLocalResult == null) myLocalResult = myView.query(reader).copyItemsSorted();
    return myLocalResult;
  }

  public int getLocalResultCount() {
    ensureHasLocalResult();
    return myLocalResult != null ? myLocalResult.size() : 0;
  }

  public void ensureHasLocalResult() {
    if (myLocalResult != null) return;
    myView.getDatabase().readBackground(new ReadTransaction<Integer>() {
      @Override
      public Integer transaction(DBReader reader) throws DBOperationCancelledException {
        return getLocalResult(reader).size();
      }
    }).waitForCompletion();
  }
}
