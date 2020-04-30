package com.almworks.items.wrapper;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBTrigger;
import com.almworks.items.api.DBWriter;

import static com.almworks.items.wrapper.DatabaseWrapperPrivateUtil.wrapWriter;

class DBTriggerWrapper extends DBTrigger {
  private final DBTrigger myTrigger;

  public DBTriggerWrapper(DBTrigger trigger) {
    super(trigger.getId(), ItemStorageAdaptor.wrapExpr(trigger.getExpr()));
    myTrigger = trigger;
  }

  @Override
  public void apply(LongList itemsSorted, DBWriter writer) {
    DBWriterWrapper wrappedWriter = wrapWriter(writer);
    myTrigger.apply(itemsSorted, wrappedWriter);
  }
}
