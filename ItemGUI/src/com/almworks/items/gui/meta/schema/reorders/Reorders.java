package com.almworks.items.gui.meta.schema.reorders;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import org.jetbrains.annotations.Nullable;

public class Reorders {
  private static final DBNamespace NS = MetaModule.NS.subNs("reorders");
  public static final DBItemType DB_TYPE = NS.type();
  public static final DBAttribute<String> ID = NS.string("name");
  public static final DBAttribute<String> DISPLAY_NAME = NS.string("displayName");
  public static final DBAttribute<Long> ATTRIBUTE = NS.link("attribute");
  public static final DBAttribute<Long> COLUMN = NS.link("column");
  public static final DBAttribute<Long> MODEL_KEY = NS.link("modelKey");
  /**
   * Serialized {@link com.almworks.items.gui.meta.schema.applicability.Applicability}
   * @see com.almworks.items.gui.meta.schema.applicability.Applicabilities
   */
  public static final DBAttribute<byte[]> APPLICABILITY = NS.bytes("applicability");

  public static DBStaticObject create(DBIdentity owner, String id, String displayName, DBAttribute<?> attribute, DBStaticObject modelKey, DBStaticObject column, @Nullable ScalarSequence applicability) {
    DBStaticObject.Builder builder = new DBStaticObject.Builder()
      .put(DISPLAY_NAME, displayName)
      .putReference(ATTRIBUTE, attribute)
      .putReference(MODEL_KEY, modelKey)
      .putReference(COLUMN, column);
    if (applicability != null) builder.putSequence(APPLICABILITY, applicability);
    return builder.create(createId(owner, id));
  }

  public static DBIdentity createId(DBIdentity owner, String name) {
    return new DBIdentity.Builder().put(DBAttribute.TYPE, DB_TYPE).put(DBCommons.OWNER.getAttribute(), owner).put(ID, name).create();
  }
}
