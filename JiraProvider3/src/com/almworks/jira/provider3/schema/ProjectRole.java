package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerProjectRole;

public class ProjectRole {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerProjectRole.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerProjectRole.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerProjectRole.NAME);
  public static final DBAttribute<String> DESCRIPTION = ServerJira.toScalarAttribute(ServerProjectRole.DESCRIPTION);
//  public static final DBAttribute<Long> PROJECT = ServerJira.toLinkAttribute(ServerProjectRole.PROJECT);

  public static final DBAttribute<Boolean> PROJECT_ROLES_ONLY = ServerJira.toScalarAttribute(ServerProjectRole.PROJECT_ROLES_ONLY);

  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(NAME)
    .create();

}
