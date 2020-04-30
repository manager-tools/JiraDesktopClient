package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerPriority;

import java.util.Set;

public class Priority {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerPriority.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerPriority.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerPriority.NAME);
  public static final DBAttribute<Integer> IMPLICIT_ORDER = ServerJira.toScalarAttribute(ServerPriority.ORDER);
  public static final DBAttribute<Set<Long>> ONLY_IN_PROJECTS = ServerJira.toLinkSetAttribute(
    ServerPriority.ONLY_IN_PROJECTS);
  public static final DBAttribute<String> COLOR = ServerJira.toScalarAttribute(ServerPriority.COLOR);
  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerPriority.DESCRIPTION);
  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .narrowByAttribute(Issue.PROJECT, ONLY_IN_PROJECTS)
    .orderByNumber(IMPLICIT_ORDER, true)
    .renderFirstNotNull(NAME, ID)
    .create();
}
