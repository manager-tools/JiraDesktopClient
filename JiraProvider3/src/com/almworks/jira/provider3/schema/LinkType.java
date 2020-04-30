package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerLinkType;
import com.almworks.util.bool.BoolExpr;

public class LinkType {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerLinkType.TYPE);
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerLinkType.ID);
  public static final DBAttribute<String> NAME = ServerJira.toScalarAttribute(ServerLinkType.NAME);
  public static final DBAttribute<String> INWARD_DESCRIPTION = ServerJira.toScalarAttribute(
    ServerLinkType.INWARD_DESCRIPTION);
  public static final DBAttribute<String> OUTWARD_DESCRIPTION = ServerJira.toScalarAttribute(
    ServerLinkType.OUTWARD_DESCRIPTION);

  public static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(DB_TYPE)
    .setUniqueKey(ID)
    .renderFirstNotNull(NAME, ID)
    .addAttributeSubloaders(NAME, INWARD_DESCRIPTION, OUTWARD_DESCRIPTION, ID)
    .create();

  public static final BoolExpr<DP> TYPES_EXPR = DPEqualsIdentified.create(DBAttribute.TYPE, DB_TYPE);
}
