package com.almworks.items.entities.api.collector.typetable;

import com.almworks.integers.IntArray;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import gnu.trove.TLongObjectHashMap;
import gnu.trove.TLongObjectProcedure;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class GenericTable implements PriEntityTable {
  private final EntityCollector2 myCollector;
  private final EntityValueTable myTable;
  private final TLongObjectHashMap<EntityPlace> myByItem = new TLongObjectHashMap<>();
  private final ResolutionTable[] myResolutions;
  private final Boolean[] myMutableResolution;
  private final Entity myType;
  private final int mySearchResolutionIndex;

  private GenericTable(EntityCollector2 collector, EntityResolution resolution, EntityKey<?>[] identityKeys, Entity type) {
    myCollector = collector;
    myType = type;
    myTable = new EntityValueTable(createAll(collector, identityKeys));
    Collection<Collection<EntityKey<?>>> identities = resolution.getIdentities();
    Collection<Collection<EntityKey<?>>> searchBy = resolution.getSearchBy();
    myResolutions = new ResolutionTable[identities.size() + searchBy.size()];
    myMutableResolution = new Boolean[identities.size()];
    int i = 0;
    for (Collection<EntityKey<?>> identity : identities) {
      myResolutions[i] = new ResolutionTable(this, identity);
      i++;
    }
    mySearchResolutionIndex = i;
    for (Collection<EntityKey<?>> keys : searchBy) {
      myResolutions[i] = new ResolutionTable(this, keys);
      i++;
    }
  }

  private static List<KeyInfo> createAll(EntityCollector2 collector2, EntityKey<?>[] identityKeys) {
    ArrayList<KeyInfo> result = Collections15.arrayList();
    for (EntityKey<?> key : identityKeys) {
      KeyInfo keyInfo = collector2.getOrCreateKey(key);
      if (keyInfo != null) result.add(keyInfo);
    }
    return result;
  }

  @Nullable
  public static GenericTable create(EntityCollector2 collector, Entity type) {
    if (type == null) return null;
    EntityResolution resolution = type.get(EntityResolution.KEY);
    if (resolution == null) {
      LogHelper.error("Missing resolution", type);
      return null;
    }
    Collection<EntityKey<?>> allKeys = resolution.getAllKeys();
    if (allKeys.isEmpty()) {
      LogHelper.error("Empty identity resolution", type);
      return null;
    }
    return new GenericTable(collector, resolution, allKeys.toArray(new EntityKey[allKeys.size()]), type);
  }

  EntityPlace identify(Entity sourceEntity) {
    if (sourceEntity == null) return null;
    Long item = sourceEntity.get(StoreBridge.ITEM_ID);
    ValueRow row = new ValueRow(myCollector);
    row.setColumns(myTable.getIdentityKeys());
    row.copyValues(sourceEntity);
    if (item != null) return identifyByItem(row, item);
    EntityPlace place = tryFind(row);
    return place != null ? place : createNew(row, false);
  }

  public EntityPlace identify(ValueRow entityRow) {
    EntityPlace place = tryFind(entityRow);
    if (place != null) return place;
    return createNew(entityRow.copyColumnItersection(myTable.getIdentityKeys()), false);
  }

  private EntityPlace identifyByItem(ValueRow entity, Long item) {
    if (entity == null || item == null || item <= 0) {
      LogHelper.error("Wrong item identity", entity, item);
      return null;
    }
    EntityPlace place = myByItem.get(item);
    if (place != null) return place;
    place = tryFind(entity);
    if (place == null) place = createNew(entity, true);
    if (place != null) myByItem.put(item, place);
    return place;
  }

  private boolean isIdentifiable(ValueRow entity) {
    for (ResolutionTable resolution : myResolutions) if (resolution.hasAllValues(entity)) return true;
    return false;
  }

  private EntityPlace createNew(ValueRow entity, boolean allowNoIdentity) {
    if (!allowNoIdentity && !isIdentifiable(entity)) {
      LogHelper.error("Missing any identity", entity);
      return null;
    }
    EntityPlace place;
    int placeIndex = myTable.addNewRow();
    myTable.copyValues(placeIndex, entity, false);
    place = new EntityPlace(this, placeIndex);
    for (ResolutionTable resolution : myResolutions) resolution.addNew(place);
    return place;
  }

  @Nullable
  public EntityPlace tryFind(ValueRow entity) {
    for (ResolutionTable resolution : myResolutions) {
      EntityPlace row = resolution.findRow(entity);
      if (row != null) return row;
    }
    return null;
  }

  public EntityCollector2 getCollector() {
    return myCollector;
  }

  public Object getValue(KeyInfo column, int row) {
    return myTable.getValue(column, row);
  }

  /**
   * Set value of a single cell. Updates resolutions if the column affects resolutions.
   * @return true iff any resolution is updated (rows were merged)
   */
  @SuppressWarnings("SimplifiableIfStatement")
  public boolean setValue(EntityPlace place, KeyInfo column, Object value, boolean override) {
    LogHelper.assertError(place.getTable() == this, "Wrong entity place", place, this);
    boolean isIdentity = myTable.isIdentity(column);
    if (override && isIdentity) {
      LogHelper.error("Can not override identity", this, column, value);
      return false;
    }
    if (!myTable.setValue(place.getIndex(), column, value, override)) return false;
    if (!isIdentity) return false;
    return mergeResolutions(place);
  }

  public void setValues(EntityPlace place, ValueRow row) {
    LogHelper.assertError(place.getTable() == this, "Wrong entity place", place, this);
    boolean changed = myTable.copyValues(place.getIndex(), row, true);
    if (!changed) return;
    mergeResolutions(place);
  }

  @Override
  public void setItem(EntityPlace place, long item) {
    EntityPlace known = myByItem.get(item);
    if (known != null) {
      int prev = known.getIndex();
      int index = place.getIndex();
      if (prev == index) return;
      for (KeyInfo info : myTable.getIdentityKeys()) {
        Object newValue = myTable.getValue(info, index);
        Object prevValue = myTable.getValue(info, prev);
        if (newValue == null || prevValue == null) continue;
        if (!info.equalValue(prevValue, newValue)) {
          LogHelper.error("Can not set item. Different identity values.", info, newValue, prevValue, place, known, item);
          return;
        }
      }
      merge(known, place);
      return;
    }
    myByItem.put(item, place);
  }

  private final ThreadLocal<IntArray> myRestoreStack = new ThreadLocal<IntArray>();
  @Override
  public Entity restoreIdentified(int placeIndex) {
    IntArray stack = myRestoreStack.get();
    boolean clearOnExit = stack == null;
    if (stack == null) {
      stack = new IntArray();
      myRestoreStack.set(stack);
    }
    int originalSize = stack.size();
    if (stack.contains(placeIndex)) {
      LogHelper.error("Cycle dependency detected");
      return null;
    }
    try {
      stack.add(placeIndex);
      Entity result = new Entity(myType);
      for (KeyInfo identityKey : myTable.getIdentityKeys()) {
        Object rawValue = getValue(identityKey, placeIndex);
        if (rawValue == null) continue;
        Object value = identityKey.restoreValue(rawValue);
        if (value == null && rawValue != ValueRow.NULL_VALUE) continue;
        //noinspection unchecked
        result.put((EntityKey<Object>) identityKey.getKey(), value);
      }
      addItemId(result, placeIndex);
      return result;
    } finally {
      if (originalSize != stack.size() - 1) LogHelper.error("Wrong stack size", originalSize, stack.size());
      else if (stack.get(originalSize) != placeIndex) LogHelper.error("Wrong top stack element", placeIndex, stack.get(originalSize));
      else stack.removeAt(originalSize);
      if (clearOnExit) myRestoreStack.set(null);
    }
  }

  private void addItemId(final Entity target, final int placeIndex) {
    myByItem.forEachEntry(new TLongObjectProcedure<EntityPlace>() {
      @Override
      public boolean execute(long item, EntityPlace place) {
        if (place.getIndex() != placeIndex) return true;
        else {
          target.put(StoreBridge.ITEM_ID, item);
          return false;
        }
      }
    });
  }

  private boolean mergeResolutions(EntityPlace place) {
    boolean updated = false;
    boolean merged;
    do {
      merged = false;
      for (ResolutionTable resolution : myResolutions) {
        EntityPlace prev = resolution.update(place);
        if (prev == null || prev.getIndex() == place.getIndex()) continue;
        if (areDistinct(prev, place)) continue;
        merge(prev, place);
        merged = true;
        updated = true;
      }
    } while (merged);
    return updated;
  }

  private boolean areDistinct(EntityPlace place1, EntityPlace place2) {
    if (place1 == place2) return false;
    if (place1 == null || place2 == null) return true;
    for (KeyInfo keyInfo : myTable.getIdentityKeys()) {
      Object value1 = place1.getValue(keyInfo);
      Object value2 = place2.getValue(keyInfo);
      if (value1 == null || value2 == null) continue;
      if (!keyInfo.equalValue(value1, value2)) return true;
    }
    return false;
  }

  void merge(EntityPlace target, EntityPlace other) {
    if (target.getIndex() == other.getIndex()) return;
    if (target.getIndex() > other.getIndex()) {
      EntityPlace tmp = target;
      target = other;
      other = tmp;
    }
    int targetIndex = target.getIndex();
    int otherIndex = other.getIndex();
    myTable.mergeRows(otherIndex, targetIndex);
    target.addSlave(other);
    myTable.removeRow(otherIndex);
  }

  public void validateIdentities() {
    for (ResolutionTable resolution : myResolutions) resolution.ensureValidHashes();
  }

  public int getPlaceCount() {
    return myTable.getRowCount();
  }

  @Override
  public Collection<EntityPlace> getAllPlaces() {
    if (getPlaceCount() == 0) return Collections.emptyList();
    EntityPlace[] array = new EntityPlace[getPlaceCount()];
    for (Object obj : myByItem.getValues()) {
      EntityPlace place = (EntityPlace) obj;
      array[place.getIndex()] = place;
    }
    for (ResolutionTable resolution : myResolutions) {
      for (EntityPlace place : resolution.getRows()) array[place.getIndex()] = place;
    }
    return Collections.unmodifiableList(Arrays.asList(array));
  }

  @NotNull
  public Pair<ItemProxy[], EntityPlace[]> getResolvedByProxy(DBNamespace namespace) {
    if (myByItem.isEmpty()) return Pair.create(ItemProxy.EMPTY_ARRAY, EntityPlace.EMPTY_ARRAY);
    ItemProxy[] proxies = new ItemProxy[myByItem.size()];
    EntityPlace[] places = new EntityPlace[myByItem.size()];
    long[] keys = myByItem.keys();
    for (int i = 0; i < keys.length; i++) {
      long item = keys[i];
      proxies[i] = new ItemProxy.Item(item);
      places[i] = myByItem.get(item);
    }
    return Pair.create(proxies, places);
  }

  public int getResolutionsCount() {
    return myResolutions.length;
  }

  public Pair<List<KeyInfo>, Collection<EntityPlace>> getResolution(int resolutionIndex) {
    List<KeyInfo> columns = getResolutionColumns(resolutionIndex);
    Collection<EntityPlace> rows = myResolutions[resolutionIndex].getRows();
    return Pair.create(columns, rows);
  }

  @Override
  public List<KeyInfo> getResolutionColumns(int resolutionIndex) {
    return myResolutions[resolutionIndex].getColumns();
  }

  public boolean isCreateResolution(int resolutionIndex) {
    return mySearchResolutionIndex > resolutionIndex;
  }

  @Override
  public boolean isMutableResolution(int resolutionIndex) {
    if (!isCreateResolution(resolutionIndex)) return true;
    Boolean known = myMutableResolution[resolutionIndex];
    if (known != null) return known;
    List<KeyInfo> columns = getResolutionColumns(resolutionIndex);
    known = false;
    for (KeyInfo column : columns) {
      if (Boolean.TRUE.equals(column.getKey().toEntity().get(EntityHolder.MUTABLE_IDENTITY))) {
        known = true;
        break;
      }
    }
    myMutableResolution[resolutionIndex] = known;
    return known;
  }

  @Override
  public boolean isCreateResolutionColumn(KeyInfo info) {
    for (int i = 0; i < mySearchResolutionIndex; i++) if (myResolutions[i].hasColumn(info)) return true;
    return false;
  }

  public Entity getItemType() {
    return myType;
  }

  public List<KeyInfo> getAllColumns() {
    return myTable.getAllColumns();
  }

  @Override
  public String toString() {
    return "EntityTable[" + getItemType() + ": " + getPlaceCount() + "]";
  }
}
