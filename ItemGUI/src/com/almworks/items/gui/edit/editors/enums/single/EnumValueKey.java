package com.almworks.items.gui.edit.editors.enums.single;

import com.almworks.api.application.ItemKey;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.*;
import com.almworks.items.gui.edit.editors.enums.EnumItemCreator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.util.CommonValueKey;
import com.almworks.items.sync.VersionSource;
import com.almworks.items.sync.util.SyncUtils;
import com.almworks.items.sync.util.TransactionCacheKey;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

public class EnumValueKey {
  private final TypedKey<Long> myDBValueKey;
  private final TypedKey<ItemKey> myEnumKey;
  private final CommonValueKey<Long> myCommonValue;
  private final DBAttribute<Long> myAttribute;
  @Nullable
  private final EnumItemCreator myCreator;
  private final TransactionCacheKey<Long> myResolvedValue;
  private final boolean myForbidIllegalCommit;

  public EnumValueKey(DBAttribute<Long> attribute, @Nullable EnumItemCreator creator, boolean forbidIllegalCommit) {
    myAttribute = attribute;
    myCreator = creator;
    myForbidIllegalCommit = forbidIllegalCommit;
    String debugPrefix = myAttribute.getName();
    myDBValueKey = TypedKey.create(debugPrefix + "/db");
    myResolvedValue = TransactionCacheKey.create(debugPrefix + "/resolved");
    myEnumKey = TypedKey.create(debugPrefix + "/key");
    myCommonValue = new CommonValueKey<Long>(debugPrefix + "/common");
  }

  public DBAttribute<Long> getAttribute() {
    return myAttribute;
  }

  public void registerAttribute(EditItemModel model) {
    model.registerSingleEnum(myAttribute, new Convertor<EditModelState, LongList>() {
      @Override
      public LongList convert(EditModelState model) {
        Long value = model.getValue(myDBValueKey);
        if (value != null && value > 0) {
          return LongArray.create(value);
        }
        ItemKey itemKey = model.getValue(myEnumKey);
        long item = itemKey != null ? itemKey.getItem() : 0;
        return item > 0 ? LongArray.create(item) : LongList.EMPTY;
      }
    });
  }

  @Override
  public String toString() {
    return "ValueKey: " + getDebugName();
  }

  public String getDebugName() {
    return myAttribute.toString();
  }

  public boolean isChanged(EditModelState model) {
    long initial = getInitialItemValue(model);
    long current = Math.max(0, Util.NN(model.getValue(myDBValueKey), 0l));
    if (initial != current) return true;
    if (!myCommonValue.isAllSame(model)) return myCommonValue.hasNull(model) || initial != 0;
    if (initial > 0) return false;
    ItemKey initialKey = model.getInitialValue(myEnumKey);
    ItemKey currentKey = model.getValue(myEnumKey);
    return !Util.equals(initialKey, currentKey);
  }

  public long getInitialItemValue(EditModelState model) {
    return Math.max(0, Util.NN(model.getInitialValue(myDBValueKey), 0l));
  }

  public ItemKey getCurrentValue(EditModelState model) {
    return model.getValue(myEnumKey);
  }

  public ItemKey getValue(EditModelState model, EnumVariantsSource variants) {
    ItemKey itemKey = model.getValue(myEnumKey);
    if (itemKey == null) {
      Long dbItem = model.getValue(myDBValueKey);
      if (dbItem != null && dbItem > 0 && variants != null) {
        itemKey = variants.getResolvedItem(model, dbItem);
      }
    }
    return itemKey;
  }

  public void setValue(EditModelState model, @Nullable ItemKey itemKey) {
    long item = itemKey != null ? itemKey.getItem() : 0;
    if (item < 0) item = 0;
    model.putValues(myDBValueKey, item, myEnumKey, itemKey);
  }

  public long commit(CommitContext context, EnumVariantsSource variants) throws CancelCommitException {
    long item = findOrCreate(context);
    if (item > 0 && myForbidIllegalCommit && variants != null) {
      if (SyncUtils.isRemoved(context.readTrunk(item))) item = 0;
      else if (!variants.isValidValueFor(context, item)) item = 0;
    }
    context.getCreator().setValue(myAttribute, item > 0 ? item : null);
    return item;
  }

  public long findOrCreate(CommitContext context) throws CancelCommitException {
    Long item = context.getModel().getValue(myDBValueKey);
    if (item == null || item <= 0) item = myResolvedValue.get(context.getReader());
    if (item != null && item > 0) return item;
    ItemKey itemKey = context.getModel().getValue(myEnumKey);
    if (BaseSingleEnumEditor.NULL_ITEM.equals(itemKey)) return 0;
    String id = itemKey != null ? itemKey.getId() : null;
    if (id == null || id.length() == 0) return 0;
    if (myCreator == null) {
      LogHelper.error("Cannot create enum", id, myAttribute, myCreator);
      throw new CancelCommitException();
    }
    Long cachedItem = context.getModel().getValue(myDBValueKey);
    if (cachedItem != null && cachedItem > 0) return cachedItem;
    long newItem = myCreator.createItem(context, id);
    myResolvedValue.put(context.getReader(), newItem);
    return newItem;
  }

  public void loadValue(VersionSource source, LongList items, EditModelState model, boolean forbidNull) {
    Long enumItem = myCommonValue.loadValue(source, model, myAttribute, items);
    if (forbidNull && myCommonValue.hasNull(model)) enumItem = null;
    model.putHint(myDBValueKey, enumItem);
  }

  public void loadValueCommonValue(EditItemModel model, long defaultValue) {
    myCommonValue.setValueIsCommon(model);
    model.putHint(myDBValueKey, defaultValue);
  }

  public void setNoInitialValue(EditItemModel model) {
    myCommonValue.setValueIsCommon(model);
  }

  public boolean hasValue(EditModelState model) {
    final Long aLong = model.getValue(myDBValueKey);
    return aLong != null && aLong > 0L;
  }

  public ComponentControl.Enabled getComponentEnableState(EditModelState model, boolean forbidNull) {
    return myCommonValue.getComponentEnabledState(model, forbidNull);
  }

  public void setValue(EditModelState model, Long item, ItemKey key) {
    if (item == null || item <= 0) setValue(model, null);
    else if (key != null) setValue(model, key);
    else model.putValues(myDBValueKey, item, myEnumKey, null);
  }

  public long getItemValue(EditModelState model) {
    Long dbValue = model.getValue(myDBValueKey);
    return dbValue == null || dbValue <= 0 ? 0 : dbValue;
  }

  public boolean hasInitialNull(EditModelState model) {
    return myCommonValue.hasNull(model);
  }

  public long getInitialCommonValue(EditModelState model) {
    Long commonValue = myCommonValue.getCommonValue(model);
    return Math.max(0, Util.NN(commonValue, 0l));
  }
}
