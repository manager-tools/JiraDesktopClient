package com.almworks.items.gui.meta.schema.dnd;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.schema.applicability.Applicabilities;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import org.jetbrains.annotations.Nullable;

public abstract class DnDChange {
  public abstract void prepare(DnDApplication application);

  public static final DBNamespace NS = MetaModule.NS.subNs("dndChanges");
  private static final DBNamespace NS_FEATURES = NS.subNs("features");
  public static final DBItemType DB_TYPE = NS.type();
  public static final DBAttribute<Long> CUBE_ATTRIBUTE = NS.link("cubeAttribute");
  public static final DBAttribute<Long> MODEL_KEY = NS.link("modelKey");
  public static final DBAttribute<Long> ENUM_TYPE = NS.link("enumType");
  public static final DBAttribute<Long> CONSTRAINT = NS.link("constraint");
  public static final DBAttribute<String> CONFIG = NS.string("config");
  public static final DBAttribute<String> NOT_SUPPORTED_MESSAGE = NS.string("notSupported");

  private static DBIdentity createId(DBIdentity owner, DBAttribute<?> attribute) {
    if (attribute.getScalarClass() != Long.class) throw new IllegalArgumentException("Must be item reference " + attribute);
    return new DBIdentity.Builder().put(DBCommons.OWNER.getAttribute(), owner)
      .put(CUBE_ATTRIBUTE, attribute)
      .put(DBAttribute.TYPE, DB_TYPE)
      .create();
  }

  public static DBStaticObject createStaticNoChange(DBIdentity owner, DBAttribute<?> attribute, DBStaticObject modelKey, String notSupported) {
    return new DBStaticObject.Builder()
      .putReference(MODEL_KEY, modelKey)
      .put(NOT_SUPPORTED_MESSAGE, notSupported)
      .create(createId(owner, attribute));
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }

  public static void registerFeatures(FeatureRegistry registry) {
  }

  public static DBStaticObject create(DBIdentity owner, DBAttribute<?> attribute, @Nullable String config, DBStaticObject enumType, DBStaticObject modelKey, DBStaticObject constraint, @Nullable ScalarSequence applicability) {
    if (applicability == null) applicability = Applicabilities.SEQUENCE_SATISFY_ANY;
    if (config != null && config.isEmpty()) config = null;
    return new DBStaticObject.Builder()
      .put(DnDChange.CONFIG, config)
      .putReference(DnDChange.ENUM_TYPE, enumType)
      .putReference(DnDChange.MODEL_KEY, modelKey)
      .putReference(DnDChange.CONSTRAINT, constraint)
      .putSequence(Applicability.ATTRIBUTE.getAttribute(), applicability)
      .create(DnDChange.createId(owner, attribute));
  }
}
