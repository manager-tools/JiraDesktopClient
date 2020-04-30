package com.almworks.items.gui.meta.schema.constraints;

import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.ItemKeySubloader;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import org.jetbrains.annotations.Nullable;

public class Descriptors {
  private static final DBNamespace NS = MetaModule.NS.subNs("descriptors");
  private static final DBNamespace NS_FEATURES = NS.subNs("features");

  public static final DBItemType DB_TYPE = NS.type();
  public static final DBAttribute<Long> ATTRIBUTE = NS.link("attribute");
  public static final DBAttribute<String> XML_NAME = NS.string("xmlName");
  public static final DBAttribute<Long> ENUM_TYPE = NS.link("enumType");
  public static final DBAttribute<byte[]> CONSTRAINT_KIND = NS.bytes("constraintKind");
  public static final DBAttribute<byte[]> CONSTRAINT_ICON = NS.bytes("constraintIcon");

  /**
   * Default scalar constraint kind<br>
   * Value for {@link #CONSTRAINT_KIND}
   */
  public static final DBIdentity FEATURE_CONSTRAINT_SCALAR = feature("scalarConstraint");
  /**
   * Creates {@link com.almworks.explorer.qbuilder.filter.DateConstraintDescriptor} for fields representing a moment of time.<br>
   * Sequence: [acceptNull(boolean)]
   */
  private static final DBIdentity FEATURE_CONSTRAINT_DATE_TIME = feature("dateTimeConstraint");
  /** Creates {@link DayConstraint} for fields that represent a day (date in a normal, non-Java sense.) 
   * Sequence: no parameters. */
  private static final DBIdentity FEATURE_CONSTRAINT_DAY = feature("dayConstraint");
  /**
   * Enum constraint kind (for single or multi value attributes) without query-for-null value feature.<br>
   * Value for {@link #CONSTRAINT_KIND}
   */
  public static final DBIdentity FEATURE_CONSTRAINT_ENUM = feature("enumConstraint");
  /**
   * Enum constraint kind with support for search for null.<br>
   * Value for {@link #CONSTRAINT_KIND}
   * @see #FEATURE_CONSTRAINT_ENUM
   */
  private static final DBIdentity FEATURE_CONSTRAINT_NULLABLE_ENUM = feature("enumConstraintNullable");
  private static final DBIdentity FEATURE_SLAVE_TEXT = feature("slave.text");
  public static final DBIdentity FEATURE_ATTRIBUTE_SUBLOADER = feature("attributeSubloader");
  public static final DBIdentity FEATURE_PARENT_SUBLOADER = feature("parentSubloader");

  public static final DBIdentity FEATURE_SUBTREE_SUBLOADER = feature("subtreeSubloader");
  public static final ScalarSequence SEQUENCE_SCALAR_CONSTRAINT = ScalarSequence.create(FEATURE_CONSTRAINT_SCALAR);
  public static final ScalarSequence SEQUENCE_ENUM_CONSTRAINT = ScalarSequence.create(FEATURE_CONSTRAINT_ENUM);
  public static final ScalarSequence SEQUENCE_DAY_CONSTRAINT = ScalarSequence.create(FEATURE_CONSTRAINT_DAY);


  public static DBIdentity createId(DBIdentity owner, DBAttribute<?> attribute) {
    return new DBIdentity.Builder().put(DBCommons.OWNER.getAttribute(), owner).put(ATTRIBUTE, attribute).put(DBAttribute.TYPE, DB_TYPE).create();
  }

  public static DBStaticObject create(DBIdentity owner, DBAttribute<?> attribute, String displayName, String xmlName, @Nullable DBStaticObject enumType, ScalarSequence constraintKind, @Nullable ScalarSequence constraintIcon) {
    return new DBStaticObject.Builder()
      .put(DBCommons.DISPLAY_NAME.getAttribute(), displayName)
      .put(XML_NAME, xmlName)
      .putReference(ENUM_TYPE, enumType)
      .putSequence(CONSTRAINT_KIND, constraintKind)
      .putSequence(CONSTRAINT_ICON, constraintIcon)
      .create(createId(owner, attribute));
  }

  public static ScalarSequence nullableEnum(String id, String displayName) {
    if (displayName == null) displayName = id;
    if (displayName == null) return ScalarSequence.create(FEATURE_CONSTRAINT_ENUM);
    return new ScalarSequence.Builder().append(FEATURE_CONSTRAINT_NULLABLE_ENUM).append(id).append(displayName).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_CONSTRAINT_SCALAR, SerializableFeature.NoParameters.create(ScalarConstraint.INSTANCE, ConstraintKind.class));
    registry.register(FEATURE_CONSTRAINT_DAY, SerializableFeature.NoParameters.create(new DayConstraint(), ConstraintKind.class));
    registry.register(FEATURE_CONSTRAINT_ENUM, SerializableFeature.NoParameters.create(EnumConstraint.NO_NULL, ConstraintKind.class));
    registry.register(FEATURE_CONSTRAINT_NULLABLE_ENUM, EnumConstraint.NULLABLE);
    registry.register(FEATURE_ATTRIBUTE_SUBLOADER, ItemKeySubloader.Attribute.SERIALIZABLE);
    registry.register(FEATURE_PARENT_SUBLOADER, ItemKeySubloader.Parent.SERIALIZABLE);
    registry.register(FEATURE_SUBTREE_SUBLOADER, ItemKeySubloader.Subtree.SERIALIZABLE);
    registry.register(FEATURE_CONSTRAINT_DATE_TIME, RemoteDateConstraint.FEATURE);
    registry.register(FEATURE_SLAVE_TEXT, SlaveTextConstraint.FEATURE);
  }

  public static ScalarSequence dateConstraint(boolean acceptNull) {
    return new ScalarSequence.Builder().append(FEATURE_CONSTRAINT_DATE_TIME).append(acceptNull).create();
  }

  public static DBStaticObject slaveText(DBIdentity owner, String displayName, String xmlName, DBAttribute<Long> masterReference, DBAttribute<String> slaveText) {
    return create(owner, slaveText, displayName, xmlName, null, new ScalarSequence.Builder().append(FEATURE_SLAVE_TEXT).append(masterReference).create(), null);
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURES.object(id));
  }
}
