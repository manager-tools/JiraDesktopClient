package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.cache.util.ItemListAttribute;
import com.almworks.jira.provider3.sync.schema.ServerActionSet;
import com.almworks.jira.provider3.sync.schema.ServerJira;

import java.util.List;

public class WorkflowActionSet {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerActionSet.TYPE);
  private static final DBAttribute<List<Long>> _ACTIONS = ServerJira.toLinkListAttribute(ServerActionSet.ACTIONS);
  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerActionSet.PROJECT);
  public static final DBAttribute<Long> ISSUE_TYPE = ServerJira.toLinkAttribute(ServerActionSet.ISSUE_TYPE);
  public static final DBAttribute<Long> STATUS = ServerJira.toLinkAttribute(ServerActionSet.STATUS);

  public static final ItemListAttribute ACTIONS = new ItemListAttribute(_ACTIONS);

}
