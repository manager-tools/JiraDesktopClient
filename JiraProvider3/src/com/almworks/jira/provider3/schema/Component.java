package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerComponent;
import com.almworks.jira.provider3.sync.schema.ServerJira;

public class Component {
  public static final String NULL_CONSTRAINT_ID = "_no_component_";
  public static final String NULL_CONSTRAINT_NAME = "No Component";

  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerComponent.ID);
  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerComponent.PROJECT);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerComponent.NAME);
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerComponent.TYPE);

  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(NAME)
    .narrowByAttribute(Issue.PROJECT, PROJECT)
    .renderFirstNotNull(NAME, ID)
    .create();
}
