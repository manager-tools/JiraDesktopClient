package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract class SpecialTable implements PriEntityTable {
  protected final EntityCollector2 myCollector;

  SpecialTable(EntityCollector2 collector) {
    myCollector = collector;
  }

  @Override
  public EntityCollector2 getCollector() {
    return myCollector;
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
    LogHelper.error("Not supported", info);
    return false;
  }

  @Override
  public Object getValue(KeyInfo column, int row) {
    LogHelper.error("Not supported", column, row);
    return null;
  }

  @Override
  public boolean setValue(EntityPlace place, KeyInfo column, Object value, boolean override) {
    LogHelper.error("Not supported", place, column, value);
    return false;
  }

  @Override
  public void setValues(EntityPlace place, ValueRow row) {
    SpecialPlace keyPlace = Util.castNullable(SpecialPlace.class, place);
    if (keyPlace == null) {
      LogHelper.error("Wrong place", place, row);
      return;
    }
    List<KeyInfo> columns = row.getColumns();
    for (int i = 0; i < columns.size(); i++) {
      KeyInfo info = row.getColumns().get(i);
      KeyInfo.HintInfo hint = Util.castNullable(KeyInfo.HintInfo.class, info);
      Object value = row.getValue(i);
      if (hint == null) {
        LogHelper.error("Not supported", info, value);
        continue;
      }
      if (value == null) continue;
      Object hintValue = keyPlace.getHint(hint);
      LogHelper.assertError(value.equals(hintValue), "Not supported", hint, value, hintValue);
    }
  }

  @Override
  public void setItem(EntityPlace place, long item) {
    LogHelper.error("Not supported", place, item);
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

  protected abstract Collection<? extends SpecialPlace> getPlaces();

  @Override
  public Pair<ItemProxy[], EntityPlace[]> getResolvedByProxy(DBNamespace namespace) {
    List<ItemProxy> proxies = Collections15.arrayList();
    List<EntityPlace> places = Collections15.arrayList();
    for (SpecialPlace keyPlace : getPlaces()) {
      DBIdentifiedObject attribute = createIdentifiedObject(namespace, keyPlace);
      if (attribute == null) continue;
      proxies.add(DBIdentity.fromDBObject(attribute));
      places.add(keyPlace);
    }
    return Pair.create(proxies.toArray(new ItemProxy[proxies.size()]), places.toArray(new EntityPlace[places.size()]));
  }

  protected abstract DBIdentifiedObject createIdentifiedObject(DBNamespace namespace, SpecialPlace keyPlace);

  @Override
  public List<KeyInfo> getAllColumns() {
    return Collections.emptyList();
  }

  @Override
  public int getPlaceCount() {
    return getPlaces().size();
  }

  @Override
  public Collection<EntityPlace> getAllPlaces() {
    return Collections.<EntityPlace>unmodifiableCollection(getPlaces());
  }

  @Override
  public Entity restoreIdentified(int placeIndex) {
    for (SpecialPlace place : getPlaces()) if (place.getIndex() == placeIndex) return place.getEntity();
    LogHelper.error("Place not found", placeIndex, this);
    return null;
  }

  static class SpecialPlace extends EntityPlace {
    private final Entity myEntity;

    SpecialPlace(SpecialTable table, int index, Entity entity) {
      super(table, index);
      myEntity = entity;
    }

    public Object getHint(KeyInfo.HintInfo hint) {
      EntityKey<?> key = hint.getKey();
      if (!myEntity.hasValue(key)) return null;
      Object value = myEntity.get(key);
      return value != null ? value : ValueRow.NULL_VALUE;
    }

    public Entity getEntity() {
      return myEntity;
    }
  }
}
