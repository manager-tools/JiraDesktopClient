package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.cache.util.ItemListAttribute;
import com.almworks.items.cache.util.ItemSetAttribute;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerWorkflowAction;

import java.util.List;
import java.util.Set;

public class WorkflowAction {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerWorkflowAction.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerWorkflowAction.ID);
  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerWorkflowAction.PROJECT);
  public static final DBAttribute<Long> ISSUE_TYPE = ServerJira.toLinkAttribute(ServerWorkflowAction.ISSUE_TYPE);
  public static final DBAttribute<String> _NAME = ServerJira.toScalarAttribute(ServerWorkflowAction.NAME);
  private static final DBAttribute<List<Long>> _FIELDS = ServerJira.toLinkListAttribute(ServerWorkflowAction.FIELDS);
  private static final DBAttribute<Set<Long>> _MANDATORY_FIELDS = ServerJira.toLinkSetAttribute(
    ServerWorkflowAction.MANDATORY_FIELDS);
  private static final DBAttribute<Long> _TARGET_STATUS = ServerJira.toLinkAttribute(ServerWorkflowAction.TARGET_STATUS);

  public static final AttributeLoader<String> NAME = new AttributeLoader<String>(_NAME);
  public static final ItemListAttribute FIELDS = new ItemListAttribute(_FIELDS);
  public static final ItemAttribute TARGET_STATUS = new ItemAttribute(_TARGET_STATUS);
  public static final ItemSetAttribute MANDATORY_FIELDS = new ItemSetAttribute(_MANDATORY_FIELDS);
}
