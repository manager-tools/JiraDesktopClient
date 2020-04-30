package com.almworks.items.entities.api.collector.typetable;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.KeyInfo;
import com.almworks.items.entities.api.collector.ValueRow;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.util.EntityUtils;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import junit.framework.Assert;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.lang.reflect.Array;
import java.util.*;

public class TransactionTestUtil {

  public static final String SEPARATOR = "------------------------------\n";

  public static String printTransaction(EntityTransaction transaction, Entity... ts) {
    ArrayList<Entity> types = Collections15.arrayList(ts);
    Collections.sort(types, Entity.TYPE_BY_ID);
    StringBuilder builder = new StringBuilder();
    for (Entity type : types) {
      builder.append("  TYPE: ").append(type).append("\n");
      boolean first = true;
      for (EntityHolder holder : transaction.getAllEntities(type)) {
        if (!first) builder.append(SEPARATOR);
        first = false;
        printPlace(builder, holder.getPlace());
      }
    }
    builder.append("  ***  BAGS ***\n");
    String bagSep = "";
    List<EntityBag2> bags = Collections15.arrayList(transaction.getBags());
    bags.sort(new Comparator<EntityBag2>() {
      @Override
      public int compare(EntityBag2 o1, EntityBag2 o2) {
        return Entity.TYPE_BY_ID.compare(o1.getQueryType(), o2.getQueryType());
      }
    });
    for (EntityBag2 bag : bags) {
      builder.append(bagSep);
      printBag(builder, bag);
      bagSep = SEPARATOR;
    }
    return builder.toString();
  }

  private static void printBag(StringBuilder builder, EntityBag2 bag) {
    EntityUtils.printValue(builder, bag.getTargetType());
    builder.append("\n");
    printQuery(builder, bag.getQuery());
    builder.append("\n");
    KeySet keySet = KeySet.create(bag.getChangeColumns());
    for (EntityKey<?> key : keySet.getKeys()) {
      int index = keySet.getIndex(key);
      Object value = bag.getChangeValue(index);
      if (value == null) {
        LogHelper.error("No change", key);
        continue;
      }
      value = convertValue(value);
      builder.append("CHANGE: ").append(key).append("=");
      EntityUtils.printValue(builder, value);
      builder.append("\n");
    }
    if (bag.isDelete()) builder.append("DELETE\n");
    for (EntityHolder holder : bag.getExclusions()) {
      EntityUtils.printValue(builder, holder.getPlace().restoreEntity());
      builder.append("\n");
    }
  }

  private static void printQuery(StringBuilder builder, ValueRow query) {
    KeySet keySet = KeySet.create(query.getColumns());
    String sep = "";
    for (EntityKey<?> key  : keySet.getKeys()) {
      KeyInfo info = keySet.getInfo(key);
      int index = query.getColumnIndex(info);
      Object value = query.getValue(index);
      if (value == null) {
        Assert.fail("Missing query value: " + key);
        continue;
      }
      value = convertValue(value);
      builder.append(sep).append(key).append("=");
      EntityUtils.printValue(builder, value);
      sep = " & ";
    }
  }

  private static void printPlace(StringBuilder builder, EntityPlace place) {
    GenericTable table = Util.castNullable(GenericTable.class, place.getTable());
    if (table == null) {
      LogHelper.error("Unsupported table", table);
      return;
    }
    KeySet keysSet = KeySet.create(table.getAllColumns());
    for (EntityKey<?> key : keysSet.getKeys()) {
      KeyInfo info = keysSet.getInfo(key);
      Object value = place.getValue(info);
      if (value == null) continue;
      value = convertValue(value);
      builder.append(key).append("=");
      EntityUtils.printValue(builder, value);
      builder.append("\n");
    }
  }

  private static Object convertValue(Object value) {
    if (value == null) {
      LogHelper.error("No value");
      return "<noValue>";
    }
    if (value == ValueRow.NULL_VALUE) return null;
    if (value instanceof EntityPlace) return  ((EntityPlace) value).restoreEntity();
    if (value instanceof Collection<?>) {
      ArrayList<Object> objects = Collections15.arrayList();
      for (Object e : (Collection<?>) value) objects.add(convertValue(e));
      return objects;
    }
    if (value.getClass().isArray()) {
      ArrayList<Object> list = Collections15.arrayList();
      for (int i = 0; i < Array.getLength(value); i++) {
        Object obj = Array.get(value, i);
        if (obj == null) LogHelper.error("Null in array");
        else obj = convertValue(obj);
        list.add(obj);
      }
      return list;
    }
    if (value instanceof EntityPlace[]) return Arrays.asList((EntityPlace[])value);
    return value;
  }

  private static class KeySet {
    private final ArrayList<EntityKey<?>> myKeys = Collections15.arrayList();
    private final HashMap<EntityKey<?>, Pair<Integer,KeyInfo>> myInfos = Collections15.hashMap();

    public static KeySet create(List<KeyInfo> infos) {
      KeySet keySet = new KeySet();
      for (int i = 0; i < infos.size(); i++) {
        KeyInfo info = infos.get(i);
        EntityKey<?> key = info.getKey();
        keySet.myKeys.add(key);
        keySet.myInfos.put(key, Pair.create(i, info));
      }
      Collections.sort(keySet.myKeys, EntityKey.ID_ORDER);
      return keySet;
    }

    public List<EntityKey<?>> getKeys() {
      return Collections.unmodifiableList(myKeys);
    }

    public KeyInfo getInfo(EntityKey<?> key) {
      return myInfos.get(key).getSecond();
    }

    public int getIndex(EntityKey<?> key) {
      return myInfos.get(key).getFirst();
    }
  }
}
