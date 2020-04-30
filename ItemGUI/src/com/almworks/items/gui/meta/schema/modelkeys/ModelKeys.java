package com.almworks.items.gui.meta.schema.modelkeys;

import com.almworks.api.application.DataPromotionPolicy;
import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Date;

public class ModelKeys {
  private static final DBNamespace NS = MetaModule.NS.subNs("modelKeys");
  private static final DBNamespace NS_FEATURE = NS.subNs("feature");
  public static final DBAttribute<String> NAME = NS.string("name");
  public static final DBItemType DB_TYPE = NS.type();
  /**
   * Factory to construct model key. First 8 bytes refer {@link com.almworks.items.gui.meta.commons.SerializableFeature}
   * that read parameters from rest bytes and constructs implementation of {@link ModelKeyLoader}.
   */
  public static final DBAttribute<byte[]> DATA_LOADER = NS.bytes("dataLoader");
  /**
   * Defines {@link com.almworks.api.application.ModelKey#getDataPromotionPolicy() ModelKey#getDataPromotionPolicy()}<br>
   * Object restored via {@link com.almworks.items.gui.meta.commons.SerializedObjectAttribute Deserializer}, value should be of type {@link com.almworks.api.application.DataPromotionPolicy DataPromotionPolicy}<br>
   * This attribute is optional if no value is set then {@link DataPromotionPolicy#STANDARD} is assumed.
   * @see #SEQUENCE_PROMOTION_ALWAYS
   */
  public static final DBAttribute<byte[]> DATA_PROMOTION_POLICY = NS.bytes("dataPromotionPolicy");

  public static final DBIdentity FEATURE_LOADER_STRING = feature("dataLoader.scalarAttribute");
  private static final DBIdentity FEATURE_LOADER_ENUM = feature("dataLoader.enumAttribute");
  public static final DBIdentity FEATURE_LOADER_DECIMAL = feature("dataLoader.decimal");
  public static final DBIdentity FEATURE_LOADER_INTEGER = feature("dataLoader.integer");
  public static final DBIdentity FEATURE_LOADER_BOOLEAN = feature("dataLoader.boolean");
  public static final DBIdentity FEATURE_LOADER_DATE_TIME = feature("dataLoader.dateTime");
  public static final DBIdentity FEATURE_LOADER_BOOLEAN_WITH_STATE_ICON = feature("dataLoader.booleanWithStateIcon");

  private static final DBIdentity FEATURE_PROMOTION_ALWAYS = feature("promotion.std.always");
  private static final DBIdentity FEATURE_PROMOTION_FULL_DOWNLOAD = feature("promotion.std.fullDownload");
  public static final ScalarSequence SEQUENCE_PROMOTION_ALWAYS = ScalarSequence.create(FEATURE_PROMOTION_ALWAYS);
  public static final ScalarSequence SEQUENCE_PROMOTION_FULL_DOWNLOAD = ScalarSequence.create(FEATURE_PROMOTION_FULL_DOWNLOAD);

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURE.object(id));
  }

  public static DBStaticObject create(DBIdentity owner, String id, ScalarSequence dataLoader, @Nullable ScalarSequence dataPromotion) {
    DBStaticObject.Builder builder = new DBStaticObject.Builder()
      .putSequence(ModelKeys.DATA_LOADER, dataLoader);
    if (dataPromotion != null) builder.putSequence(ModelKeys.DATA_PROMOTION_POLICY, dataPromotion);
    return builder.create(ModelKeys.createId(owner, id));
  }

  private static DBIdentity createId(DBIdentity owner, String id) {
    return new DBIdentity.Builder().put(DBAttribute.TYPE, DB_TYPE).put(DBCommons.OWNER.getAttribute(), owner).put(NAME, id).create();
  }

  public static ScalarSequence scalarDataLoader(DBAttribute<String> stringAttribute) {
    DBAttribute.ScalarComposition composition = stringAttribute.getComposition();
    Class<?> aClass = stringAttribute.getScalarClass();
    if (composition != DBAttribute.ScalarComposition.SCALAR || !String.class.equals(aClass)) {
      LogHelper.error("String attribute expected", aClass, composition, stringAttribute);
      return null;
    }
    return new ScalarSequence.Builder().append(FEATURE_LOADER_STRING).append(stringAttribute).create();
  }

  public static ScalarSequence enumAttribute(DBAttribute<?> enumAttribute, ItemProxy enumType) {
    Class<?> aClass = enumAttribute.getScalarClass();
    if (!Long.class.equals(aClass)) {
      LogHelper.error("Expected link attribute", aClass, enumAttribute);
      return null;
    }
    return new ScalarSequence.Builder().append(FEATURE_LOADER_ENUM).append(enumAttribute).append(enumType).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_LOADER_STRING, SimpleDataKind.scalarValue(String.class));
    registry.register(FEATURE_LOADER_ENUM, EnumValueDataKind.FEATURE);
    registry.register(FEATURE_LOADER_DECIMAL, SimpleDataKind.scalarValue(BigDecimal.class));
    registry.register(FEATURE_LOADER_INTEGER, SimpleDataKind.scalarValue(Integer.class));
    registry.register(FEATURE_LOADER_BOOLEAN, SimpleDataKind.scalarValue(Boolean.class));
    registry.register(FEATURE_LOADER_DATE_TIME, SimpleDataKind.scalarValue(Date.class));
    registry.register(FEATURE_LOADER_BOOLEAN_WITH_STATE_ICON, BooleanDataKindWithStateIcon.FEATURE);

    registry.register(FEATURE_PROMOTION_ALWAYS, SerializableFeature.NoParameters.create(DataPromotionPolicy.ALWAYS, DataPromotionPolicy.class));
    registry.register(FEATURE_PROMOTION_FULL_DOWNLOAD, SerializableFeature.NoParameters.create(DataPromotionPolicy.FULL_DOWNLOAD, DataPromotionPolicy.class));
  }
}
