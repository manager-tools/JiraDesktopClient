package com.almworks.items.util;

import com.almworks.integers.IntArray;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Equality;
import com.almworks.util.commons.Function2;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.almworks.util.Collections15.arrayList;

public class DatabaseUtil {
  public static final Class<List<Long>> LONG_LIST = (Class) List.class;
  public static final Class<List<String>> STRING_LIST = (Class) List.class;

  public static AttributeMap diff(long artifact, @Nullable DBAttribute<AttributeMap> fromSnapshot,
    @Nullable DBAttribute<AttributeMap> toSnapshot, DBReader reader)
  {
    Set<DBAttribute> attrs = Collections15.hashSet();
    attrs.addAll(getAttributes(artifact, fromSnapshot, reader));
    attrs.addAll(getAttributes(artifact, toSnapshot, reader));
    AttributeMap r = new AttributeMap();
    for (DBAttribute attr : attrs) {
      Object newValue = getValue(artifact, toSnapshot, reader, attr);
      if (!valueEquals(newValue, getValue(artifact, fromSnapshot, reader, attr))) {
        r.put(attr, newValue);
      }
    }
    return r;
  }

  public static <T> T getShadowValue(long artifact, DBAttribute<T> attribute, DBAttribute<AttributeMap> shadow,
    boolean defaultToMainValue, DBReader reader)
  {
    T result = null;
    AttributeMap map = shadow == null ? null : reader.getValue(artifact, shadow);
    if (map != null)
      result = map.get(attribute);
    if (result == null && defaultToMainValue)
      result = reader.getValue(artifact, attribute);
    return result;
  }


  private static Object getValue(long artifact, DBAttribute<AttributeMap> shadow, DBReader reader, DBAttribute attr) {
    return shadow == null ? reader.getValue(artifact, attr) : getShadowValue(artifact, attr, shadow, false, reader);
  }

  private static Set<DBAttribute<?>> getAttributes(long artifact, DBAttribute<AttributeMap> shadow, DBReader reader) {
    if (shadow == null)
      return reader.getAttributeMap(artifact).keySet();
    AttributeMap map = shadow.getValue(artifact, reader);
    return map == null ? Collections.<DBAttribute<?>>emptySet() : map.keySet();
  }

  public static void copyAll(long fromArtifact, long toArtifact, DBWriter writer) {
    Set<DBAttribute<?>> attributes = writer.getAttributeMap(fromArtifact).keySet();
    for (DBAttribute attribute : attributes) {
      writer.setValue(toArtifact, attribute, writer.getValue(fromArtifact, attribute));
    }
  }

  @Nullable
  public static DBAttribute attributeFromArtifact(long artifact, DBReader reader) {
    String id = DBAttribute.ID.getValue(artifact, reader);
    String name = DBAttribute.NAME.getValue(artifact, reader);
    String className = DBAttribute.SCALAR_CLASS.getValue(artifact, reader);
    Integer composition = DBAttribute.SCALAR_COMPOSITION.getValue(artifact, reader);
    if (id == null) {
      Log.warn("attribute " + artifact + " does not have id");
      return null;
    } else if (className == null) {
      Log.warn("attribute " + artifact + " does not have className");
      return null;
    } else if (composition == null) {
      Log.warn("attribute " + artifact + " does not have composition");
      return null;
    }
    Class<?> valueClass = null;
    try {
      valueClass = Class.forName(className);
    } catch (ClassNotFoundException e) {
      Log.warn("attribute " + artifact + " uses unknown class " + className, e);
      return null;
    }
    DBAttribute.ScalarComposition c = DBAttribute.ScalarComposition.byOrdinal(composition);
    if (c == null) {
      Log.warn("attribute " + artifact + " uses unknown composition " + composition);
      return null;
    }
    boolean propagatingChange = false;
    if (Long.class.equals(valueClass)) {
      Boolean b = DBAttribute.PROPAGATING_CHANGE.getValue(artifact, reader);
      propagatingChange = b != null && b;
    }
    return DBAttribute.create(id, name, valueClass, c, propagatingChange);
  }

  public static void query(final DBFilter view, ThreadGate gate, Procedure<LongList> proc) {
    view.getDatabase().read(DBPriority.BACKGROUND, new ReadTransaction<LongList>() {
      public LongList transaction(DBReader reader) {
        return view.query(reader).copyItemsSorted();
      }
    }).finallyDo(gate, proc);
  }

  public static List<AttributeMap> queryMap(final BoolExpr<DP> query) {
    return dbRequire().readForeground(new ReadTransaction<List<AttributeMap>>() {
      @Override
      public List<AttributeMap> transaction(DBReader reader) throws DBOperationCancelledException {
        return queryMap(reader, query);
      }
    }).waitForCompletion();
  }

  public static List<AttributeMap> queryMap(DBReader reader, BoolExpr<DP> query) {
    LongArray items = reader.query(query).copyItemsSorted();
    return map(reader, items.toNativeArray());
  }

  private static Database dbRequire() {
    return Context.require(Database.ROLE);
  }

  public static boolean isIdentified(Long artifact, DBIdentifiedObject object, DBReader reader) {
    if (artifact == null || object == null || artifact == 0)
      return false;
    return artifact == reader.findMaterialized(object);
  }

  // call it under debug

  private static List<AttributeMap> map(final long... artifacts) {
    return dbRequire().read(DBPriority.BACKGROUND, new ReadTransaction<List<AttributeMap>>() {
      @Override
      public List<AttributeMap> transaction(DBReader reader) {
        return map(reader, artifacts);
      }
    }).waitForCompletion();
  }

  private static List<AttributeMap> map(DBReader reader, long[] artifacts) {
    List<AttributeMap> maps = arrayList();
    for (long a : artifacts) {
      maps.add(reader.getAttributeMap(a));
    }
    return maps;
  }

  public static AttributeMap loadMap(final long artifact) {
    return dbRequire().read(DBPriority.BACKGROUND, new ReadTransaction<AttributeMap>() {
      public AttributeMap transaction(DBReader reader) {
        return reader.getAttributeMap(artifact);
      }
    }).waitForCompletion();
  }

  public static <T> T flushDatabase(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      Log.warn(cause);
      return null;
    }
  }

  public static IntArray toIntArray(LongList artifacts) {
    IntArray r = new IntArray(artifacts.size());
    for (int i = 0; i < artifacts.size(); i++)
      r.add((int) artifacts.get(i));
    return r;
  }

  public static boolean checkKind(DBReader reader, long artifact, DBIdentifiedObject expectedKind) {
    return reader.getValue(artifact, DBAttribute.TYPE) == reader.findMaterialized(expectedKind);
  }

/*
  public static <T> TLongObjectHashMap<T> getOrCreateLongMap(DBReader reader,
    TypedKey<TLongObjectHashMap<T>> key) {
    TLongObjectHashMap<T> map = key.getFrom(reader.getTransactionContext());
    if (map == null) {
      map = new TLongObjectHashMap<T>();
      key.putTo(reader.getTransactionContext(), map);
    }
    return map;
  }
*/

  public static <T> Set<T> getOrCreateSet(DBReader reader, TypedKey<Set<T>> key) {
    Map context = reader.getTransactionCache();
    Set<T> set = key.getFrom(context);
    if (set == null) {
      set = Collections15.hashSet();
      key.putTo(context, set);
    }
    return set;
  }

  public static boolean valueEquals(Object v1, Object v2) {
    if (v1 == v2)
      return true;
    if (v1 == null || v2 == null)
      return false;
    if (v1 instanceof byte[] && v2 instanceof byte[])
      return Arrays.equals((byte[]) v1, (byte[]) v2);
    if (v1 instanceof BigDecimal && v2 instanceof BigDecimal)
      return ((BigDecimal)v1).compareTo((BigDecimal)v2) == 0;
    return v1.equals(v2);
  }

  public static int valueHash(Object value) {
    if (value == null)
      return 0;
    if (value instanceof byte[])
      return Arrays.hashCode((byte[]) value);
    return value.hashCode();
  }

  public static StringBuilder valueString(StringBuilder builder, Object value) {
    if (value instanceof byte[]) {
      builder.append('[');
      byte[] ba = (byte[]) value;
      int MAX = 10;
      int len = Math.min(MAX, ba.length);
      for (int i = 0; i < len; i++) {
        if (i > 0)
          builder.append(',');
        builder.append(ba[i]);
      }
      if (ba.length > MAX)
        builder.append(",...");
      builder.append(']');
    } else {
      builder.append(value);
    }
    return builder;
  }

  public static String valueString(Object value) {
    return valueString(new StringBuilder(), value).toString();
  }

  @Nullable
  public static Set<DBAttribute> collectAffectingAttributes(BoolExpr<DP> expr) {
    if (expr == null)
      return null;
    return expr.foldTerms(Collections15.<DBAttribute>hashSet(),
      new Function2<DP, Set<DBAttribute>, Set<DBAttribute>>() {
        @Override
        public Set<DBAttribute> invoke(DP dp, Set<DBAttribute> set) {
          if (set == null)
            return null;
          boolean available = dp.addAffectingAttributes(set);
          return !available ? null : set;
        }
      });
  }

  public static <K, V> Map<K, V> getMapCache(DBReader reader, TypedKey<Map<K, V>> key) {
    Map cache = reader.getTransactionCache();
    Map<K, V> map = key.getFrom(cache);
    if (map == null) {
      map = Collections15.hashMap();
      key.putTo(cache, map);
    }
    return map;
  }

  public static boolean itemsEqual(Long a, Long b) {
    final long a1 = a == null ? 0L : Math.max(a.longValue(), 0L);
    final long b1 = b == null ? 0L : Math.max(b.longValue(), 0L);
    return a1 == b1;
  }

  public static <T> boolean isEqualValue(DBAttribute<T> attribute, T value1, T value2) {
    if (valueEquals(value1, value2)) return true;
    DBAttribute.ScalarComposition composition = attribute.getComposition();
    if (composition == DBAttribute.ScalarComposition.SCALAR) return false;
    if (composition == DBAttribute.ScalarComposition.LIST) {
      List list1 = Util.castNullable(List.class, value1);
      List list2 = Util.castNullable(List.class, value2);
      if (list1 != value1 || list2 != value2) {
        Log.error("Wrong value class (expected list) " + value1 + " " + value2 + " " + attribute);
        return false;
      }
      return isListsEqual(list1, list2);
    }
    if (composition == DBAttribute.ScalarComposition.SET) {
      Set set1 = Util.castNullable(Set.class, value1);
      Set set2 = Util.castNullable(Set.class, value2);
      if (set1 != value1 || set2 != value2) {
        Log.error("Wrong value class (expected set) " + value1 + " " + value2 + " " + attribute);
        return false;
      }
      return isSetsEqual(set1, set2);
    }
    Log.error("Unknown composition " + composition + " " + attribute + " " + value1 + " " + value2);
    return false;
  }

  public static boolean isSetsEqual(Collection set1, Collection set2) {
    if (set1 == null) set1 = Collections.emptySet();
    if (set2 == null) set2 = Collections.emptySet();
    if (set1.size() != set2.size()) return false;
    if (isListsEqual(set1, set2)) return true;
    for (Object o : set1) if (!contains(o, set2)) return false;
    for (Object o : set2) if (!contains(o, set1)) return false;
    return true;
  }

  private static boolean contains(Object value, Collection c) {
    for(final Object o : c) {
      if(valueEquals(value, o)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isListsEqual(Collection list1, Collection list2) {
    if (list1 == null) list1 = Collections.emptyList();
    if (list2 == null) list2 = Collections.emptyList();
    if (list1.size() != list2.size()) return false;
    if (list1.isEmpty()) return true;
    Iterator it1 = list1.iterator();
    Iterator it2 = list2.iterator();
    while (it1.hasNext() && it2.hasNext()) if (!valueEquals(it1.next(), it2.next())) return false;
    return !it1.hasNext() && !it2.hasNext();
  }

  public static <T> boolean isEqualValueInMap(DBAttribute<T> attribute, AttributeMap map1, AttributeMap map2) {
    T value1 = map1 != null ? map1.get(attribute) : null;
    T value2 = map2 != null ? map2.get(attribute) : null;
    return isEqualValue(attribute, value1, value2);
  }

  public static String decimalToString(BigDecimal value) {
    if(value == null) {
      return null;
    }
    
    final String str = value.toPlainString();
    if(value.scale() <= 0) {
      return str;
    }

    int last = str.length() - 1;
    char c;
    while((c = str.charAt(last)) == '0') {
      last--;
    }
    if(c == '.') {
      last--;
    }
    
    return str.substring(0, last + 1);
  }

  public static byte[] serializeAttributeMap(DBReader reader, AttributeMap map) {
    ByteArrayOutputStream mapArray = new ByteArrayOutputStream();
    DataOutputStream out = new DataOutputStream(mapArray);
    reader.serializeMap(map, out);
    try {
      out.close();
    } catch (IOException e) {
      // ignore
    }
    return mapArray.toByteArray();
  }

  public static class DBAttributeEquality<T> implements Equality<T> {
    private final DBAttribute<T> myAttribute;

    public DBAttributeEquality(DBAttribute<T> attribute) {
      myAttribute = attribute;
    }

    @Override
    public boolean areEqual(T o1, T o2) {
      return DatabaseUtil.isEqualValue(myAttribute, o1, o2);
    }
  }
}
