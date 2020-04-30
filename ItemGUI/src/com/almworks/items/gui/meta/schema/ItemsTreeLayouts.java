package com.almworks.items.gui.meta.schema;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;

public class ItemsTreeLayouts {
  private static final DBNamespace NS = MetaModule.NS.subNs("itemsTreeLayout");
  public static final DBItemType DB_TYPE = NS.type();
  /**
   * Should restore {@link com.almworks.util.components.TreeStructure TreeStructure&lt;com.almworks.api.application.LoadedItem, ?, TreeModelBridge&lt;com.almworks.api.application.LoadedItem&gt;&gt;}
   */
  public static final DBAttribute<byte[]> STRUCTURE_DATA = NS.bytes("loader", "Items tree layout loader");
  public static final DBAttribute<String> NAME = NS.string("name");
  public static final DBAttribute<String> ID = NS.string("id");
  public static final DBAttribute<Integer> ORDER = NS.integer("order");


  public static DBIdentity createId(DBIdentity owner, String id) {
    return new DBIdentity.Builder().put(DBCommons.OWNER.getAttribute(), owner).put(ID, id).put(DBAttribute.TYPE, DB_TYPE).create();
  }

  public static DBStaticObject create(DBIdentity owner, String id, String displayName, ScalarSequence structure, Integer order) {
    DBStaticObject.Builder builder = new DBStaticObject.Builder()
      .put(NAME, displayName)
      .putSequence(STRUCTURE_DATA, structure);
    if (order != null) builder.put(ORDER, order);
    return builder.create(createId(owner, id));

  }

  private ItemsTreeLayouts() {}
}
