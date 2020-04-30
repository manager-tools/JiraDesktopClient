package com.almworks.items.sync.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.sync.*;
import com.almworks.items.sync.impl.BranchUtil;
import com.almworks.items.sync.impl.HolderCache;
import com.almworks.items.sync.impl.SyncSchema;
import com.almworks.items.sync.impl.VersionHolder;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.items.util.SlaveUtils;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncUtils {
  /**
   * Deletes direct slaves referred via specified masterAttribute
   * @param master master item
   * @param masterAttribute slave to master reference
   */
  public static void deleteAllSlaves(ItemVersionCreator master, DBAttribute<Long> masterAttribute) {
    LongList slaves = master.getSlaves(masterAttribute);
    for (int i = 0; i < slaves.size(); i++) master.changeItem(slaves.get(i)).delete();
  }

  @NotNull
  public static ItemVersion readTrunk(DBReader reader, long item) {
    return BranchUtil.instance(reader).readItem(item, false);
  }

  public static ItemVersion readTrunk(DBReader reader, DBIdentifiedObject object) {
    long item = reader.findMaterialized(object);
    return readTrunk(reader, item);
  }

  public static <T> long findOrCreate(DBDrain drain, BoolExpr<DP> expr, DBAttribute<T> attribute, T key,
    Procedure<ItemVersionCreator> initializer)
  {
    final long found = drain.getReader().query(expr).getItemByKey(attribute, key);
    if(found > 0) {
      return found;
    }

    final ItemVersionCreator newItem = drain.createItem();
    initializer.invoke(newItem);
    return newItem.getItem();
  }

  public static void deleteAll(DBDrain drain, LongList items) {
    if (items == null || items.isEmpty()) return;
    for (int i = 0; i < items.size(); i++) drain.changeItem(items.get(i)).delete();
  }

  /**
   * @see com.almworks.items.sync.util.BranchSource#trunk(com.almworks.items.api.DBReader)
   */
  public static List<ItemVersion> readItems(final DBReader reader, final LongList items) {
    if (items == null || items.isEmpty()) return Collections15.emptyList();
    return BranchSource.trunk(reader).readItems(items);
  }

  public static <T> List<ItemVersion> selectItems(List<ItemVersion> items, DBAttribute<T> attr, T value) {
    HashMap<DBAttribute<?>, Object> sample = Collections15.hashMap();
    sample.put(attr, value);
    return selectItems(items, sample);
  }

  public static List<ItemVersion> selectItems(List<ItemVersion> bugs, Map<DBAttribute<?>, Object> sample) {
    List<ItemVersion> candidate = Collections15.arrayList();
    for (ItemVersion bug : bugs) {
      boolean matches = true;
      for (Map.Entry<DBAttribute<?>, Object> entry : sample.entrySet()) {
        if (!DatabaseUtil.isEqualValue((DBAttribute)entry.getKey(), entry.getValue(), bug.getValue(entry.getKey()))) {
          matches = false;
          break;
        }
      }
      if (matches) candidate.add(bug);
    }
    return candidate;
  }

  /**
   * @see #getSlavesSubtree(com.almworks.items.api.DBReader, long)
   */
  public static LongList getSlavesSubtrees(DBReader reader, LongList masters) {
    if (masters == null || masters.isEmpty()) return LongList.EMPTY;
    List<DBAttribute<Long>> masterAttributes = SlaveUtils.getMasterAttributes(reader);
    LongSet result = new LongSet();
    for (int i = 0; i < masters.size(); i++) {
      long master = masters.get(i);
      if (result.contains(master)) continue;
      result.add(master);
      collectSlavesRecursive(reader, master, result, masterAttributes);
    }
    return result;
  }

  /**
   * @return all slaves of the given item and the item itself
   */
  @NotNull
  public static LongList getSlavesSubtree(DBReader reader, long item) {
    LongSet result = new LongSet();
    result.add(item);
    collectSlavesRecursive(reader, item, result, SlaveUtils.getMasterAttributes(reader));
    return result;
  }

  public static void collectSlavesRecursive(DBReader reader, long item, LongSet target,
    List<DBAttribute<Long>> masterAttributes)
  {
    if (masterAttributes == null || masterAttributes.isEmpty()) return;
    List<BoolExpr<DP>> exprs = Collections15.arrayList();
    for (DBAttribute<Long> attribute : masterAttributes) exprs.add(DPEquals.create(attribute, item));
    LongArray slaves = reader.query(BoolExpr.or(exprs)).copyItemsSorted();
    for (int i = 0; i < slaves.size(); i++) {
      long slave = slaves.get(i);
      if (target.contains(slave)) continue;
      target.add(slave);
      collectSlavesRecursive(reader, slave, target, masterAttributes);
    }
  }

  @Nullable
  public static ItemVersion readBaseIfExists(DBReader reader, long item) {
    return BranchUtil.readServerShadow(reader, item, SyncSchema.BASE, false);
  }

  public static ItemVersion readConflictIfExists(DBReader reader, long item) {
    return BranchUtil.readServerShadow(reader, item, SyncSchema.CONFLICT, false);
  }

  @Nullable
  public static ItemVersion readServerIfExists(DBReader reader, long item) {
    return BranchUtil.instance(reader).readServerIfExists(item);
  }

  @NotNull
  public static ItemVersion readServer(DBReader reader, long item) {
    return BranchUtil.instance(reader).readItem(item, true);
  }

  private static final List<DBAttribute<AttributeMap>> SERVER_NOT_BASE_SHADOWS =
    Collections15.unmodifiableListCopy(SyncSchema.DOWNLOAD, SyncSchema.CONFLICT);
  @Nullable
  public static ItemVersion readLastNotBaseServer(DBReader reader, long item) {
    for (DBAttribute<AttributeMap> shadow : SERVER_NOT_BASE_SHADOWS) {
      AttributeMap map = reader.getValue(item, shadow);
      if (map != null) return new MapItemVersion(map, readServer(reader, item));
    }
    return null;
  }

  public static boolean isNew(long item, DBReader reader) {
    if (item <= 0) return false;
    HolderCache cache = HolderCache.instance(reader);
    VersionHolder holder = cache.getHolder(item, SyncSchema.BASE, false);
    if (holder == null || !Boolean.TRUE.equals(holder.getValue(SyncSchema.INVISIBLE))) return false;
    return cache.getHolder(item, SyncSchema.DOWNLOAD, false) == null && cache.getHolder(item, SyncSchema.CONFLICT, false) == null;
  }

  public static boolean isTrunkInvisible(DBReader reader, long item) {
    return isInvisible(item, reader, null);
  }

  private static boolean isInvisible(long item, DBReader reader, @Nullable DBAttribute<AttributeMap> shadow) {
    if (item <= 0) return false;
    VersionHolder holder = HolderCache.instance(reader).getHolder(item, shadow, false);
    return holder != null && Boolean.TRUE.equals(holder.getValue(SyncSchema.INVISIBLE));
  }

  @Nullable
  public static ItemVersion getLatestServer(long item, DBReader reader) {
    return BranchUtil.instance(reader).readServerIfExists(item);
  }

  @SuppressWarnings({"unchecked"})
  public static void copyValues(ItemVersionCreator creator, AttributeMap values) {
    for (DBAttribute attribute : values.keySet()) {
      creator.setValue(attribute, values.get(attribute));
    }
  }

  @SuppressWarnings({"unchecked"})
  public static void copyValues(ItemVersionCreator creator, Map<DBAttribute<?>, Object> values) {
    for (Map.Entry<DBAttribute<?>, Object> entry : values.entrySet()) {
      Object value = entry.getValue();
      DBAttribute<?> attribute = entry.getKey();
      if (value instanceof DBIdentifiedObject) creator.setValue(((DBAttribute<Long>) attribute), (DBIdentifiedObject)value);
      else creator.setValue((DBAttribute<Object>) attribute, value);
    }
  }

  /**
   * Notifies that all items of the given type are uploaded and should be unlocked.<br>
   * Use this method for items which do not require special post-upload check and can detect successful upload via
   * downloaded state inspection.
   * @param drain upload drain
   * @param type items type
   */
  public static void setAllUploaded(UploadDrain drain, DBItemType type) {
    for (ItemVersion item : drain.readItems(drain.getLockedForUpload())) {
      if (item.equalValue(DBAttribute.TYPE, type)) drain.setAllDone(item.getItem());
    }
  }

  public static boolean isRemoved(ItemVersion item) {
    return item == null || item.getValue(SyncAttributes.EXISTING) == null;
  }

  public static <T> T mapValue(DBReader reader, Long value, Map<? extends DBIdentifiedObject, T> map) {
    if (value == null || value <= 0) return null;
    for (Map.Entry<? extends DBIdentifiedObject, T> entry : map.entrySet()) {
      if (equalValue(reader, value, entry.getKey())) return entry.getValue();
    }
    return null;
  }

  public static boolean equalValue(DBReader reader, Long item, DBIdentifiedObject object) {
    if (item == null || item <= 0) return false;
    long materialized = reader.findMaterialized(object);
    return materialized == item;
  }

  public static <T> boolean isEqualValueInMap(DBReader reader, DBAttribute<T> attribute, AttributeMap map1, AttributeMap map2) {
    T value1 = map1 != null ? map1.get(attribute) : null;
    T value2 = map2 != null ? map2.get(attribute) : null;
    return isEqualValue(reader, attribute, value1, value2);
  }

  public static <T> boolean isEqualValue(DBReader reader, DBAttribute<T> attribute, T value1, T value2) {
    if (DatabaseUtil.isEqualValue(attribute, value1, value2)) return true;
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    if (composition != DBAttribute.ScalarComposition.SCALAR) return false;
    if (attribute.getScalarClass() != BigDecimal.class) return false;
    DBAttribute<BigDecimal> decAttr = (DBAttribute<BigDecimal>) attribute;
    BigDecimal dec1 = (BigDecimal) value1;
    BigDecimal dec2 = (BigDecimal) value2;
    return isDecimalEquals(reader, decAttr, dec1, dec2);
  }

  public static boolean isDecimalEquals(DBReader reader, DBAttribute<BigDecimal> attribute, BigDecimal value1,
    BigDecimal value2) {
    if (Util.equals(value1, value2)) return true;
    if (value1 == null || value2 == null) return false;
    long attr = reader.findMaterialized(attribute);
    Integer scale = reader.getValue(attr, SyncSchema.DECIMAL_SCALE);
    if (scale == null) return false;
    int scale1 = value1.scale();
    int scale2 = value2.scale();
    if (scale1 == scale2 && scale >= scale1) return false;
    int commonScale = Math.min(scale, Math.max(scale1, scale2));
    BigDecimal v1 = value1.setScale(commonScale, BigDecimal.ROUND_HALF_UP);
    BigDecimal v2 = value2.setScale(commonScale, BigDecimal.ROUND_HALF_UP);
    return v1.equals(v2);
  }

  public static ItemDiff diffTrunkToServer(ItemVersion trunk) {
    if (trunk == null) throw new NullPointerException();
    ItemVersion server = trunk.switchToServer();
    return ItemDiffImpl.createToTrunk(server, 0);
  }
}
