package com.almworks.tags;

import com.almworks.api.application.ItemDownloadStage;
import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ModelKey;
import com.almworks.integers.LongArray;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPIntersects;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedModelKey;
import com.almworks.items.gui.meta.schema.enums.EnumType;
import com.almworks.items.gui.meta.schema.modelkeys.ModelKeys;
import com.almworks.items.gui.meta.util.EnumTypeBuilder;
import com.almworks.items.gui.meta.util.MultiEnumInfo;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.DownloadProcedure;
import com.almworks.items.sync.SyncManager;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.sync.util.identity.DBStaticObject;
import com.almworks.items.sync.util.identity.ScalarSequence;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.LogHelper;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.LongObjFunction2;
import com.almworks.util.properties.Role;
import org.picocontainer.Startable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TagsComponentImpl implements Startable {
  public static final Role<TagsComponentImpl> ROLE = Role.role("FC");

  public static final DBNamespace TAG_NS = TagsFeatures.NS.subNs("tag");

  /**
   * Type of a tag item.
   */
  public static final DBItemType TYPE_TAG = TAG_NS.type();
  /**
   * Attribute of tag item.
   */
  public static final DBAttribute<String> ICON_PATH = TAG_NS.string("iconPath", "Icon Path", false);
  /**
   * Attribute that is set on a taggable item, contains item's tags.
   */
  public static final DBAttribute<Set<Long>> TAGS_ATTRIBUTE = TagsFeatures.NS.linkSet("tags", "Tags", false);

  public static final DBIdentity OWNER = DBIdentity.fromDBObject(TagsFeatures.NS.object("tagsOwner"));
  private static final ScalarSequence KEY_DATA_LOADER = ScalarSequence.create(TagsFeatures.FEATURE_KEY_DATA_LOADER);
  private static final DBStaticObject CONSTRAINT;
  public static final DBStaticObject VIEWER;
  private static final DBStaticObject DND;
  private static final DBStaticObject MODEL_KEY;
  static final DBStaticObject ENUM_TYPE = new EnumTypeBuilder()
    .setType(TYPE_TAG)
    .setUniqueKey(DBAttribute.NAME)
    .setNarrower(EnumType.NARROW_IDENTITY)
    .renderFirstNotNull(DBAttribute.NAME)
    .setIconLoader(new ScalarSequence.Builder().append(TagsFeatures.FEATURE_ICON_FROM_RESOURCE).append(ICON_PATH).create())
    .addAttributeSubloaders(ICON_PATH)
    .create();

  static {
    MultiEnumInfo tags = new MultiEnumInfo()
      .setOwner(OWNER)
      .setEnumType(ENUM_TYPE)
      .setHideEmptyLeftField(true)
      .setMultiIconTextField()
      .setRendererMinStage(ItemDownloadStage.DUMMY)
      .setId("$tags")
      .setDisplayName("Tags")
      .setAttribute(TAGS_ATTRIBUTE)
      .setDataPromotion(ModelKeys.SEQUENCE_PROMOTION_ALWAYS)
      .overrideKeyDataLoader(KEY_DATA_LOADER)
      .setConstraintIcon(ScalarSequence.create(TagsFeatures.FEATURE_TAGS_ICON))
      .setDefaultDnD(null);
    CONSTRAINT = tags.createDescriptor();
    VIEWER = tags.createViewerField();
    DND = tags.createDnDChange();
    MODEL_KEY = tags.createModelKey();
  }

  private final SyncManager myManager;

  public TagsComponentImpl(SyncManager manager) {
    myManager = manager;
  }

  public void start() {
    myManager.writeDownloaded(new DownloadProcedure<DBDrain>() {
      @Override
      public void write(DBDrain drain) throws DBOperationCancelledException {
        drain.materialize(CONSTRAINT);
        drain.materialize(VIEWER);
        drain.materialize(DND);
      }

      @Override
      public void onFinished(DBResult<?> result) {
      }
    });
  }

  public void stop() {
  }

  public LongArray getAllTags(DBReader reader) {
    return reader.query(DPEqualsIdentified.create(DBAttribute.TYPE, TYPE_TAG)).copyItemsSorted();
  }

  public static void deleteTag(DBWriter writer, long tagItem) {
    deleteTag(writer, tagItem, null);
  }

  public static void deleteTag(final DBWriter writer, final long tagItem, BoolExpr<DP> taggedItems) {
    if (taggedItems == null) {
      taggedItems = DPIntersects.create(TAGS_ATTRIBUTE, Collections.singleton(tagItem));
    }
    writer.query(taggedItems).fold(null, new LongObjFunction2<Object>() {
      @Override
      public Object invoke(long primaryItem, Object b) {
        Set<Long> value = writer.getValue(primaryItem, TAGS_ATTRIBUTE);
        if (value != null) {
          value.remove(tagItem);
          writer.setValue(primaryItem, TAGS_ATTRIBUTE, value);
        } else assert false : primaryItem; // query is bad?
        return null;
      }
    });
    DatabaseUnwrapper.clearItem(writer, tagItem);
  }

  public static ModelKey<List<ItemKey>> getModelKey(GuiFeaturesManager features) {
    LoadedModelKey<?> key = features.findModelKey(MODEL_KEY);
    LoadedModelKey<List<ItemKey>> result = key != null ? key.castList(ItemKey.class) : null;
    LogHelper.assertError(result != null, "Missing Tags ModelKey", key);
    return result;
  }
}
