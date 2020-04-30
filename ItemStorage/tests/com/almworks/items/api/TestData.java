package com.almworks.items.api;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.impl.DBWriterImpl;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.SlaveUtils;
import org.almworks.util.Collections15;
import org.almworks.util.Const;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class TestData {
  public static final DBAttribute<String> STRING = DBAttribute.String("string", "String");
  public static final DBAttribute<Boolean> BOOL = DBAttribute.Bool("bool", "Bool");
  public static final DBAttribute<Integer> INT = DBAttribute.Int("int", "Int");
  public static final DBAttribute<Long> LINK = SlaveUtils.masterReference("link", "Link");
  public static final DBAttribute<Long> LONG = DBAttribute.Long("long", "Long");
  public static final DBAttribute<BigDecimal> DECIMAL = DBAttribute.Scalar("decimal", "Decimal", BigDecimal.class);
  public static final DBAttribute<Date> DATE = DBAttribute.Scalar("date", "Date", Date.class);
  public static final DBAttribute<byte[]> ARRAY = DBAttribute.ByteArray("array", "Bytes");
  public static final DBAttribute<Character> CHAR = DBAttribute.Scalar("char", "Character", Character.class);
  public static final DBAttribute<AttributeMap> MAP = DBAttribute.AttributeMap("attributeMap", "Attribute Map");
  public static final DBAttribute<LongList> LONG_LIST_SCALAR = DBAttribute.Scalar("longListScalar", "LongListScalar", LongList.class);

  public static final DBAttribute<List<String>> STRING_LIST = listVariant(STRING);
  public static final DBAttribute<List<Boolean>> BOOL_LIST = listVariant(BOOL);
  public static final DBAttribute<List<Integer>> INT_LIST = listVariant(INT);
  public static final DBAttribute<List<Long>> LINK_LIST = listVariant(LINK);
  public static final DBAttribute<List<Long>> LONG_LIST = listVariant(LONG);
  public static final DBAttribute<List<BigDecimal>> DECIMAL_LIST = listVariant(DECIMAL);
  public static final DBAttribute<List<Date>> DATE_LIST = listVariant(DATE);

  public static final DBAttribute<Set<String>> STRING_SET = setVariant(STRING);
  public static final DBAttribute<Set<Boolean>> BOOL_SET = setVariant(BOOL);
  public static final DBAttribute<Set<Integer>> INT_SET = setVariant(INT);
  public static final DBAttribute<Set<Long>> LINK_SET = setVariant(LINK);
  public static final DBAttribute<Set<Long>> LONG_SET = setVariant(LONG);
  public static final DBAttribute<Set<BigDecimal>> DECIMAL_SET = setVariant(DECIMAL);
  public static final DBAttribute<Set<Date>> DATE_SET = setVariant(DATE);

  public static final AttributeMap SCALAR_VALUES = new AttributeMap();
  public static final AttributeMap VALUESET1 = new AttributeMap();
  public static final AttributeMap VALUESET2 = new AttributeMap();

  public static final long ITEM1 = 1000;
  public static final long ITEM2 = 1003;
  public static final long ITEM3 = 1006;

  static {
    initValues();
  }

  private static void initValues() {
    SCALAR_VALUES.put(STRING, "foo");
    SCALAR_VALUES.put(BOOL, false);
    SCALAR_VALUES.put(BOOL, true);
    SCALAR_VALUES.put(INT, 123);
    SCALAR_VALUES.put(LINK, ITEM1);
    SCALAR_VALUES.put(LONG, 88888888833L);
    SCALAR_VALUES.put(DECIMAL, new BigDecimal("3.1415926"));
    SCALAR_VALUES.put(DATE, new Date(5 * Const.DAY * 365));
    SCALAR_VALUES.put(ARRAY, new byte[] {1, -1, 0});
    SCALAR_VALUES.put(CHAR, '+');
    SCALAR_VALUES.put(LONG_LIST_SCALAR, LongArray.create(0, 1, 2, 3, 4, 5));

    VALUESET1.putAll(SCALAR_VALUES);
    VALUESET1.put(STRING_LIST, Arrays.asList("foo", "bar", "bar"));
    VALUESET1.put(BOOL_LIST, Arrays.asList(true, false, true));
    VALUESET1.put(INT_LIST, Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));
    VALUESET1.put(LINK_LIST, Arrays.asList(ITEM1, ITEM2, ITEM2));
    VALUESET1.put(LONG_LIST, Arrays.asList(-1L, 0L, 1L));
    VALUESET1.put(DECIMAL_LIST, Arrays.asList(new BigDecimal("3.1415926")));
    VALUESET1.put(DATE_LIST, Arrays.asList(new Date(), new Date(), new Date(0)));
    VALUESET1.put(STRING_SET, Collections15.hashSet(Arrays.asList("bar", "foo")));
    VALUESET1.put(BOOL_SET, Collections15.hashSet(Arrays.asList(true)));
    VALUESET1.put(INT_SET, Collections15.hashSet(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6)));
    VALUESET1.put(LINK_SET, Collections15.hashSet(Arrays.asList(ITEM1, ITEM2, ITEM2)));
    VALUESET1.put(LONG_SET, Collections15.hashSet(Arrays.asList(-1L, 0L, 1L)));
    VALUESET1.put(DECIMAL_SET, Collections15.hashSet(Arrays.asList(new BigDecimal("3.1415926"))));
    VALUESET1.put(DATE_SET, Collections15.hashSet(Arrays.asList(new Date(), new Date(0))));

    VALUESET2.put(STRING, "bar");
    VALUESET2.put(BOOL, false);
    VALUESET2.put(INT, 1234);
    VALUESET2.put(LINK, ITEM2);
    VALUESET2.put(LONG, 888888888334L);
    VALUESET2.put(DECIMAL, new BigDecimal("3.141592647"));
    VALUESET2.put(DATE, new Date(6 * Const.DAY * 365));
    VALUESET2.put(ARRAY, new byte[] {1, -1});
    VALUESET2.put(CHAR, (char) 0);
    VALUESET2.put(LONG_LIST_SCALAR, LongList.EMPTY);
    VALUESET2.put(STRING_LIST, Arrays.asList("foo", "bar", "bar", "bar"));
    VALUESET2.put(BOOL_LIST, Arrays.asList(false, true));
    VALUESET2.put(INT_LIST, Arrays.asList(3, 1, 4, 1, 5));
    VALUESET2.put(LINK_LIST, Arrays.asList(ITEM1, ITEM2));
    VALUESET2.put(LONG_LIST, Arrays.asList(-2L, -1L, 0L, 1L));
    VALUESET2.put(DECIMAL_LIST, Arrays.asList(new BigDecimal("3.1415926"), new BigDecimal("3.1415926")));
    VALUESET2.put(DATE_LIST, Arrays.asList(new Date(0)));
    VALUESET2.put(STRING_SET, Collections15.hashSet(Arrays.asList("barz", "fooz")));
    VALUESET2.put(BOOL_SET, Collections15.hashSet(Arrays.asList(true, false)));
    VALUESET2.put(INT_SET, Collections15.hashSet(Arrays.asList(1, 4, 1, 5, 9, 2, 6)));
    VALUESET2.put(LINK_SET, Collections15.hashSet(Arrays.asList(ITEM1, ITEM2, ITEM3)));
    VALUESET2.put(LONG_SET, Collections15.hashSet(Arrays.asList(-1L, 1L)));
    VALUESET2.put(DECIMAL_SET,
      Collections15.hashSet(Arrays.asList(new BigDecimal("3.1415926"), new BigDecimal("3.14159263"))));
    VALUESET2.put(DATE_SET, Collections15.hashSet(Arrays.asList(new Date(System.currentTimeMillis() - 10000))));
  }

  public static <T> DBAttribute<List<T>> listVariant(DBAttribute<T> scalar) {
    return DBAttribute.List(scalar.getId() + ".list", "List of " + scalar.getName(), scalar.getValueClass());
  }

  public static <T> DBAttribute<Set<T>> setVariant(DBAttribute<T> scalar) {
    return DBAttribute.Set(scalar.getId() + ".set", "Set of " + scalar.getName(), scalar.getValueClass());
  }

  public static void writeMap(long item, AttributeMap map, DBWriter writer) {
    for (DBAttribute attribute : map.keySet()) {
      writer.setValue(item, attribute, map.get(attribute));
    }
  }

  public static void materializeTestItems(Database db) {
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter dbWriter) throws DBOperationCancelledException {
        DBWriterImpl writer = (DBWriterImpl)dbWriter;
        writer.itemChanged(ITEM1);
        writer.itemChanged(ITEM2);
        writer.itemChanged(ITEM3);
        return null;
      }
    }).waitForCompletion();
  }
}
