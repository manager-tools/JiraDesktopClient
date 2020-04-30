package com.almworks.items.wrapper;

import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBLiveQuery;
import com.almworks.items.api.DBReader;

import static com.almworks.items.wrapper.DatabaseWrapperPrivateUtil.wrapReader;

class DBLiveQueryListenerWrapper implements DBLiveQuery.Listener {
  private final DBLiveQuery.Listener myListener;

  public DBLiveQueryListenerWrapper(DBLiveQuery.Listener listener) {
    myListener = listener;
  }

  @Override
  public void onICNPassed(long icn) {
    myListener.onICNPassed(icn);
  }

  @Override
  public void onDatabaseChanged(DBEvent event, DBReader reader) {
    DBReaderWrapper wrappedReader = wrapReader(reader);
    myListener.onDatabaseChanged(event, wrappedReader);
  }
}
