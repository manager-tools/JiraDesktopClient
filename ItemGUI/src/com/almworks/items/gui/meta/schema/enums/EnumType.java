package com.almworks.items.gui.meta.schema.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.ItemKeyDisplayName;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.components.CanvasRenderer;

public class EnumType {
  private static final DBNamespace NS = MetaModule.NS.subNs("enumType");
  private static final DBNamespace NS_FEATURE = NS.subNs("feature");
  public static final DBAttribute<Long> ENUM_TYPE = NS.link("type");
  public static final DBAttribute<Boolean> SEARCH_SUBSTRING = NS.bool("searchSubstring");
  /**
   * @see com.almworks.items.gui.meta.schema.renderers.ItemRenderers
   */
  public static final SerializedObjectAttribute<CanvasRenderer<ItemKey>> EDITOR_RENDERER
    = (SerializedObjectAttribute)SerializedObjectAttribute.create(CanvasRenderer.class, NS.bytes("editorRenderer"));

  public static final DBAttribute<byte[]> NARROWER = NS.bytes("narrower");
  private static final DBIdentity FEATURE_NARROW_DEFAULT = feature("narrow.default");
  private static final DBIdentity FEATURE_NARROW_IDENTITY = feature("narrow.identity");
  private static final DBIdentity FEATURE_NARROW_SINGLE_ATTRIBUTE = feature("narrow.singleSet");
  public static final ScalarSequence NARROW_DEFAULT = new ScalarSequence.Builder().append(FEATURE_NARROW_DEFAULT).create();
  public static final ScalarSequence NARROW_IDENTITY = new ScalarSequence.Builder().append(FEATURE_NARROW_IDENTITY).create();

  public static abstract class ItemKeys {
    /**
     * Attribute reference. The value for {@link com.almworks.api.application.ItemKey#getId() getId()}
     */
    public static final DBAttribute<Long> UNIQUE_KEY = NS.link("uniqueKey");

    /**
     * Restores {@link com.almworks.items.gui.meta.ItemKeyDisplayName}
     */
    public static final DBAttribute<byte[]> RENDERER = NS.bytes("renderer");
    public static final DBIdentity FEATURE_RENDERER_FIRST_NOT_NULL = feature("renderer.firstNotNull");
    public static final DBIdentity FEATURE_RENDERER_PATH_FROM_ROOT = feature("renderer.pathFromRoot");

    /**
     * The value for {@link com.almworks.api.application.ItemKey#getIcon() getIcon}
     */
    public static final DBAttribute<byte[]> ICON_LOADER = NS.bytes("iconLoader");
    /**
     * Sequence [iconUrl:DBAttribute&lt;String&gt;]
     */
    private static final DBIdentity FEATURE_ICON_BY_URL_ATTRIBUTE = feature("iconLoader.urlAttribute");

    public static final DBAttribute<byte[]> ORDER = NS.bytes("order");
    private static final DBIdentity FEATURE_ORDER_BY_DISPLAY_NAME = feature("order.byDisplayName");
    private static final DBIdentity FEATURE_ORDER_BY_NUMBER_VALUE = feature("order.byIntValue");
    private static final DBIdentity FEATURE_ORDER_NO = feature("order.no");
    public static final ScalarSequence SEQUENCE_BY_DISPLAY_NAME = ScalarSequence.create(FEATURE_ORDER_BY_DISPLAY_NAME);

    public static final DBAttribute<byte[]> SUBLOADERS = NS.bytes("itemKeySubloaders");

    private static void registerFeatures(FeatureRegistry registry) {
      registry.register(FEATURE_ORDER_BY_DISPLAY_NAME, OrderKind.READ_BY_DISPLAY_NAME);
      registry.register(FEATURE_ORDER_BY_NUMBER_VALUE, OrderKind.READ_BY_NUMBER_ORDER);
      registry.register(FEATURE_ORDER_NO, OrderKind.READ_NO_ORDER);
      registry.register(FEATURE_RENDERER_FIRST_NOT_NULL, ItemKeyDisplayName.FIRST_NOT_NULL);
      registry.register(FEATURE_RENDERER_PATH_FROM_ROOT, ItemKeyDisplayName.PATH_FROM_ROOT);
      registry.register(FEATURE_ICON_BY_URL_ATTRIBUTE, IconLoader.FromUrl.LOADER);
    }
  }

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURE.object(id));
  }

  public static DBIdentity simpleId(DBItemType type) {
    return new DBIdentity.Builder().put(ENUM_TYPE, DBIdentity.fromDBObject(type)).create();
  }

  public static long findByDBType(DBReader reader, DBItemType type) {
    LongArray items = reader.query(DPEqualsIdentified.create(ENUM_TYPE, type)).copyItemsSorted();
    if (items.isEmpty()) return 0;
    LogHelper.assertError(items.size() == 1, "Multiple enum types for single dbtype", type, items);
    return items.get(0);
  }

  public static ScalarSequence rendererFirstNotNull(DBAttribute<?>... attributes) {
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(ItemKeys.FEATURE_RENDERER_FIRST_NOT_NULL);
    builder.append(attributes.length);
    for (DBAttribute<?> attribute : attributes) builder.append(attribute);
    return builder.create();
  }

  public static ScalarSequence rendererPathFromRoot(DBAttribute<Long> parentAttr, String pathSep, DBAttribute<?>... attributes) {
    final ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(ItemKeys.FEATURE_RENDERER_PATH_FROM_ROOT);
    builder.append(parentAttr);
    builder.append(pathSep);
    builder.append(attributes.length);
    for(final DBAttribute<?> attribute : attributes) {
      builder.append(attribute);
    }
    return builder.create();
  }

  public static ScalarSequence orderByNumber(DBAttribute<? extends Number> intAttribute, boolean direct) {
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(ItemKeys.FEATURE_ORDER_BY_NUMBER_VALUE);
    builder.append(intAttribute);
    builder.append(direct);
    return builder.create();
  }

  public static ScalarSequence narrowByAttribute(DBAttribute<Long> issueAttribute, DBAttribute<?> enumRestriction) {
    if (enumRestriction == null || !Long.class.equals(enumRestriction.getScalarClass())) {
      LogHelper.error("Wrong restricting attribute", enumRestriction);
      return NARROW_DEFAULT;
    }
    ScalarSequence.Builder builder = new ScalarSequence.Builder();
    builder.append(FEATURE_NARROW_SINGLE_ATTRIBUTE);
    builder.append(issueAttribute);
    builder.append(enumRestriction);
    return builder.create();
  }

  public static ScalarSequence iconByUrl(DBAttribute<String> iconUrl) {
    return new ScalarSequence.Builder().append(ItemKeys.FEATURE_ICON_BY_URL_ATTRIBUTE).append(iconUrl).create();
  }

  public static void registerFeatures(FeatureRegistry registry) {
    registry.register(FEATURE_NARROW_DEFAULT, SerializableFeature.NoParameters.create(LoadedEnumNarrower.DEFAULT, LoadedEnumNarrower.class));
    registry.register(FEATURE_NARROW_IDENTITY, SerializableFeature.NoParameters.create(LoadedEnumNarrower.IDENTITY, LoadedEnumNarrower.class));
    registry.register(FEATURE_NARROW_SINGLE_ATTRIBUTE, SingleControlling.FEATURE);
    ItemKeys.registerFeatures(registry);
  }
}
