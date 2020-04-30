package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.slaves.SimpleDependent;
import com.almworks.jira.provider3.sync.download2.details.slaves.SlaveLoader;
import com.almworks.jira.provider3.sync.download2.rest.JRIssue;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.util.LogHelper;

import java.util.Collection;

class Subtasks implements SlaveLoader<EntityBag2> {
  @Override
  public Collection<? extends Parsed<EntityBag2>> loadValue(Object jsonObject, int order) {
    Entity subtask = JRIssue.DUMMY_JSON_CONERTOR.convert(jsonObject);
    if (subtask == null) {
      LogHelper.error("Failed to load subtask", jsonObject != null ? jsonObject.getClass() : "<null>");
      return null;
    }
    return SimpleDependent.MyParsed.singleton(ServerIssue.TYPE, ServerIssue.PARENT, subtask, null, order);
  }

  @Override
  public EntityBag2 createBags(EntityHolder master) {
    EntityBag2 removeSubtasks = master.getTransaction().addBagRef(ServerIssue.TYPE, ServerIssue.PARENT, master);
    removeSubtasks.changeReference(ServerIssue.PARENT, null);
    return removeSubtasks;
  }

  @Override
  public String toString() {
    return "Subtasks";
  }
}
