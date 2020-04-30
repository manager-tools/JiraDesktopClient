package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import com.almworks.jira.provider3.sync.schema.ServerLink;

public class Link {
  public static final DBItemType DB_TYPE = ServerJira.toItemType(ServerLink.TYPE);
  public static final ItemAttribute SOURCE = new ItemAttribute(ServerJira.toLinkAttribute(ServerLink.SOURCE));
  public static final ItemAttribute TARGET = new ItemAttribute(ServerJira.toLinkAttribute(ServerLink.TARGET));
  public static final DBAttribute<Integer> ID = ServerJira.toScalarAttribute(ServerLink.ID);
  public static final DBAttribute<Long> LINK_TYPE = ServerJira.toLinkAttribute(ServerLink.LINK_TYPE);
}
