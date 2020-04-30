package com.almworks.items.impl;

import com.almworks.items.api.*;
import com.almworks.items.impl.sqlite.DBReadJob;
import com.almworks.items.impl.sqlite.DatabaseJob;
import com.almworks.items.impl.sqlite.DatabaseManager;
import com.almworks.items.impl.sqlite.QueryProcessor;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class LiveQueryManager implements QueryProcessor.Client {
  private final Map<BoolExpr<DP>, DBLiveQueryImpl> myFilters = Collections15.hashMap();
  // for fast access
  private final List<DBLiveQueryImpl> myFilterList = new CopyOnWriteArrayList<DBLiveQueryImpl>();
  private final ReadTransaction<Void> myTransaction = new ReadTransaction<Void>() {
    @Override
    public Void transaction(DBReader reader) throws DBOperationCancelledException {
      update(reader);
      return null;
    }
  };

  private final Object myLock = myFilters;
  private DatabaseManager myDatabase;
  private long myIcn = 0;
  @Nullable
  private ResolutionSubscription myCurrentResolutionSubscription = null;

  public void start(DatabaseManager database) {
    synchronized (myLock) {
      if (myDatabase != null) {
        assert false : this;
        return;
      }
      myDatabase = database;
    }
    QueryProcessor processor = database.getQueryProcessor();
    processor.attach(Lifespan.FOREVER, this);
  }

  public DBLiveQuery attach(BoolExpr<DP> expr, Lifespan lifespan, DBLiveQuery.Listener listener) {
    if (lifespan.isEnded())
      return DBLiveQuery.DEAF;
    BoolExpr<DP> dnf = Reductions.toDnf(expr);
    DatabaseManager database;
    DBLiveQueryImpl filter;
    synchronized (myLock) {
      database = myDatabase;
      filter = myFilters.get(dnf);
      if (filter == null) {
        filter = new DBLiveQueryImpl(dnf);
        myFilters.put(dnf, filter);
        myFilterList.add(filter);
      }
    }
    filter.addListener(lifespan, listener);
    database.read(DBPriority.FOREGROUND, createHandle());
    return filter;
  }

  private void update(DBReader reader) {
// todo avoid unnecessary jobs
    cycleResolutionSubscription();
    long icn = reader.getTransactionIcn() - 1;
    if (myIcn == 0) {
      firstUpdate(reader);
    } else {
      subsequentUpdate(reader, icn);
    }
    myIcn = icn;
  }

  private void cycleResolutionSubscription() {
    if (myCurrentResolutionSubscription != null) {
      myCurrentResolutionSubscription.dispose();
    }
    myCurrentResolutionSubscription = new ResolutionSubscription();
  }

  private void firstUpdate(DBReader reader) {
    for (DBLiveQueryImpl query : myFilterList) {
      long start = System.currentTimeMillis();
      Log.debug("Live query first update started " + query);
      query.independentUpdate(reader, myCurrentResolutionSubscription);
      long duration = System.currentTimeMillis() - start;
      Log.debug("FirstUpdate: " +duration + "ms " + query);
    }
  }

  private void subsequentUpdate(DBReader reader, long curIcn) {
    DBEvent event = curIcn == myIcn ? DBEvent.EMPTY : null;
    for (DBLiveQueryImpl query : myFilterList) {
      long start = System.currentTimeMillis();
      if (query.getLastProcessedTransactionIcn() == myIcn) {
        if (event == null) {
          event = DBEvent.create(reader.getChangedItemsSorted(myIcn));
        }
        query.guidedUpdate(myIcn, curIcn, event, null, reader, myCurrentResolutionSubscription);
      } else {
        query.independentUpdate(reader, myCurrentResolutionSubscription);
      }
      long duration = System.currentTimeMillis() - start;
      if (duration > 10) Log.debug("Live query subsequent update done " +duration + "ms " + query);
    }
  }

  @Override
  public DatabaseJob createJob() {
    return new DBReadJob(createHandle());
  }

  private ReadHandle<Void> createHandle() {
    return new ReadHandle<Void>(myTransaction);
  }

  class ResolutionSubscription implements DP.ResolutionSubscription {
    private final Lifecycle myLife = new Lifecycle();

    @Override
    public Lifespan getLife() {
      return myLife.lifespan();
    }

    public void dispose() {
      myLife.dispose();
    }

    /**
     * Creates a resolution subscription with lifespan that is the intersection of this subscription's and the given one.
     * */
    public DP.ResolutionSubscription createIntersection(Lifespan life) {
      final Lifespan intersection = intersect(myLife, life);
      return new DP.ResolutionSubscription() {
        @Override
        public Lifespan getLife() {
          return intersection;
        }

        @Override
        public void onChange() {
          ResolutionSubscription.this.onChange();
        }
      };
    }

    @Override
    public void onChange() {
      DatabaseManager db;
      synchronized (myLock) {
        db = myDatabase;
        if (db == null) return;
      }
      db.read(DBPriority.FOREGROUND, createHandle());
    }
  }

  private static Lifespan intersect(Lifecycle cycle, Lifespan span) {
    if (span.isEnded() || cycle.isDisposed()) return Lifespan.NEVER;
    Lifecycle intersection = new Lifecycle();
    span.add(intersection.getDisposeDetach());
    cycle.lifespan().add(intersection.getDisposeDetach());
    return intersection.lifespan();
  }
}
