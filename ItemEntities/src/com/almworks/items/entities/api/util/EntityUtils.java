package com.almworks.items.entities.api.util;

import com.almworks.integers.IntArray;
import com.almworks.integers.IntIterator;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class EntityUtils {
  public static final EntityKey<Boolean> EMPTY_LIST_HINT = EntityKey.hint("sys.api.hint.emptyList", Boolean.class);
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss.S z", Locale.US);
  static {
    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GTM"));
  }

  public static String printValue(Object value) {
    StringBuilder builder = new StringBuilder();
    printValue(builder, value);
    return builder.toString();
  }

  public static void printValue(StringBuilder builder, Object value) {
    if (value == null) builder.append("<null>");
    else if (value instanceof EntityKey) builder.append(value);
    else if (value instanceof Entity) {
      builder.append(value);
      EntityResolution.printResolutionValues(builder, (Entity) value);
    } else if (value instanceof Collection) {
      builder.append("{");
      for (Object v :((Collection) value)) printValue(builder, v);
      builder.append("}");
    } else if (value instanceof Date) builder.append(DATE_FORMAT.format(value));
    else builder.append(value);
  }

  /**
   * @return entity list key with hint that empty value should be written as {0} (list with element zero), but not null.
   */
  public static EntityKey<List<Entity>> emptyList(String id) {
    Entity key = EntityKey.buildKey();
    key.put(EMPTY_LIST_HINT, true);
    return EntityKey.entityList(id, key);
  }

  @NotNull
  public static ArrayList<EntityHolder> collectHolders(EntityTransaction transaction, IntArray ids, Entity type, EntityKey<Integer> idKey) {
    ArrayList<EntityHolder> result = Collections15.arrayList();
    for (IntIterator cursor : ids) {
      EntityHolder holder = transaction.addEntity(type, idKey, cursor.value());
      if (holder != null) result.add(holder);
    }
    return result;
  }

  public static void setRefCollectionByIds(EntityHolder target, EntityKey<? extends Collection<Entity>> collectionKey, IntArray ids, Entity type, EntityKey<Integer> idKey) {
    if (target == null) return;
    target.setReferenceCollection(collectionKey, collectHolders(target.getTransaction(), ids, type, idKey));
  }

  @Nullable
  public static <M, S> EntityHolder findSlave(EntityTransaction transaction, EntityKey<Entity> masterRef, EntityKey<M> masterIdKey, M masterId,
    Entity slaveType, EntityKey<S> slaveIdKey, S slaveId) {
    if (transaction == null || masterRef == null || masterIdKey == null || slaveIdKey == null || slaveType == null) {
      LogHelper.error("Illegal arguments", transaction, masterRef, masterIdKey, slaveIdKey, slaveType);
      return null;
    }
    if (masterId == null || slaveId == null) return null;
    for (EntityHolder slave : transaction.getAllEntities(slaveType)) {
      S thisId = slave.getScalarValue(slaveIdKey);
      if (!Util.equals(thisId, slaveId)) continue;
      EntityHolder master = slave.getReference(masterRef);
      if (master == null) continue;
      M thisMasterId = master.getScalarValue(masterIdKey);
      if (Util.equals(thisMasterId, masterId)) return slave;
    }
    return null;
  }

  @NotNull
  public static <T> Map<T, EntityHolder> collectById(EntityTransaction transaction, Entity type, EntityKey<T> idKey) {
    List<EntityHolder> entities = transaction.getAllEntities(type);
    HashMap<T, EntityHolder> result = Collections15.hashMap();
    for (EntityHolder entity : entities) {
      T id = entity.getScalarValue(idKey);
      result.put(id, entity);
    }
    return result;
  }
}
