package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerStatusCategory;

public class StatusCategory {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerStatusCategory.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerStatusCategory.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerStatusCategory.NAME);
  public static final DBAttribute<String> COLOR_NAME = ServerJira.toScalarAttribute(ServerStatusCategory.COLOR_NAME);
}
