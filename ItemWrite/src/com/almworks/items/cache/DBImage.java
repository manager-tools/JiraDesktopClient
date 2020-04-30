package com.almworks.items.cache;

import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.CollectionRemove;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Procedure;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author dyoma
 */
public class DBImage implements Startable {
  public static final Role<DBImage> ROLE = Role.role(DBImage.class);

  private final Database myDB;
  private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock();
  private final CopyOnWriteArrayList<BaseImageSlice> mySlices = new CopyOnWriteArrayList<BaseImageSlice>();
  private final DataTable myData = new DataTable();
  private final DetachComposite myLife = new DetachComposite();
  private final UpdaterRequest myDBRequest = new UpdaterRequest();
  private final List<CoherentUpdate<?>> myCoherentUpdates = Collections15.arrayList();
  private final UserDataHolder myUserData = new UserDataHolder();
  private static final int STATE_INITIAL = 0;
  private static final int STATE_STARTUP = 1;
  private static final int STATE_RUNNING = 2;
  private static final int STATE_STOPPED = 3;

  private final AtomicInteger myRunning = new AtomicInteger(STATE_INITIAL);
  private long myICN = -1;

  public DBImage(Database db) {
    myDB = db;
  }

  Lock getReadLock() {
    return myLock.readLock();
  }

  public Database getDatabase() {
    return myDB;
  }

  public UserDataHolder getUserData() {
    return myUserData;
  }

  public boolean hasValue(long item, DataLoader<?> data) {
    Lock lock = getReadLock();
    lock.lock();
    try {
      return myData.hasValue(item, data);
    } finally {
      lock.unlock();
    }
  }

  public <T> T getValue(long item, DBAttribute<T> attribute) {
    return getValue(item, AttributeLoader.create(attribute));
  }

  public <T> T getValue(long item, DataLoader<T> data) {
    Lock lock = getReadLock();
    lock.lock();
    try {
      return myData.getValue(item, data);
    } finally {
      lock.unlock();
    }
  }

  public ManualImageSlice manualSlice(Lifespan life) {
    ManualImageSlice slice = createManualSlice();
    slice.ensureStarted(life);
    return slice;
  }

  public QueryImageSlice querySlice(Lifespan life, BoolExpr<DP> query) {
    QueryImageSlice slice = createQuerySlice(query);
    slice.ensureStarted(life);
    return slice;
  }

  public QueryImageSlice createQuerySlice(BoolExpr<DP> query) {
    return new QueryImageSlice(this, query);
  }

  public ManualImageSlice createManualSlice() {
    return new ManualImageSlice(this);
  }


  public long getICN() {
    Lock lock = getReadLock();
    lock.lock();
    try {
      return myICN;
    } finally {
      lock.unlock();
    }
  }

  public void addCoherentUpdate(Lifespan life, CoherentUpdate<?> update) {
    if (life.isEnded()) return;
    myCoherentUpdates.add(update);
    life.add(new CollectionRemove<CoherentUpdate<?>>(myCoherentUpdates, update, myCoherentUpdates));
  }

  @Override
  public void start() {
    ensureStarted();
  }

  public void stop() {
    myDB.getUserData().replace(ROLE, this, null);
    while (true) {
      int state = myRunning.get();
      switch (state) {
      case STATE_INITIAL: if (myRunning.compareAndSet(STATE_INITIAL, STATE_STOPPED)) return;
      case STATE_STARTUP:
      case STATE_RUNNING:
        myLife.detach();
        myRunning.set(STATE_STOPPED);
        return;
      case STATE_STOPPED: return;
      default: LogHelper.error("unknown state", state);
      }
    }
  }

  private boolean isRunning() {
    return myRunning.get() == STATE_RUNNING;
  }

  void requestUpdate() {
    myDBRequest.request();
  }

  private boolean ensureStarted() {
    while (true) {
      int state = myRunning.get();
      switch (state) {
      case STATE_INITIAL:
        if (myRunning.compareAndSet(STATE_INITIAL, STATE_STARTUP)) {
          boolean success = false;
          try {
            doStartup();
            if (myRunning.compareAndSet(STATE_STARTUP, STATE_RUNNING)) success = true;
          } finally {
            if (!success) myRunning.compareAndSet(STATE_STARTUP, STATE_INITIAL);
          }
        }
        break;
      case STATE_STARTUP: Thread.yield(); break;
      case STATE_RUNNING: return true;
      case STATE_STOPPED: LogHelper.error("already stopped"); return false;
      default: LogHelper.error("unknown state", state); return false;
      }
    }
  }

  private void doStartup() {
    if (!myDB.getUserData().putIfAbsent(ROLE, this))
      LogHelper.error("Another DBImage already registered", myDB.getUserData().getUserData(ROLE));
    myDB.addListener(myLife, myDBRequest);
  }

  void ensureRegistered(Lifespan life, BaseImageSlice slice) {
    if (life == null || life.isEnded()) return;
    if (!ensureStarted()) LogHelper.error("not running", slice);
    else if (slice.isInitial()) {
      mySlices.add(slice);
      slice.doStartSlice(life);
    }
  }

  void unregister(BaseImageSlice slice) {
    LogHelper.assertError(slice.isBuried(), slice);
    boolean removed = mySlices.remove(slice);
    LogHelper.assertError(removed, slice);
  }

  List<BaseImageSlice> copySlices() {
    List<BaseImageSlice> slices = Collections15.arrayList();
    for (BaseImageSlice slice : mySlices) if (slice.isRunning()) slices.add(slice);
    return slices;
  }

  void updateComplete(CacheUpdate update) {
    Threads.assertAWTThread();
    ReentrantReadWriteLock.WriteLock lock = myLock.writeLock();
    lock.lock();
    DataChange change;
    Collection<BaseImageSlice> updatedSlices;
    try {
      change = myData.applyUpdate(update);
      myICN = update.getICN();
      updatedSlices = update.getSlices();
      for (BaseImageSlice slice : updatedSlices) slice.applyUpdate(update, change);
    } finally {
      lock.unlock();
    }
    myDBRequest.updateComplete();
    for (BaseImageSlice slice : updatedSlices) slice.notifyListeners(change);
  }

  void copyCoherentUpdates(List<CoherentUpdate<?>> target) {
    synchronized (myCoherentUpdates) {
      target.addAll(myCoherentUpdates);
    }
  }

  private static final int REQ_NO = 0;
  private static final int REQ_WAITING = 1;
  private static final int REQ_RUNNING = 2;
  private static final int REQ_RESTART = 3;

  @Nullable
  public static DBImage getInstance(DBReader reader) {
    UserDataHolder userData = reader.getDatabaseUserData();
    if (userData == null) {
      LogHelper.error("Missing user data", reader);
      return null;
    }
    DBImage image = userData.getUserData(DBImage.ROLE);
    LogHelper.assertError(image != null, "Missing DB image");
    return image;
  }

  private class UpdaterRequest implements ReadTransaction<Object>, DBListener, Procedure<LongList> {
    private final AtomicInteger myState = new AtomicInteger(REQ_NO);
    private final Lifecycle myActualLife = new Lifecycle();

    public void request() {
      if (!ensureStarted()) return;
      while (true) {
        int state = myState.get();
        boolean done = false;
        switch (state) {
        case REQ_NO:
          done = myState.compareAndSet(REQ_NO, REQ_WAITING);
          break;
        case REQ_WAITING:
        case REQ_RESTART: return;
        case REQ_RUNNING:
          if (myState.compareAndSet(REQ_RUNNING, REQ_RESTART)) return;
          break;
        default: LogHelper.error("unknown state", state); return;
        }
        if (done) break;
      }
      myActualLife.cycle();
      myDB.readForeground(this);
    }

    @Override
    public Object transaction(DBReader reader) throws DBOperationCancelledException {
      doUpdate(reader);
      return null;
    }

    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      while (true) {
        int state = myState.get();
        boolean doUpdate = false;
        switch (state) {
        case REQ_NO:
        case REQ_WAITING: doUpdate = true; break;
        case REQ_RUNNING:
          if (myState.compareAndSet(REQ_RUNNING, REQ_RESTART)) return;
          break;
        case REQ_RESTART: return;
        default: LogHelper.error("unknown state", state);
        }
        if (doUpdate) break;
      }
      doUpdate(reader);
    }

    @Override
    public void invoke(LongList arg) {
      requestUpdate();
    }

    private void doUpdate(DBReader reader) {
      if (isRunning()) {
        while (true) {
          int state = myState.get();
          boolean done = false;
          switch (state) {
          case REQ_NO:
          case REQ_WAITING:
            if (myState.compareAndSet(state, REQ_RUNNING)) done = true;
            break;
          case REQ_RUNNING: requestUpdate();
          case REQ_RESTART: return;
          default: LogHelper.error("unknown state", state); return;
          }
          if (done) break;
        }
      }
      boolean success = false;
      try {
        new CacheUpdate(DBImage.this, reader, myActualLife.lifespan(), this).perform();
        success = true;
      } finally {
        if (!success) myState.set(REQ_NO);
      }
    }

    public void updateComplete() {
      while (true) {
        int state = myState.get();
        switch (state) {
        case REQ_NO: LogHelper.error("improper state", state); return;
        case REQ_RUNNING:
          if (myState.compareAndSet(REQ_RUNNING, REQ_NO)) return;
          break;
        case REQ_WAITING: LogHelper.error("improper state", state);
        case REQ_RESTART:
          if (myState.compareAndSet(REQ_RESTART, REQ_NO)) {
            requestUpdate();
            return;
          }
          break;
        default: LogHelper.error("unknown state", state); return;
        }
      }
    }
  }
}
