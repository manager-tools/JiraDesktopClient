package com.almworks.timetrack.impl;

import com.almworks.api.engine.Engine;
import com.almworks.integers.LongIterator;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

public class RemovedItemsCleaner implements Startable, DBLiveQuery.Listener {
  private final TimeTracker myTimeTracker;
  private final Database myDatabase;
  private final Engine myEngine;

  private final DetachComposite myLife = new DetachComposite();

  public RemovedItemsCleaner(TimeTracker timeTracker, Database database, Engine engine) {
    myTimeTracker = timeTracker;
    myDatabase = database;
    myEngine = engine;
  }

  @Override
  public void onICNPassed(long icn) {
  }

  @Override
  public void start() {
    myDatabase.liveQuery(myLife, myEngine.getViews().getPrimaryItemsFilter(), this);
  }

  @Override
  public void stop() {
    myLife.detach();
  }

  @Override
  public void onDatabaseChanged(DBEvent event, DBReader reader) {
    final LongList removed = event.getRemovedSorted();
    if(removed.isEmpty()) {
      return;
    }

    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        clearState(removed);
      }
    });
  }

  private void clearState(LongList items) {
    for(final LongIterator it = items.iterator(); it.hasNext();) {
      myTimeTracker.clearStateForItem(it.nextValue());
    }
  }
}
