package com.almworks.items.impl.migrations;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBWriter;

public abstract class DBMigrationProcedure {
  private final DBIdentifiedObject myId;

  protected DBMigrationProcedure(String id) {
    myId = new DBIdentifiedObject(id);
  }

  public void migrateIfNeeded(DBWriter writer) {
    if(writer.findMaterialized(myId) <= 0) {
      if(migrate(writer)) {
        writer.materialize(myId);
      }
    }
  }

  protected abstract boolean migrate(DBWriter w);
}
