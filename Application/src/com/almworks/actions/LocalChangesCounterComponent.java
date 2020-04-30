package com.almworks.actions;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.LongObjFunction2;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.CanBlock;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dyoma
 */
public class LocalChangesCounterComponent extends SimpleModifiable implements Startable {
  public static Role<LocalChangesCounterComponent> ROLE = Role.role("localChangesCounter");
  private final Engine myEngine;
  private final Lifecycle myLife = new Lifecycle();

  @Nullable
  private CollectingListener myListener;
  private DBLiveQuery myResult;

  public LocalChangesCounterComponent(Engine engine) {
    myEngine = engine;
  }

  public void start() {
    Database db = myEngine.getDatabase();
    myListener = new CollectingListener();
    myResult = db.filter(myEngine.getViews().getLocalChangesFilter()).liveQuery(myLife.lifespan(), myListener);
  }

  public void stop() {
    myLife.dispose();
    dispose();
    myResult = null;
    myListener = null;
  }


  @NotNull
  @CanBlock
  public DBResult<Map<Connection, LongList>> getChangedItems() {
    final ConnectionManager coman = myEngine.getConnectionManager();
    return myEngine.getDatabase().readBackground(new ReadTransaction<Map<Connection, LongList>>() {
      public Map<Connection, LongList> transaction(final DBReader reader) {
        assert myListener != null;
        if (myResult == null) return Collections.emptyMap();
        return myResult.fold(Collections15.<Connection, LongList>hashMap(), new LongObjFunction2<HashMap<Connection, LongList>>() {
          public HashMap<Connection, LongList> invoke(long a, HashMap<Connection, LongList> b) {
            long ci = Util.NN(SyncAttributes.CONNECTION.getValue(a, reader), 0L);
            Connection connection = coman.findByItem(ci);
            if (connection != null) {
              insert(b, connection, a);
            }
            return b;
          }
        });
      }
    });
  }

  private static void insert(HashMap<Connection, LongList> map, Connection key, long value) {
    LongList l = map.get(key);
    if (l == null) {
      l = new LongArray();
      map.put(key, l);
    }
    ((LongArray) l).add(value);
  }

  private class CollectingListener implements DBLiveQuery.Listener {
    @Override
    public void onICNPassed(long icn) {
    }

    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      if (myLife.isDisposed())
        return;
      fireChanged();
    }
  }
}
