package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerSecurity;

import java.util.Set;

public class Security {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerSecurity.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerSecurity.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerSecurity.NAME);
  public static final DBAttribute<Set<Long>> ONLY_IN_PROJECTS = ServerJira.toLinkSetAttribute(
    ServerSecurity.ONLY_IN_PROJECTS);
  public static final DBAttribute<Integer> IMPLICIT_ORDER = ServerJira.toScalarAttribute(ServerSecurity.ORDER);

  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .renderFirstNotNull(NAME, ID)
    .orderByNumber(IMPLICIT_ORDER, true)
    .narrowByAttribute(Issue.PROJECT, ONLY_IN_PROJECTS)
    .create();
}
