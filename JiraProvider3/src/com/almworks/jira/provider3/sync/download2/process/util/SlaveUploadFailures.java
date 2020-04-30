package com.almworks.jira.provider3.sync.download2.process.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.write.EntityWriter;
import com.almworks.util.LogHelper;
import gnu.trove.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.List;

public abstract class SlaveUploadFailures<T> {
  private final Entity myType;
  private final EntityKey<Entity> myMaster;

  protected SlaveUploadFailures(Entity type, EntityKey<Entity> master) {
    myType = type;
    myMaster = master;
  }

  public void perform(EntityWriter writer) {
    ArrayList<EntityHolder> slaves = writer.getUnresolved(myType);
    TLongObjectHashMap<List<T>> issueFailures = new TLongObjectHashMap<>();
    for (EntityHolder slave : slaves) {
      EntityHolder issue = slave.getReference(myMaster);
      if (issue == null) {
        LogHelper.error("Missing master issue", myType, myMaster);
        continue;
      }
      long issueItem = writer.getItem(issue);
      if (issueItem < 0) continue;
      List<T> failures = issueFailures.get(issueItem);
      if (failures == null) {
        failures = loadFailures(writer.getReader(), issueItem);
        issueFailures.put(issueItem, failures);
      }
      int index = findFailedSlave(failures, slave);
      if (index >= 0) {
        T failure = failures.remove(index);
        writer.addExternalResolution(slave, getItem(failure));
      }
    }
  }

  protected abstract long getItem(T failure);

  protected int findFailedSlave(List<T> failures, EntityHolder slave) {
    for (int i = 0; i < failures.size(); i++) {
      T failure =  failures.get(i);
      if (matches(failure, slave)) return i;
    }
    return -1;
  }

  protected abstract boolean matches(T failure, EntityHolder slave);

  protected abstract List<T> loadFailures(DBReader reader, long issueItem);
}
