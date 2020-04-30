package com.almworks.api.engine;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.cache.util.AttributeLoader;
import com.almworks.items.cache.util.ItemAttribute;
import com.almworks.items.util.DBNamespace;

public class DBCommons {
  public static final DBNamespace NS = DBNamespace.moduleNs("platform");
  public static final DBIdentifiedObject PLATFORM = NS.object("platform");

  public static final ItemAttribute OWNER = new ItemAttribute(NS.link("owner"));
  public static final AttributeLoader<String> DISPLAY_NAME = AttributeLoader.create(NS.string("displayName"));
}
