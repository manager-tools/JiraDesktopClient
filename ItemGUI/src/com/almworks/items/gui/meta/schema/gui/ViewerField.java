package com.almworks.items.gui.meta.schema.gui;

import com.almworks.api.application.ModelMap;
import com.almworks.api.engine.DBCommons;
import com.almworks.engine.gui.Formlet;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.util.BlobLongListAttribute;
import com.almworks.items.gui.meta.MetaModule;
import com.almworks.items.gui.meta.commons.FeatureRegistry;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.DataHolder;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.components.renderer.RendererContext;
import com.almworks.util.components.renderer.table.TableRenderer;
import com.almworks.util.components.renderer.table.TableRendererLine;
import com.almworks.util.config.Configuration;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class ViewerField {
  private static final DBNamespace NS = MetaModule.NS.subNs("viewerField");
  private static final DBNamespace NS_FEATURE = NS.subNs("feature");
  private static final DBAttribute<String> ID = NS.string("name");
  public static final DBItemType DB_TYPE = NS.type();
  /**
   * Serialize {@link ViewerField}
   */
  public static final DBAttribute<byte[]> BEHAVIOUR = NS.bytes("behaviour");
  /**
   * Identity attribute for fields associated with single model key
   */
  private static final DBAttribute<Long> MODEL_KEY = NS.link("modelKey");

  static final DBIdentity FEATURE_BEHAVIOUR_SEPARATOR = feature("separator");
  static final DBIdentity FEATURE_BEHAVIOUR_SINGLE_ENUM = feature("singleEnum");
  static final DBIdentity FEATURE_BEHAVIOUR_MULTI_ENUM = feature("multiEnum");
  static final DBIdentity FEATURE_BEHAVIOUR_INLINE_MULTI_ENUM = feature("labels");
  public static final DBIdentity FEATURE_BEHAVIOUR_TEXT = feature("text");
  public static final DBIdentity FEATURE_BEHAVIOUR_DECIMAL_NOT_ZERO = feature("decimal");
  public static final DBIdentity FEATURE_BEHAVIOUR_DECIMAL = feature("decimalOrZero");
  public static final DBIdentity FEATURE_BEHAVIOUR_INTEGER = feature("integer");
  public static final DBIdentity FEATURE_BEHAVIOUR_DAY = feature("day");
  public static final DBIdentity FEATURE_BEHAVIOUR_DATE_TIME = feature("dateTime");
  public static final DBIdentity FEATURE_BEHAVIOUR_URL = feature("url");
  public static final DBIdentity FEATURE_BEHAVIOUR_SECONDS_DURATION = feature("secondsDuration");
  static final DBIdentity FEATURE_CELL_CONVERTOR_LABEL = feature("cellConvertor.label");
  static final DBIdentity FEATURE_CELL_CONVERTOR_ICON_TEXT = feature("cellConvertor.iconText");
  public static final ScalarSequence CELL_LABEL = new ScalarSequence.Builder().append(FEATURE_CELL_CONVERTOR_LABEL).create();
  public static final ScalarSequence CELL_ICON_TEXT = new ScalarSequence.Builder().append(FEATURE_CELL_CONVERTOR_ICON_TEXT).create();

  private static DBIdentity feature(String id) {
    return DBIdentity.fromDBObject(NS_FEATURE.object(id));
  }

  public static final DBStaticObject SEPARATOR;
  static {
    SEPARATOR = new DBStaticObject.Builder()
      .putReference(DBCommons.OWNER.getAttribute(), DBCommons.PLATFORM)
      .put(ID, "field.separator")
      .putReference(DBAttribute.TYPE, DB_TYPE)
      .putSequence(BEHAVIOUR, new ScalarSequence.Builder().append(FEATURE_BEHAVIOUR_SEPARATOR).create())
      .create(DBIdentity.fromDBObject(NS.object("field.separator")));
  }

  public static final DataLoader<ViewerField> LOADER = SerializedObjectAttribute.create(ViewerField.class, BEHAVIOUR);
  public static final BlobLongListAttribute CONNECTION_FIELDS = new BlobLongListAttribute(NS.bytes("connectionFields"));

  public static ScalarSequence singleEnum(DBStaticObject modelKey, String displayName, boolean hideEmpty, boolean isMultiline) {
    return behavior(FEATURE_BEHAVIOUR_SINGLE_ENUM, modelKey, displayName, hideEmpty, isMultiline);
  }

  public static ScalarSequence multiEnum(DBStaticObject modelKey, String displayName, boolean hideEmpty, boolean isMultiline, ScalarSequence cellConvertor) {
    return buildBehaviour(FEATURE_BEHAVIOUR_MULTI_ENUM, modelKey, displayName, hideEmpty, isMultiline).appendSubsequence(cellConvertor).create();
  }

  public static ScalarSequence inlineMultiEnum(DBStaticObject modelKey, String displayName, boolean hideEmpty, boolean isMultiline) {
    return behavior(FEATURE_BEHAVIOUR_INLINE_MULTI_ENUM, modelKey, displayName, hideEmpty, isMultiline);
  }

  public static ScalarSequence behavior(DBIdentity feature, DBStaticObject modelKey, String displayName, boolean hideEmpty, boolean isMultiline) {
    return buildBehaviour(feature, modelKey, displayName, hideEmpty, isMultiline).create();
  }

  private static ScalarSequence.Builder buildBehaviour(DBIdentity feature, DBStaticObject modelKey, String displayName, boolean hideEmpty, boolean isMultiline) {
    return new ScalarSequence.Builder()
      .append(feature)
      .append(modelKey)
      .append(displayName)
      .append(hideEmpty)
      .append(isMultiline);
  }

  public static void registerFeatures(FeatureRegistry features) {
    ViewerFieldsLoaders.registerFeatures(features);
  }

  protected final DataHolder myData;

  protected ViewerField(TypedKey<?>[] keys) {
    myData = new DataHolder(keys);
  }

  protected ViewerField(DataHolder data) {
    myData = data;
  }

  @NotNull
  public abstract List<? extends TableRendererLine> createLeftFieldLines(TableRenderer renderer, RendererContext context);

  @Nullable
  public abstract RightField createRightField(Configuration settings);

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    ViewerField other = Util.castNullable(ViewerField.class, obj);
    return other != null && myData.equals(other.myData);
  }

  @Override
  public int hashCode() {
    return ViewerField.class.hashCode() ^ myData.hashCode();
  }

  public boolean updateFrom(ViewerField updatedField) {
    LogHelper.assertNotNull(updatedField);
    if (updatedField != null && canUpdateFrom(updatedField))
      return myData.update(updatedField.myData);
    else return false;
  }

  protected abstract boolean canUpdateFrom(ViewerField field);

  private static DBIdentity createModelKeyId(DBIdentity owner, DBStaticObject modelKey) {
    return new DBIdentity.Builder().put(MODEL_KEY, modelKey).put(DBAttribute.TYPE, DB_TYPE).create();
  }

  public static DBStaticObject create(DBIdentity owner, DBStaticObject modelKey, ScalarSequence behaviour) {
    return new DBStaticObject.Builder()
      .putSequence(ViewerField.BEHAVIOUR, behaviour)
      .create(ViewerField.createModelKeyId(owner, modelKey));
  }

  public interface RightField {
    Formlet getFormlet();

    void connectUI(Lifespan life, ModelMap model);

    String getDisplayName();
  }

}
