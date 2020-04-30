package com.almworks.items.api;

import com.almworks.items.util.DBNamespace;

public class DBItemType extends DBIdentifiedObject {
  public static final DBNamespace TYPE_NS = Database.NS.subNs("type");
  public static final DBItemType TYPE = new DBItemType(TYPE_NS.idType(), "TType", false);

  public static final DBNamespace ATTR_NS =  Database.NS.subNs("attribute");
  public static final DBItemType ATTRIBUTE = new DBItemType(ATTR_NS.idType(), "TAttribute", false);

  public static final DBItemType[] EMPTY_ARRAY = new DBItemType[0];

  static {
    // make sure DBAttribute is initialized, as it is initializing static types here
    DBAttribute.class.getClassLoader();
  }

  public DBItemType(String id) {
    this(id, null, true);
  }

  public DBItemType(String id, String name) {
    this(id, name, true);
  }

  DBItemType(String id, String name, boolean initialize) {
    super(id, name, initialize);
    if (initialize) {
      init();
    }
  }

  void init() {
    initialize(DBAttribute.NAME, getName());
    initialize(DBAttribute.ID, getId());
    initialize(DBAttribute.TYPE, DBItemType.TYPE);
  }
}
