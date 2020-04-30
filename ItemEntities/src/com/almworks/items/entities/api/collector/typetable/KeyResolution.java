package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.dbwrite.impl.DBObjectsCache;
import com.almworks.items.util.DBNamespace;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

class KeyResolution extends SpecialTable {
  private final Map<String, KeyPlace> myKeys = Collections15.hashMap();
  private final Map<EntityKey<?>, KeyInfo> myHints = Collections15.hashMap();

  KeyResolution(EntityCollector2 collector) {
    super(collector);
  }

  @Override
  public Entity getItemType() {
    return EntityKey.typeKey();
  }

  @Override
  protected Collection<? extends SpecialPlace> getPlaces() {
    return myKeys.values();
  }

  @Override
  protected DBIdentifiedObject createIdentifiedObject(DBNamespace namespace, SpecialPlace keyPlace) {
    return DBObjectsCache.createAttribute(keyPlace.createEntity(), namespace);
  }

  @Nullable
  public EntityPlace getOrCreatePlace(Entity keyEntity) {
    if (keyEntity == null) return null;
    if (!Entity.isKeyType(keyEntity.getType())) return null;
    String id = EntityKey.getId(keyEntity);
    if (id == null) {
      LogHelper.error("Missing key id", keyEntity);
      return null;
    }
    KeyPlace place = myKeys.get(id);
    if (place != null) return place;
    EntityKey.Composition composition = EntityKey.getComposition(keyEntity);
    if (composition == null) {
      LogHelper.error("Missing composition", keyEntity);
      return null;
    }
    if (composition == EntityKey.Composition.HINT) {
      LogHelper.error("Hint can not be identified", keyEntity);
      return null;
    }
    return createPlace(keyEntity, id);
  }

  @NotNull
  private KeyPlace createPlace(Entity keyEntity, String id) {
    KeyPlace place = new KeyPlace(this, myKeys.size(), keyEntity);
    myKeys.put(id, place);
    return place;
  }

  public KeyInfo getKnownKeyInfo(EntityKey<?> key) {
    if (key == null) return null;
    if (key.getComposition() == EntityKey.Composition.HINT) return myHints.get(key);
    KeyPlace place = myKeys.get(key.getId());
    return place != null ? place.getInfo() : null;
  }
  
  @Nullable
  public KeyInfo getOrCreateKey(EntityKey<?> key) {
    if (key == null) return null;
    String id = key.getId();
    if (id == null) return null;
    KeyPlace place = myKeys.get(id);
    if (place != null) {
      place.ensureHasInfo(key);
      return place.getInfo();
    }
    EntityKey.Composition composition = key.getComposition();
    if (composition == null) {
      LogHelper.error("Missing composition", key);
      return null;
    }
    if (composition == EntityKey.Composition.HINT) return getOrCreateHint(key);
    place = createPlace(key.toEntity(), id);
    place.ensureHasInfo(key);
    return place.getInfo();
  }

  private KeyInfo getOrCreateHint(EntityKey<?> key) {
    KeyInfo info = myHints.get(key);
    if (info == null) {
      info = KeyInfo.create(key);
      if (info == null) {
        LogHelper.error("Failed to create hint", key);
        return null;
      }
      myHints.put(key, info);
    }
    return info;
  }

  private static class KeyPlace extends SpecialPlace {
    private KeyInfo myInfo;

    private KeyPlace(KeyResolution table, int index, Entity keyEntity) {
      super(table, index, keyEntity);
    }

    public EntityKey<?> getKey() {
      return myInfo != null ? myInfo.getKey() : null;
    }

    public KeyInfo getInfo() {
      return myInfo;
    }

    public void ensureHasInfo(EntityKey<?> key) {
      if (myInfo != null) return;
      myInfo = KeyInfo.create(key);
      LogHelper.assertError(myInfo != null, "Failed to create info", key);
    }
  }
}
