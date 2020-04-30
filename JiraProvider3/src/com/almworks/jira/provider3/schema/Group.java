package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerGroup;
import com.almworks.jira.provider3.sync.schema.ServerJira;

public class Group {
  public static final String NULL_CONSTRAINT_ID = "-1";

  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerGroup.TYPE);
  public static final DBAttribute<String> ID = ServerJira.toScalarAttribute(ServerGroup.ID);
  public static final EnumItemCreator CREATOR = new EnumItemCreator.SimpleCreator(DB_TYPE, ID);

  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .create();
}
