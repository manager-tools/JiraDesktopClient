package com.almworks.jira.provider3.gui.edit.editors;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.engine.Connection;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.enums.EnumModelConfigurator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEditorBuilder;
import com.almworks.items.gui.edit.editors.enums.single.DropdownEnumEditor;
import com.almworks.items.gui.edit.editors.enums.single.ItemRenderer;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.schema.Group;
import com.almworks.jira.provider3.schema.ProjectRole;
import com.almworks.spi.provider.AbstractConnection;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SegmentedListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.config.Configuration;
import com.almworks.util.text.NameMnemonic;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class VisibilityEditor implements EnumVariantsSource {
  public static final EnumVariantsSource VARIANTS = new VisibilityEditor();
  public static final String VISIBLE_TO_ALL = "All Users";
  private static final CanvasRenderable.TextRenderable NULL_RENDERABLE = new CanvasRenderable.TextRenderable(Font.ITALIC, VISIBLE_TO_ALL);
  public static final CanvasRenderer<ItemKey> RENDERER = new ItemRenderer(NULL_RENDERABLE, null);

  private final TypedKey<Long> myGroupsType = TypedKey.create("visibility/groups");
  private final TypedKey<Long> myRolesType = TypedKey.create("visibility/roles");

  public static DropdownEnumEditor create(DBAttribute<Long> attribute) {
    return new DropdownEditorBuilder()
      .setLabelText(NameMnemonic.parseString("Visibilit&y Level"))
      .setAppendNull(true)
      .setVariants(VARIANTS)
      .setAttribute(attribute)
      .setNullRenderable(NULL_RENDERABLE)
      .createFixed();
  }

  @Override
  public void prepare(VersionSource source, EditModelState model) {
    if (model.getValue(myGroupsType) != null && model.getValue(myRolesType) != null) return;
    EngineConsts.ensureGuiFeatureManager(source, model);
    long groupsType = Group.ENUM_TYPE.findItem(source.getReader());
    long rolesType = ProjectRole.ENUM_TYPE.findItem(source.getReader());
    LogHelper.assertError(groupsType > 0 && rolesType > 0, "Missing enum type", groupsType, rolesType);
    model.putHint(myRolesType, rolesType);
    ItemVersion connectionIv = source.forItem(model.getValue(EngineConsts.VALUE_CONNECTION).getConnectionItem());
    Boolean rolesOnly = connectionIv.getValue(ProjectRole.PROJECT_ROLES_ONLY);
    if (rolesOnly == null || !rolesOnly)
      model.putHint(myGroupsType, groupsType);
  }

  @Override
  public void configure(Lifespan life, EditItemModel model, VariantsAcceptor<ItemKey> acceptor) {
    final EnumTypesCollector.Loaded groups = getEnumType(model, myGroupsType);
    final EnumTypesCollector.Loaded roles = getEnumType(model, myRolesType);
    if (roles == null) {
      LogHelper.error("Missing roles enum type", roles);
      return;
    }
    new EnumModelConfigurator(model, acceptor) {
      @Override
      protected void collectCubeAttributes(HashSet<DBAttribute<Long>> attributes) {
        roles.getNarrower().collectIssueAttributes(attributes);
        if (groups != null)
          groups.getNarrower().collectIssueAttributes(attributes);
      }

      @Override
      protected void updateVariants(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants, EditItemModel model, UserDataHolder data) {
        Connection connection = model.getValue(EngineConsts.VALUE_CONNECTION);
        Configuration config = connection == null ? Configuration.EMPTY_CONFIGURATION :
          connection.getConnectionConfig(AbstractConnection.RECENTS, "visibilityLevel");
        acceptor.accept(variants, config);
      }

      @Override
      protected AListModel<LoadedItemKey> getSortedVariantsModel(Lifespan life, EditItemModel model, ItemHypercube cube) {
        return VisibilityEditor.this.getVariantsModel(life, cube, groups, roles);
      }
    }.start(life);
  }

  private EnumTypesCollector.Loaded getEnumType(EditModelState model, TypedKey<Long> typeKey) {
    Long type = model.getValue(typeKey);
    if (type == null || type <= 0) return null;
    GuiFeaturesManager manager = EngineConsts.getGuiFeaturesManager(model);
    return manager != null ? manager.getEnumTypes().getType(type) : null;
  }

  @Override
  public LoadedItemKey getResolvedItem(EditModelState model, long item) {
    LoadedItemKey key = getResolvedItem(model, myGroupsType, item);
    if (key == null) key = getResolvedItem(model, myRolesType, item);
    return key;
  }

  @Nullable
  private LoadedItemKey getResolvedItem(EditModelState model, TypedKey<Long> typeKey, long item) {
    EnumTypesCollector.Loaded enumType = getEnumType(model, typeKey);
    return enumType != null ? enumType.getResolvedItem(item) : null;
  }

  private AListModel<LoadedItemKey> getVariantsModel(Lifespan life, ItemHypercube cube, @Nullable EnumTypesCollector.Loaded groups, @Nullable EnumTypesCollector.Loaded roles) {
    SegmentedListModel<LoadedItemKey> result = SegmentedListModel.create(life);
    if (roles != null) {
      AListModel<LoadedItemKey> unsortedModel = roles.getValueModel(life, cube);
      result.addSegment(SortedListDecorator.create(life, unsortedModel, ItemKey.COMPARATOR));
    }
    if (groups != null) {
      AListModel<LoadedItemKey> unsortedModel = groups.getValueModel(life, cube);
      result.addSegment(SortedListDecorator.create(life, unsortedModel, ItemKey.COMPARATOR));
    }
    return result;
  }

  @NotNull
  @Override
  public List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
    ArrayList<ItemKey> invalids = Collections15.arrayList();
    selectInvalid(model, items, invalids, myGroupsType, Group.DB_TYPE);
    selectInvalid(model, items, invalids, myRolesType, ProjectRole.DB_TYPE);
    return invalids;
  }

  private void selectInvalid(EditModelState model, Collection<? extends ItemKey> items, ArrayList<ItemKey> target, TypedKey<Long> typeKey, DBItemType itemType) {
    EnumTypesCollector.Loaded enumType = getEnumType(model, typeKey);
    if (enumType == null) return;
    LoadedEnumNarrower narrower = enumType.getNarrower();
    HashSet<DBAttribute<Long>> attributes = Collections15.hashSet();
    narrower.collectIssueAttributes(attributes);
    attributes.remove(SyncAttributes.CONNECTION);
    if (attributes.isEmpty()) return;
    ItemHypercube cube = model.collectHypercube(attributes);
    for (ItemKey item : items) {
      LoadedItemKey loaded = Util.castNullable(LoadedItemKey.class, item);
      if (loaded == null) {
        LogHelper.assertError(!(item instanceof ResolvedItem), "Expected LoadedItemKey", item);
        continue;
      }
      if (!itemType.equals(loaded.getType())) continue;
      if (narrower.isAccepted(cube, loaded)) continue;
      if (target.contains(item)) continue;
      target.add(item);
    }
  }

  @Override
  public boolean isValidValueFor(CommitContext context, long itemValue) {
    ItemVersion valueVersion = context.getDrain().forItem(itemValue);
    Long type = valueVersion.getValue(DBAttribute.TYPE);
    if (type == null || type <= 0) return false;
    DBReader reader = context.getReader();
    TypedKey<Long> typedKey;
    if (type == reader.findMaterialized(Group.DB_TYPE)) typedKey = myGroupsType;
    else if (type == reader.findMaterialized(ProjectRole.DB_TYPE)) typedKey = myRolesType;
    else {
      LogHelper.error("Unknown type", type, reader.getAttributeMap(type));
      return false;
    }
    Long enumType = context.getModel().getValue(typedKey);
    if (enumType == null || enumType <= 0) {
      LogHelper.error("Missing enum type", typedKey, enumType);
      return true; // Probably the item value is valid
    }
    LoadedEnumNarrower narrower = EnumTypesCollector.getNarrower(reader, enumType);
    if (narrower == null) {
      LogHelper.error("Missing narrower", typedKey, type);
      return true;
    }
    return narrower.isAllowedValue(context.readTrunk(), valueVersion);
  }
}
