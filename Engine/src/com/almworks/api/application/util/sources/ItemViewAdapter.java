package com.almworks.api.application.util.sources;

import com.almworks.api.application.ItemsCollector;
import com.almworks.integers.*;
import com.almworks.items.api.*;
import com.almworks.util.Getter;
import com.almworks.util.L;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.progress.Progress;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedInt;

public class ItemViewAdapter extends AbstractItemSource {
  private final TypedKey<ScalarModel<Long>> REQUIRED_CN_MODEL_KEY = key(ScalarModel.class);

  private final DBFilter myItemFilter;
  private final Getter<Integer> myTotalCount;
  @Nullable
  private final BoolExpr<DP> mySubFilter;

  private BasicScalarModel<LongList> myResultModel;
  private LongArray myResultModelValue;

  private ItemViewAdapter(@NotNull DBFilter view, BoolExpr<DP> subFilter, Getter<Integer> totalCount, BasicScalarModel<LongList> resultModel) {
    super(ItemViewAdapter.class.getName());
    //noinspection ConstantConditions
    assert view != null;
    myItemFilter = view;
    mySubFilter = subFilter;
    myTotalCount = totalCount;
    myResultModel = resultModel;
    myResultModelValue = myResultModel == null ? null : new LongArray();
  }

  public void stop(ItemsCollector collector) {
    Detach detach = collector.getValue(DETACH);
    if (detach == null) {
      // ?
      Log.warn("null detach", new Throwable());
      return;
    }
    detach.detach();
    collector.putValue(DETACH, null);
  }

  private static class ReloadOp {
    public SynchronizedInt count = new SynchronizedInt(0);
    public String activity = L.progress(Local.parse("Loading from DB"));
    public Progress progress = new Progress("iva-loading");
    public Integer totalCount;
    public float step;
  }

  public void reload(final ItemsCollector collector) {
    final DetachComposite detach = new DetachComposite(true);
    collector.putValue(DETACH, detach);   // todo remove

    final ReloadOp op = new ReloadOp();
    setProgress(collector, op.progress);

    op.totalCount = getTotalCount();
    op.step = (op.totalCount == null || op.totalCount < 1) ? 0.0001F : 1F / op.totalCount;

    final Synchronized<Long> currentCN = new Synchronized<Long>(null);
    final ScalarModel<Long> requiredCN = collector.getValue(REQUIRED_CN_MODEL_KEY);
    if(requiredCN != null) {
      requiredCN.getEventSource().addStraightListener(new ScalarModel.Adapter<Long>() {
        public void onScalarChanged(ScalarModelEvent<Long> event) {
          maybeDone(currentCN.get(), requiredCN, collector);
        }
      });
    }

    myItemFilter.liveQuery(detach, new DBLiveQuery.Listener() {
      volatile boolean loaded = false;

      @Override
      public void onICNPassed(long icn) {
        currentCN.set(icn);
        maybeDone(icn, requiredCN, collector);
      }

      public void onDatabaseChanged(DBEvent event, final DBReader reader) {
        if (isStopped(collector)) {
          return;
        }

        if(!loaded) {
          // initial load - tbd
          loaded = true;
          initialLoad(event, reader, collector, op);
        } else {
          subsequentEvent(event, reader, collector, op);
        }

        final long cn = reader.getTransactionIcn();
        currentCN.set(cn);
        maybeDone(cn, requiredCN, collector);
      }
    });
  }

  private void initialLoad(DBEvent event, DBReader reader, ItemsCollector collector, ReloadOp op) {
    assert event.getAddedSorted().isEmpty() : event;
    assert event.getRemovedSorted().isEmpty() : event;
    onPlusItems(event.getChangedSorted(), true, collector, op, reader);
    initialLoadComplete(op);
  }

  private void initialLoadComplete(ReloadOp op) {
    final BasicScalarModel<LongList> rModel;
    final LongArray rList;
    synchronized(this) {
      rModel = myResultModel;
      rList = myResultModelValue;
      myResultModel = null;
      myResultModelValue = null;
    }

    if(rModel != null && rList != null) {
      rModel.commitValue(null, rList);
    }

    op.progress.setProgress(0.95F, null); // Clear activity
  }

  private void subsequentEvent(DBEvent event, DBReader reader, ItemsCollector collector, ReloadOp op) {
    final LongList removed = event.getRemovedSorted();
    for(int i = 0; i < removed.size(); i++) {
      final long item = removed.get(i);
      // todo filter check?
      collector.removeItem(item);
    }

    // changed -- ignored?

    onPlusItems(event.getAddedSorted(), false, collector, op, reader);
  }

  private void onPlusItems(
    LongIterable items, final boolean useCount,
    final ItemsCollector collector, final ReloadOp op, final DBReader reader)
  {
    if(mySubFilter != null) {
      reader.query(mySubFilter).filterItems(items, new AbstractLongCollector() {
        public void add(long value) {
          // todo(IS): careful review - see javadocs for filterItems
          if(!onPlusItem(value, useCount, collector, op, reader)) {
            throw new DBOperationCancelledException();
          }
        }
      });
    } else {
      for(final LongIterator i = items.iterator(); i.hasNext();) {
        onPlusItem(i.nextValue(), useCount, collector, op, reader);
      }
    }
  }

  private boolean onPlusItem(long item, boolean useCount, ItemsCollector collector, ReloadOp op, DBReader reader) {
    if(isStopped(collector)) {
      return false;
    }

    collector.addItem(item, reader);

    if(myResultModelValue != null) {
      synchronized(this) {
        final LongArray resultModelValue = myResultModelValue;
        if(resultModelValue != null) {
          resultModelValue.add(item);
        }
      }
    }

    if(useCount) {
      final int c = op.count.increment();
      final float p = Math.min(op.step * c, 1F);
      String a = op.activity;
      if(op.totalCount != null && op.totalCount >= c) {
        a += " (" + c + " of " + op.totalCount + ")";
      }
      op.progress.setProgress(p, a);
    }

    return !isStopped(collector);
  }

  private boolean isStopped(final ItemsCollector collector) {
    return collector.getValue(DETACH) == null;
  }

  private void maybeDone(Long currentWCN, ScalarModel<Long> requiredWCN, ItemsCollector collector) {
    if(currentWCN == null) {
      return;
    }

    if(requiredWCN != null) {
      final Long wcn = requiredWCN.getValue();
      if(wcn == null || (currentWCN <= wcn && wcn >= 0)) {
        return;
      }
    }

    getProgressDelegate(collector).setDone();
  }

  @Nullable
  private Integer getTotalCount() {
    return myTotalCount != null ? myTotalCount.get() : null;
  }

  public static ItemViewAdapter create(@NotNull DBFilter itemView) {
    return new ItemViewAdapter(itemView, null, null, null);
  }

  public static ItemViewAdapter create(@NotNull DBFilter itemView,
    BasicScalarModel<LongList> resultModel)
  {
    return new ItemViewAdapter(itemView, null, null, resultModel);
  }

  @NotNull
  public static ItemViewAdapter create(@NotNull DBFilter itemView, Getter<Integer> totalCount) {
    return new ItemViewAdapter(itemView, null, totalCount, null);
  }

  @NotNull
  public static ItemViewAdapter create(@NotNull DBFilter itemView, Getter<Integer> totalCount,
    BasicScalarModel<LongList> resultModel)
  {
    return new ItemViewAdapter(itemView, null, totalCount, resultModel);
  }

  public static ItemViewAdapter create(@NotNull DBFilter itemView, BoolExpr<DP> filter,
    Getter<Integer> totalCount)
  {
    return new ItemViewAdapter(itemView, filter, totalCount, null);
  }

  /**
   * Must be called before reload() ! E.g. from CompositeItemSource.reloadingPrepare()<br>
   * Negative wcn value means stop wait - proper value won't be provided because of some kind of problem.
   */
  public void setRequiredCNModel(ItemsCollector collector, ScalarModel<Long> requiredWCNModel) {
    collector.putValue(REQUIRED_CN_MODEL_KEY, requiredWCNModel);
  }
}
