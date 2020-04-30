package com.almworks.items.entities.dbwrite.impl;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.EntityValueMerge;
import com.almworks.items.entities.api.util.EntityResolution;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.items.entities.dbwrite.SameChecker;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

class UniteEntities {
  private final EqualityClasses myEqualityClasses = new EqualityClasses();

  public Entity unite(Entity e1, Entity e2) {
    e1.fix();
    e2.fix();
    Entity key1 = myEqualityClasses.getClassKey(e1);
    Entity key2 = myEqualityClasses.getClassKey(e2);
    Entity united = doUnite(key1 != null ? key1 : e1, key2 != null ? key2 : e2);
    myEqualityClasses.unitClasses(united, key1, e1, key2, e2);
    return united;
  }

  public int findSame(EntityResolution resolution, Entity sample, int pos, List<Entity> entityList) {
    for (int i = pos; i < entityList.size(); i++) {
      Entity candidate = entityList.get(i);
      if (candidate == null) continue;
      if (candidate == sample) return i;
      if (SameChecker.isSameResolution(resolution, sample, candidate)) return i;
    }
    return -1;
  }

  private Entity doUnite(Entity e1, Entity e2) {
    if (e1 == null || e2 == null) return e1 != null ? e1 : e2;
    Entity source;
    Entity other;
    if (e1.getValueKeys().size() > e2.getValueKeys().size()) {
      source = e1;
      other = e2;
    } else {
      source = e2;
      other = e1;
    }
    Entity united = Entity.copy(source);
    boolean updated = false;
    for (EntityKey<?> key : other.getValueKeys()) if (copyValue(other, key, united)) updated = true;
    return updated ? united.fix() : source;
  }

  private <T> boolean copyValue(Entity source, EntityKey<T> key, Entity target) {
    if (!target.hasValue(key)) {
      target.copyFrom(key, source);
      return true;
    }
    T srcValue = source.get(key);
    T trgValue = target.get(key);
    if (Util.equals(trgValue, srcValue)) return false;
    EntityValueMerge merger = key.toEntity().get(EntityValueMerge.KEY);
    if (merger != null) {
      T merged = merger.mergeValues(trgValue, srcValue);
      boolean update = !Util.equals(trgValue, merged);
      if (update) target.put(key, merged);
      return update;
    }
    Entity eValue = Util.castNullable(Entity.class, trgValue);
    Entity otherEntity = Util.castNullable(Entity.class, srcValue);
    if (eValue == null || srcValue == null || !SameChecker.isSameResolution(eValue, otherEntity))
      LogHelper.error("Different values", source, EntityResolution.printResolutionValues(source), key, EntityUtils.printValue(trgValue), EntityUtils.printValue(srcValue));
    return false;
  }

  public boolean isEmpty() {
    return myEqualityClasses.isEmpty();
  }

  public Map<Entity, Entity> getUnited() {
    return myEqualityClasses.toMap(Collections15.<Entity, Entity>hashMap());
  }

  public void getUnited(Map<Entity, Entity> target) {
    myEqualityClasses.toMap(target);
  }

  private static class EqualityClasses {
    private final Map<Entity, Set<Entity>> myClasses = Collections15.hashMap();

    public Map<Entity, Entity> toMap(Map<Entity, Entity> target) {
      for (Map.Entry<Entity, Set<Entity>> entry : myClasses.entrySet()) {
        Entity classKey = entry.getKey();
        for (Entity instance : entry.getValue()) {
          if (instance == classKey) continue;
          Entity prev = target.put(instance, classKey);
          LogHelper.assertError(prev == null, "Remap equality class", classKey, instance, prev, myClasses.get(prev));
        }
      }
      return target;
    }

    public boolean isEmpty() {
      return myClasses.isEmpty();
    }

    /**
     * @return Equality class key for the given instance (may be the instance if it equals to class key).
     * Returns null if the instance does not belong to any equality class
     */
    public Entity getClassKey(Entity instance) {
      Set<Entity> aClass = myClasses.get(instance);
      if (aClass != null) return instance;
      for (Map.Entry<Entity, Set<Entity>> entry : myClasses.entrySet()) {
        if (entry.getValue().contains(instance)) return entry.getKey();
      }
      return null;
    }

    /**
     * Replaces two equality classes with single one.
     * @param unitedClassKey class key for united class
     * @param key1 key of first equality class. Null means the instance1 does not belong to any equality class
     * @param instance1 the instance of first equality class
     * @param key2 the same as key1
     * @param instance2 the same as instance1
     * @see #getClassKey(com.almworks.items.entities.api.Entity)
     */
    public void unitClasses(Entity unitedClassKey, @Nullable Entity key1, Entity instance1, @Nullable Entity key2, Entity instance2) {
      assert checkKeyAndInstance(key1, instance1);
      assert checkKeyAndInstance(key2, instance2);
      Collection<Entity> class1 = key1 != null ? myClasses.get(key1) : null;
      Collection<Entity> class2 = key2 != null ? myClasses.get(key2) : null;
      Set<Entity> uClass = myClasses.get(unitedClassKey);
      if (uClass == null) {
        uClass = Collections15.hashSet();
        myClasses.put(unitedClassKey, uClass);
        myClasses.remove(key1);
        myClasses.remove(key2);
        if (class1 != null) uClass.addAll(class1);
        if (class2 != null) uClass.addAll(class2);
        uClass.add(unitedClassKey);
      } else {
        if (class1 != null && !key1.equals(unitedClassKey)) {
          myClasses.remove(key1);
          uClass.addAll(class1);
        }
        if (class2 != null && !key2.equals(unitedClassKey)) {
          myClasses.remove(key2);
          uClass.addAll(class2);
        }
      }
      uClass.add(instance1);
      uClass.add(instance2);
    }

    private boolean checkKeyAndInstance(Entity classKey, Entity instance) {
      if (classKey == null) {
        Entity actualClassKey = getClassKey(instance);
        LogHelper.assertError(actualClassKey == null, "Class key exists", actualClassKey, instance);
      } else {
        Set<Entity> aClass = myClasses.get(classKey);
        if (aClass == null) LogHelper.error("Not a class key", classKey, instance);
        else LogHelper.assertError(aClass.contains(instance), "Wrong class key", classKey, instance, getClassKey(instance));
      }
      return true;
    }
  }
}
