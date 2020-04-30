package com.almworks.items.impl.sqlite;

import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.almworks.util.Collections15.*;

/**
 * A simple filter manager with all constraints having the same priority.
 */
class SimpleOldFilterManager implements OldFilterManager, FilterMaster {
  private final QueryProcessor myQueryProcessor;
  private final DatabaseContext myDatabaseContext;

  private final Map<BoolExpr<DP>, FilteringItemSource> mySourceMap = hashMap();
  private final Set<BoolExpr<DP>> myUpdateSet = hashSet();
  private final Mapper myMapper = new Mapper();
  private final Object myLock = mySourceMap;

  public SimpleOldFilterManager(QueryProcessor queryProcessor, DatabaseContext databaseContext) {
    myQueryProcessor = queryProcessor;
    myDatabaseContext = databaseContext;
  }

  @NotNull
  public FilteringItemSource getFilter(BoolExpr<DP> constraint) {
    return mapFilter(constraint, true);
  }

  private FilteringItemSource mapFilter(BoolExpr<DP> constraint, boolean markForUpdate) {
    BoolExpr<DP> c = Reductions.simplify(constraint);
    FilteringItemSource source;
    synchronized (myLock) {
      // todo more equality checks? DNF?
      source = mySourceMap.get(c);
      if (source == null) {
        source = FilteringItemSource.create(myQueryProcessor, this, c, myDatabaseContext);
        mySourceMap.put(c, source);
        if (markForUpdate) {
          myUpdateSet.add(c);
        }
      }
    }
    return source;
  }

  public void updateFilter(FilteringItemSource source, TransactionContext context) {
    update(context);
  }

  void update(TransactionContext context) {
    List<BoolExpr<DP>> update;
    synchronized (myLock) {
      update = myUpdateSet.isEmpty() ? null : arrayList(myUpdateSet);
      myUpdateSet.clear();
    }
    if (update != null) {
      for (BoolExpr<DP> expr : update) {
        FilteringItemSource fis = myMapper.map(expr, context);
        assert fis != null;
      }
    }
  }

  Mapper getMapper() {
    return myMapper;
  }

  class Mapper extends BoolSubjectTreeMapper<DP, FilteringItemSource> {
    protected FilteringItemSource create(BoolExpr<DP> expr) {
      return mapFilter(expr, false);
    }

    protected void setParent(FilteringItemSource source, FilteringItemSource parent, BoolExpr<DP> workingExpr, TransactionContext context) {
      source.setWorkingFilter(parent, workingExpr, context);
    }

    @Override
    protected MapOp createOp() {
      return new DBMapOp();
    }

    @Nullable
    @Override
    protected Boolean isBetterEntry(Entry entry, List<BoolExpr<DP>> conj, MapOp mapOp) {
      Boolean r = super.isBetterEntry(entry, conj, mapOp);
      if (r != null)
        return r;

      DBMapOp op = (DBMapOp) mapOp;

      int count = entry.getSubject().getCountable().getCount();
      if (count < 0) {
        return null;
      }

      return count < op.bestCount;
    }

    @Override
    protected void setBestEntry(Entry entry, MapOp mapOp) {
      super.setBestEntry(entry, mapOp);
      int count = entry.getSubject().getCountable().getCount();
      ((DBMapOp) mapOp).bestCount = count < 0 ? Integer.MAX_VALUE : count;
    }

    private class DBMapOp extends MapOp {
      private int bestCount = Integer.MAX_VALUE;
    }
  }
}
