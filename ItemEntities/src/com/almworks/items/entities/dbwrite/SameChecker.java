package com.almworks.items.entities.dbwrite;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.Collection;
import java.util.List;

public class SameChecker {
  private final List<Entity> myCheckSameStack = Collections15.arrayList();

  public static boolean isSameResolution(Entity e1, Entity e2) {
    return new SameChecker().isSame(e1, e2);
  }

  public static boolean isSameResolution(EntityResolution resolution, Entity e1, Entity e2) {
    return new SameChecker().isSame(resolution, e1, e2);
  }

  private boolean isSame(Entity e1, Entity e2) {
    if (e1 == e2) return true;
    if (e1 == null || e2 == null) return false;
    if (e1.equals(e2)) return true;
    Entity t1 = e1.getType();
    Entity t2 = e2.getType();
    if (t1 == null || t2 == null) return false;
    if (!t1.equals(t2)) return false;
    if (Entity.isKeyType(t1)) return false; // already checked not equal
    Long item1 = e1.get(StoreBridge.ITEM_ID);
    Long item2 = e2.get(StoreBridge.ITEM_ID);
    if (item1 != null && Util.equals(item1, item2)) return true;
    if (item1 != null && item2 != null) return false;
    String id1 = e1.get(StoreBridge.STORE_ID);
    String id2 = e2.get(StoreBridge.STORE_ID);
    if (id1 != null && Util.equals(id1, id2)) return true;
    if (id2 != null) return false;
    EntityResolution resolution = t1.get(EntityResolution.KEY);
    if (resolution == null) {
      LogHelper.error("Missing resolution", e1, e2, t1);
      return false;
    }
    return isSame(resolution, e1, e2);
  }

  private boolean isSame(EntityResolution resolution, Entity e1, Entity e2) {
    if (myCheckSameStack.contains(e1) || myCheckSameStack.contains(e2)) return false;
    myCheckSameStack.add(e1);
    myCheckSameStack.add(e2);
    try {
      if (isSameResolution(resolution.getIdentities(), e1, e2)) return true;
      if (isSameResolution(resolution.getSearchBy(), e1, e2)) return true;
    } finally {
      myCheckSameStack.remove(myCheckSameStack.size() - 1);
      myCheckSameStack.remove(myCheckSameStack.size() - 1);
    }
    return false;
  }

  private boolean isSameResolution(Collection<Collection<EntityKey<?>>> ids, Entity e1, Entity e2) {
    for (Collection<EntityKey<?>> id : ids) if (matches(id, e1, e2)) return true;
    return false;
  }

  private boolean matches(Collection<EntityKey<?>> id, Entity e1, Entity e2) {
    for (EntityKey<?> key : id) {
      Object v1 = e1.get(key);
      Object v2 = e2.get(key);
      if (v1 == null || v2 == null) return false;
      if (Util.equals(v1, v2)) continue;
      Entity ev1 = Util.castNullable(Entity.class, v1);
      Entity ev2 = Util.castNullable(Entity.class, v2);
      if (ev1 == null || ev2 == null) return false;
      if (!SameChecker.isSameResolution(ev1, ev2)) return false;
    }
    return true;
  }
}
