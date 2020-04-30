package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

class TypeResolution extends SpecialTable{
  private final Map<String, SpecialPlace> myTypes = Collections15.hashMap();

  TypeResolution(EntityCollector2 collector) {
    super(collector);
  }

  @Override
  protected Collection<? extends SpecialPlace> getPlaces() {
    return myTypes.values();
  }

  @Override
  protected DBIdentifiedObject createIdentifiedObject(DBNamespace namespace, SpecialPlace keyPlace) {
    return StoreBridge.toDBType(keyPlace.getEntity(), namespace);
  }

  @Override
  public Entity getItemType() {
    return Entity.metaType();
  }

  public EntityPlace getOrCreatePlace(Entity type) {
    if (type == null) return null;
    if (!Entity.isMetaType(type.getType())) return null;
    String id = type.getTypeId();
    if (id == null) {
      LogHelper.error("Missing type id", type);
      return null;
    }
    SpecialPlace place = myTypes.get(id);
    if (place != null) return place;
    return createPlace(type, id);
  }

  @NotNull
  private SpecialPlace createPlace(Entity type, String id) {
    SpecialPlace place = new SpecialPlace(this, myTypes.size(), type);
    myTypes.put(id, place);
    return place;
  }
}
