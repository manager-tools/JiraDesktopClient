package com.almworks.items.gui.meta.schema.export;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;

public class Exports {
  private static final DBNamespace NS = MetaModule.NS.subNs("exports");
  private static final DBNamespace NS_FEATURES = NS.subNs("features");
  public static final DBItemType DB_TYPE = NS.type();

  /**
   * Value for {@link com.almworks.api.application.util.ItemExport#getId()}
   */
  public static final DBAttribute<String> ID = NS.string("id");

  /**
   * Serialized {@link ExportPolicy}
   */
  public static final DBAttribute<byte[]> POLICY = NS.bytes("policy");

  public static DBStaticObject create(DBIdentity owner, String id, String displayName, ScalarSequence policy) {
    return new DBStaticObject.Builder()
      .put(DBCommons.DISPLAY_NAME.getAttribute(), displayName)
      .putSequence(POLICY, policy)
      .create(createId(owner, id));
  }

  private static DBIdentity createId(DBIdentity owner, String id) {
    return new DBIdentity.Builder().put(DBAttribute.TYPE, DB_TYPE).put(DBCommons.OWNER.getAttribute(), owner).put(ID, id).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    ModelKeyValueExport.registerFeatures(registry);
  }

  public static DBNamespace featuresNS(String ns) {
    return NS_FEATURES.subNs(ns);
  }
}
