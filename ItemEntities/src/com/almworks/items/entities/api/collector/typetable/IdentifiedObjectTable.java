package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class IdentifiedObjectTable implements PriEntityTable {
  private final EntityCollector2 myCollector;
  private final Map<ItemProxy, DBObjectPlace> myResolution = Collections15.hashMap();
  private final List<DBObjectPlace> myObjects = Collections15.arrayList();
  private final Map<String, KeyInfo> myAllColumns = Collections15.hashMap();

  IdentifiedObjectTable(EntityCollector2 collector) {
    myCollector = collector;
  }

  @Override
  public Object getValue(KeyInfo column, int row) {
    return myObjects.get(row).priGetValue(column);
  }

  @Override
  public boolean setValue(EntityPlace place, KeyInfo column, Object value, boolean override) {
    DBObjectPlace objPlace = cast(place);
    return objPlace != null && objPlace.priSetValue(column, value, override);
  }

  @Override
  public void setValues(EntityPlace place, ValueRow row) {
    DBObjectPlace objPlace = cast(place);
    if (objPlace != null) objPlace.priSetValues(row);
  }

  @Override
  public void setItem(EntityPlace place, long item) {
    LogHelper.error("Not supported", place, item);
  }

  @Override
  public EntityCollector2 getCollector() {
    return myCollector;
  }

  @Override
  public Entity getItemType() {
    return StoreBridge.NULL_TYPE;
  }

  @Override
  public final int getResolutionsCount() {
    return 0;
  }

  @Override
  public final boolean isMutableResolution(int resolutionIndex) {
    LogHelper.error("Index out of bounds", resolutionIndex);
    return false;
  }

  @Override
  public Pair<List<KeyInfo>, Collection<EntityPlace>> getResolution(int resolutionIndex) {
    LogHelper.error("Not supported", resolutionIndex);
    return null;
  }

  @Override
  public List<KeyInfo> getResolutionColumns(int resolutionIndex) {
    LogHelper.error("Not supported", resolutionIndex);
    return null;
  }

  @Override
  public boolean isCreateResolution(int resolutionIndex) {
    LogHelper.error("Not supported", resolutionIndex);
    return false;
  }

  @Override
  public boolean isCreateResolutionColumn(KeyInfo info) {
    return false;
  }

  @Override
  public Pair<ItemProxy[], EntityPlace[]> getResolvedByProxy(DBNamespace namespace) {
    if (myObjects.isEmpty()) return Pair.create(ItemProxy.EMPTY_ARRAY, EntityPlace.EMPTY_ARRAY);
    ItemProxy[] objects = new ItemProxy[myObjects.size()];
    EntityPlace[] places = new EntityPlace[myObjects.size()];
    for (Map.Entry<ItemProxy, DBObjectPlace> entry : myResolution.entrySet()) {
      DBObjectPlace place = entry.getValue();
      int index = place.getIndex();
      places[index] = place;
      objects[index] = entry.getKey();
    }
    return Pair.create(objects, places);
  }

  @Override
  public Collection<KeyInfo> getAllColumns() {
    return Collections.unmodifiableCollection(myAllColumns.values());
  }

  @Override
  public int getPlaceCount() {
    return myObjects.size();
  }

  @Override
  public Collection<EntityPlace> getAllPlaces() {
    return Collections.<EntityPlace>unmodifiableList(myObjects);
  }

  @Override
  public Entity restoreIdentified(int placeIndex) {
    DBObjectPlace place = myObjects.get(placeIndex);
    ItemProxy proxy = place.myDbObject;
    return StoreBridge.buildFromProxy(proxy).fix();
  }

  private DBObjectPlace cast(EntityPlace place) {
    DBObjectPlace objPlace = Util.castNullable(DBObjectPlace.class, place);
    LogHelper.assertError(objPlace != null, "Wrong place", place);
    return objPlace;
  }

  private void ensureKnowsColumn(KeyInfo column) {
    if (column == null) return;
    String id = column.getKey().getId();
    if (myAllColumns.containsKey(id)) return;
    myAllColumns.put(id, column);
  }

  public EntityPlace identify(ItemProxy dbObject) {
    if (dbObject == null) return null;
    DBObjectPlace place = myResolution.get(dbObject);
    if (place == null) {
      place = new DBObjectPlace(this, myObjects.size(), dbObject);
      myObjects.add(place);
      myResolution.put(dbObject, place);
    }
    return place;
  }

  private static class DBObjectPlace extends EntityPlace {
    private final ValueRow myValues;
    private final ItemProxy myDbObject;

    DBObjectPlace(IdentifiedObjectTable table, int index, ItemProxy dbObject) {
      super(table, index);
      myDbObject = dbObject;
      myValues = new ValueRow(table.getCollector());
    }

    private IdentifiedObjectTable getMyTable() {
      return (IdentifiedObjectTable) getTable();
    }
    
    public Object priGetValue(KeyInfo column) {
      int columnIndex = myValues.getColumnIndex(column);
      return columnIndex >= 0 ? myValues.getValue(columnIndex) : null;
    }

    public boolean priSetValue(KeyInfo column, Object value, boolean override) {
      getMyTable().ensureKnowsColumn(column);
      int index = myValues.getColumnIndex(column);
      if (index < 0) {
        myValues.addColumn(column, value);
        return true;
      }
      Object prev = myValues.getValue(index);
      boolean equal = column.equalValue(prev, value);
      LogHelper.assertError(prev == null || equal || override, "Different values", column, prev, value);
      myValues.setValue(index, value);
      return !equal;
    }

    public void priSetValues(ValueRow row) {
      List<KeyInfo> columns = row.getColumns();
      for (int i = 0; i < columns.size(); i++) {
        KeyInfo info = columns.get(i);
        priSetValue(info, row.getValue(i), false);
      }
    }
  }
}
