package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.ItemReference;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public abstract class EnumTypeProvider {
  public void prepare(VersionSource source, EditModelState model) {
    EngineConsts.ensureGuiFeatureManager(source, model);
    doPrepare(source, model);
  }

  @Nullable
  public EnumTypesCollector.Loaded getEnumType(EditModelState model) {
    GuiFeaturesManager features = EngineConsts.getGuiFeaturesManager(model);
    EnumTypesCollector typesCollector = features != null ? features.getEnumTypes() : null;
    long typeItem = getEnumTypeItem(model);
    if (typesCollector == null) {
      LogHelper.error("Missing enumType", this, model);
      return null;
    }
    EnumTypesCollector.Loaded enumType = typesCollector.getType(typeItem);
    LogHelper.assertError(enumType != null, "Missing enumType", this, model);
    return enumType;
  }

  public abstract long getEnumTypeItem(EditModelState model);

  protected void doPrepare(VersionSource source, EditModelState model) {}

  public LoadedItemKey getResolvedItem(EditModelState model, long item) {
    if (item <= 0) return null;
    EnumTypesCollector.Loaded enumType = getEnumType(model);
    if (enumType == null) return null;
    return enumType.getResolvedItem(item);
  }

  public boolean isValidValueFor(CommitContext context, long itemValue) {
    long enumType = getEnumTypeItem(context.getModel());
    if (enumType <= 0) {
      LogHelper.error("Missing enum type", this);
      return true;
    }
    LoadedEnumNarrower narrower = EnumTypesCollector.getNarrower(context.getReader(), enumType);
    LogHelper.assertError(narrower != null, "Missing narrower", enumType, this);
    return narrower == null || narrower.isAllowedValue(context.readTrunk(), context.readTrunk(itemValue));
  }

  public ArrayList<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
    EnumTypesCollector.Loaded enumType = getEnumType(model);
    LoadedEnumNarrower narrower = enumType != null ? enumType.getNarrower() : LoadedEnumNarrower.DEFAULT;
    HashSet<DBAttribute<Long>> attributes = Collections15.hashSet();
    narrower.collectIssueAttributes(attributes);
    attributes.remove(SyncAttributes.CONNECTION);
    ArrayList<ItemKey> invalids = Collections15.arrayList();
    if (attributes.isEmpty()) return invalids;
    ItemHypercube cube = model.collectHypercube(attributes);
    for (ItemKey item : items) {
      LoadedItemKey loaded = Util.castNullable(LoadedItemKey.class, item);
      if (loaded == null) {
        LogHelper.assertError(!(item instanceof ResolvedItem), "Expected LoadedItemKey", item);
        continue;
      }
      if (narrower.isAccepted(cube, loaded)) continue;
      if (invalids.contains(item)) continue;
      invalids.add(item);
    }
    return invalids;
  }

  public static class DynamicEnum extends EnumTypeProvider {
    private final long myEnumTypeItem;

    public DynamicEnum(long enumTypeItem) {
      myEnumTypeItem = enumTypeItem;
    }

    @Override
    public long getEnumTypeItem(EditModelState model) {
      return myEnumTypeItem;
    }
  }

  public static class StaticEnum extends EnumTypeProvider {
    private final TypedKey<Long> myTypeKey;
    private final ItemReference myEnumType;

    public StaticEnum(ItemReference enumType, String debugName) {
      myTypeKey = TypedKey.create(debugName + "/enumType");
      myEnumType = enumType;
    }

    @Override
    public long getEnumTypeItem(EditModelState model) {
      return Util.NN(model.getValue(myTypeKey), 0l);
    }

    @Override
    protected void doPrepare(VersionSource source, EditModelState model) {
      long enumType = myEnumType.findItem(source.getReader());
      if (enumType <= 0) LogHelper.error("Failed to resolve", myEnumType);
      model.putHint(myTypeKey, enumType);
    }
  }

  public static abstract class Source implements EnumVariantsSource {
    private final EnumTypeProvider myEnumType;

    protected Source(EnumTypeProvider enumType) {
      myEnumType = enumType;
    }

    @Override
    public void prepare(VersionSource source, EditModelState model) {
      myEnumType.prepare(source, model);
    }

    @Nullable
    @Override
    public LoadedItemKey getResolvedItem(EditModelState model, long item) {
      return myEnumType.getResolvedItem(model, item);
    }

    @NotNull
    @Override
    public List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
      return myEnumType.selectInvalid(model, items);
    }

    @Override
    public boolean isValidValueFor(CommitContext context, long itemValue) {
      return myEnumType.isValidValueFor(context, itemValue);
    }

    @Nullable
    public EnumTypesCollector.Loaded getEnumType(EditModelState model) {
      return myEnumType.getEnumType(model);
    }

    @NotNull
    protected AListModel<LoadedItemKey> getValueModel(Lifespan life, EditItemModel model, ItemHypercube cube) {
      EnumTypesCollector.Loaded enumType = getEnumType(model);
      //noinspection unchecked
      return enumType != null ? enumType.getValueModel(life, cube) : AListModel.EMPTY;
    }
  }
}
