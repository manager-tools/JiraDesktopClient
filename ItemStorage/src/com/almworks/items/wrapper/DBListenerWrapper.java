package com.almworks.items.wrapper;

import com.almworks.items.api.DBEvent;
import com.almworks.items.api.DBListener;
import com.almworks.items.api.DBReader;

import static com.almworks.items.wrapper.DatabaseWrapperPrivateUtil.wrapReader;

class DBListenerWrapper implements DBListener {
  private final DBListener myListener;

  public DBListenerWrapper(DBListener listener) {
    myListener = listener;
  }

  @Override
  public void onDatabaseChanged(DBEvent event, DBReader reader) {
    DBReaderWrapper wrappedReader = wrapReader(reader);
    myListener.onDatabaseChanged(event, wrappedReader);
  }
}
