package com.almworks.syncreg;

import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercubeImpl;
import com.almworks.api.syncreg.SyncCubeRegistry;
import com.almworks.api.syncreg.SyncFlagRegistry;
import com.almworks.api.syncreg.SyncRegistry;
import com.almworks.items.api.*;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.events.EventSource;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.persist.Persistable;
import com.almworks.util.io.persist.PersistableBoolean;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableString;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Startable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

class SyncRegistryImpl implements SyncRegistry, Startable {
  private final FireEventSupport<Listener> myEvents = FireEventSupport.createSynchronized(Listener.class);
  private final BasicScalarModel<Boolean> myLoaded = BasicScalarModel.createWithValue(false, true);
  private final SyncCubeRegistryImpl myCubeRegistry = new SyncCubeRegistryImpl(this);
  private final SyncFlagRegistryImpl myFlagRegistry = new SyncFlagRegistryImpl(this);

  private Bottleneck mySave;
  private int myUpdateLockers = 0;
  private boolean myMoreWhileLocked = false;
  private boolean myLessWhileLocked = false;
  private final Notifier[] myNotifiers = createNotifiers();
  private final Database myDb;

  private Notifier[] createNotifiers() {
    Notifier[] notifiers = new Notifier[4];
    notifiers[getIndex(true, false)] = new Notifier(true, false);
    notifiers[getIndex(false, true)] = new Notifier(false, true);
    notifiers[getIndex(true, true)] = new Notifier(true, true);
    return notifiers;
  }

  public SyncRegistryImpl(Database db) {
    myDb = db;
  }

  public void start() {
    mySave = new Bottleneck(500, ThreadGate.LONG(this), new SaveMethod());

    myDb.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        myCubeRegistry.load(reader);
        myFlagRegistry.load(reader);
        myLoaded.setValue(true);
        onSyncRegistryChanged(true, true);
        return null;
      }
    }).finallyDoWithResult(ThreadGate.STRAIGHT, new Procedure<DBResult<Object>>() {
      @Override
      public void invoke(DBResult<Object> arg) {
        LogHelper.assertError(arg.isSuccessful(), "Failed to load syncRegistry", arg.getError());
      }
    });
  }

  public void stop() {

  }

  public EventSource<Listener> getEventSource() {
    return myEvents;
  }

  public SyncFlagRegistry getSyncFlagRegistry() {
    return myFlagRegistry;
  }

  @NotNull
  public SyncCubeRegistry getSyncCubeRegistry() {
    return myCubeRegistry;
  }

  public void clearRegistryForConnection(@NotNull Connection connection) {
    myFlagRegistry.clearFlags(connection.getConnectionID());

    long connectionItem = connection.getConnectionItem();
    ItemHypercubeImpl cube = new ItemHypercubeImpl();
    cube.addValue(SyncAttributes.CONNECTION, connectionItem, true);
    myCubeRegistry.setUnsynced(cube);
  }

  public void lockUpdate() {
    Threads.assertAWTThread();
    myUpdateLockers++;
    myMoreWhileLocked = false;
    myLessWhileLocked = false;
  }

  public void unlockUpdate() {
    Threads.assertAWTThread();
    if (--myUpdateLockers <= 0) {
      if (myUpdateLockers < 0) {
        assert false : myUpdateLockers;
        myUpdateLockers = 0;
      }
      boolean more = myMoreWhileLocked;
      boolean less = myLessWhileLocked;
      myMoreWhileLocked = false;
      myLessWhileLocked = false;
      if (more || less) {
        myEvents.getDispatcher().onSyncRegistryChanged(more, less);
      }
    }
  }

  public ScalarModel<Boolean> getStartedModel() {
    return myLoaded;
  }

  private void save() {
    if (mySave != null && myLoaded.getValue())
      mySave.requestDelayed();
  }

  public void onSyncRegistryChanged(boolean moreSynchronized, boolean lessSynchronized) {
    assert moreSynchronized || lessSynchronized;
    if (!moreSynchronized && !lessSynchronized) {
        return;
    }
    ThreadGate.AWT.execute(getNotifier(moreSynchronized, lessSynchronized));
  }

  private Notifier getNotifier(boolean more, boolean less) {
    return myNotifiers[getIndex(more, less)];
  }

  private static int getIndex(boolean more, boolean less) {
    int index = 0;
    if (more) {
      index += 1;
    }
    if (less) {
      index += 2;
    }
    return index;
  }

  public boolean isLoaded() {
    return myLoaded.getValue();
  }

  private class Notifier implements Runnable {
    private final boolean myMore;
    private final boolean myLess;

    public Notifier(boolean more, boolean less) {
      myMore = more;
      myLess = less;
    }

    public void run() {
      Threads.assertAWTThread();
      save();
      if (myUpdateLockers == 0) {
        myEvents.getDispatcher().onSyncRegistryChanged(myMore, myLess);
      } else {
        if (myMore)
          myMoreWhileLocked = true;
        if (myLess) {
          myLessWhileLocked = true;
        }
      }
    }
  }

  private static Persistable<Map<String, Map<String, Boolean>>> createPersister() {
    return new PersistableHashMap<String, Map<String, Boolean>>(new PersistableString(),
      new PersistableHashMap<String, Boolean>(new PersistableString(), new PersistableBoolean()));
  }

  private class SaveMethod implements Runnable, WriteTransaction<Object> {
    private final AtomicBoolean myRequested = new AtomicBoolean(false);

    public void run() {
      if (myLoaded.getValue()) {
        if (myRequested.compareAndSet(false, true)) myDb.writeBackground(this);
      }
    }

    @Override
    public Object transaction(DBWriter writer) throws DBOperationCancelledException {
      myRequested.set(false);
      myCubeRegistry.save(writer);
      myFlagRegistry.save(writer);
      return null;
    }
  }
}
