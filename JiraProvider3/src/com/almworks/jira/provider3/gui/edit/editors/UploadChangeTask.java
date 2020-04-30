package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.ConnectionManager;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.collections.LongSet;
import gnu.trove.TLongObjectHashMap;

public class UploadChangeTask {
  private final TLongObjectHashMap<LongSet> myIssues = new TLongObjectHashMap<>();

  public void addSlave(ItemVersion slave, DBAttribute<Long>... masters) {
    for (DBAttribute<Long> masterRef : masters) {
      Long masterItem = slave.getValue(masterRef);
      if (masterItem == null || masterItem <= 0) continue;
      ItemVersion master = slave.forItem(masterItem);
      Long connection = master.getValue(SyncAttributes.CONNECTION);
      if (connection == null || connection <= 0) continue;
      if (masters.length > 1 && master.getSyncState().isLocalOnly()) continue;
      addToUpload(masterItem, connection);
    }
  }

  public void addPrimary(ItemVersion item) {
    long connection = item.getNNValue(SyncAttributes.CONNECTION, 0l);
    if (connection <= 0) return;
    addToUpload(item.getItem(), connection);
  }

  private void addToUpload(long item, long connection) {
    LongSet issues = myIssues.get(connection);
    if (issues == null) {
      issues = new LongSet();
      myIssues.put(connection, issues);
    }
    issues.add(item);
  }

  public void perform(Engine engine) {
    ConnectionManager manager = engine.getConnectionManager();
    for (long connectionItem : myIssues.keys()) {
      Connection connection = manager.findByItem(connectionItem);
      if (connection != null) connection.uploadItems(myIssues.get(connectionItem));
    }
  }
}
