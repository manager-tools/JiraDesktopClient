package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.dbwrite.StoreBridge;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;

import java.util.*;

public class Resolver {
  private final Map<Entity, Entity> myReplacements;
  private final Map<Entity, Long> myResolutions = Collections15.hashMap();

  private Resolver(Map<Entity, Entity> replacements) {
    myReplacements = replacements;
  }

  public static Resolver create(Collection<Entity> entities) {
    Map<Entity, List<Entity>> byType = Collections15.hashMap();
    for (Entity entity : entities) {
      Entity type = entity.getType();
      if (Entity.isMetaType(type) || Entity.isKeyType(type) || StoreBridge.NULL_TYPE.equals(type)) continue;
      List<Entity> list = byType.get(type);
      if (list == null) {
        list = Collections15.arrayList();
        byType.put(type, list);
      }
      list.add(entity);
    }
    Map<Entity, Entity> replacements = collectReplacements(byType);
    return new Resolver(replacements);
  }

  private static Map<Entity, Entity> collectReplacements(Map<Entity, List<Entity>> byType) {
    HashMap<Entity, Entity> result = Collections15.hashMap();
    for (Map.Entry<Entity, List<Entity>> entry : byType.entrySet()) {
      UniteEntities replacements = new UniteEntities();
      Entity type = entry.getKey();
      EntityResolution resolution = type.get(EntityResolution.KEY);
      if (resolution == null) {
        LogHelper.error("Missing resolution", type);
        continue;
      }
      collectReplacements(entry.getValue(), resolution, replacements);
      replacements.getUnited(result);
    }
    return result.isEmpty() ? Collections15.<Entity, Entity>emptyMap() : Collections.unmodifiableMap(result);
  }

  static void collectReplacements(List<Entity> entities, EntityResolution resolution, UniteEntities union) {
    for (int i = 0; i < entities.size(); i++) {
      Entity entity = entities.get(i);
      if (entity == null) continue;
      boolean found = true;
      while (found) {
        found = false;
        int pos = 0;
        while ((pos = union.findSame(resolution, entity, pos, entities)) >= 0) {
          if (pos == i) {
            pos++;
            continue;
          }
          entities.set(i, null);
          Entity other = entities.set(pos, null);
          entity = union.unite(entity, other);
          found = true;
        }
      }
    }
  }

  public Entity getGeneric(Entity entity) {
    if (entity == null) return entity;
    Entity replacement = myReplacements.get(entity);
    return replacement != null ? replacement : entity;
  }

  public long getResolution(Entity entity) {
    entity = getGeneric(entity);
    if (entity == null) return 0;
    Long item = myResolutions.get(entity);
    return item != null && item > 0 ? item : 0;
  }

  public void setResolution(Entity entity, long item) {
    if (item <= 0 || entity == null) return;
    entity = getGeneric(entity);
    if (entity == null) return;
    Long prev = myResolutions.get(entity);
    if (prev != null) LogHelper.error("Already resolved", entity, item, prev);
    else myResolutions.put(entity, item);
  }
}
