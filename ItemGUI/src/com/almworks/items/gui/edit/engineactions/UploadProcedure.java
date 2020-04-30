package com.almworks.items.gui.edit.engineactions;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Collections15;

import java.util.Map;

public class UploadProcedure implements Procedure<LongList> {
  private final Database myDb;
  private final ConnectionManager myManager;

  public UploadProcedure(Database db, ConnectionManager manager) {
    myDb = db;
    myManager = manager;
  }

  public static Procedure<LongList> create(ActionContext context) throws CantPerformException {
    Database db = context.getSourceObject(Database.ROLE);
    ConnectionManager connections = context.getSourceObject(Engine.ROLE).getConnectionManager();
    return new UploadProcedure(db, connections);
  }

  @Override
  public void invoke(final LongList committed) {
    if (committed == null) return;
    myDb.readBackground(new ReadTransaction<Map<Connection, LongArray>>() {
      @Override
      public Map<Connection, LongArray> transaction(DBReader reader) throws DBOperationCancelledException {
        Map<Connection, LongArray> result = Collections15.hashMap();
        for (int i = 0; i < committed.size(); i++) {
          long item = committed.get(i);
          Long cItem = reader.getValue(item, SyncAttributes.CONNECTION);
          if (cItem == null || cItem <= 0) {
            LogHelper.error("Cannot upload", SyncUtils.readTrunk(reader, item), cItem);
            continue;
          }
          Connection connection = myManager.findByItem(cItem);
          if (connection == null) {
            LogHelper.error("Missing connection", cItem, SyncUtils.readTrunk(reader, cItem), item);
            continue;
          }
          LongArray upload = result.get(connection);
          if (upload == null) {
            upload = new LongArray();
            result.put(connection, upload);
          }
          upload.add(item);
        }
        return result;
      }
    }).finallyDo(ThreadGate.LONG, new Procedure<Map<Connection, LongArray>>() {
      @Override
      public void invoke(Map<Connection, LongArray> arg) {
        if (arg == null) return;
        for (Map.Entry<Connection, LongArray> entry : arg.entrySet()) {
          entry.getKey().uploadItems(entry.getValue());
        }
      }
    });
  }
}
