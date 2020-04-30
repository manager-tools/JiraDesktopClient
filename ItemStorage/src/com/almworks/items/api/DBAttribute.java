package com.almworks.items.api;

import com.almworks.integers.LongList;
import com.almworks.items.util.AttributeMap;
import com.almworks.items.util.DatabaseUtil;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Collections15;

import java.math.BigDecimal;
import java.util.*;

import static com.almworks.items.api.DBAttribute.ScalarComposition.SCALAR;

/**
 * DBAttribute is a typed aspect of data, which may or may not have a value for each item.
 * <p>
 * Attribute IDs should follow convention:
 * <code>[package name]:a:[local id]</code>
 * See {@link com.almworks.items.util.DBNamespace}.
 * <p>
 * Attribute type T may be any valid scalar type, or List, or Set of scalars.
 * The validity of type is defined by {@link com.almworks.items.impl.DBConfiguration},
 * which can be pre-configured to add support for non-standard scalar types.
 * <p>
 * <strong>IMPORTANT</strong>: Scalar type name is stored in the database, so it
 * <b>must not be obfuscated</b>. Otherwise, another version of the application will
 * fail when reading the attribute.
 * <p>
 * The following scalar types are supported by default:
 * <ul>
 * <li>java.lang.Long
 * <li>java.lang.Integer
 * <li>java.lang.String
 * <li>java.lang.Boolean
 * <li>byte[]
 * <li>java.math.BigDecimal
 * <li>java.util.Date
 * <li>com.almworks.integers.LongList
 * <li>com.almworks.items.util.AttributeMap
 * </ul>
 *
 * @param <T>
 */
public final class DBAttribute<T> extends DBIdentifiedObject {
  public static final DBAttribute[] EMPTY_ARRAY = {};

  public static final DBAttribute<String> ID;
  public static final DBAttribute<String> NAME;
  public static final DBAttribute<Long> TYPE;
  public static final DBAttribute<String> SCALAR_CLASS;
  public static final DBAttribute<Integer> SCALAR_COMPOSITION;
  public static final DBAttribute<Boolean> PROPAGATING_CHANGE;

  public static final Convertor<DBAttribute, String> TO_ID = new Convertor<DBAttribute, String>() {
    @Override
    public String convert(DBAttribute value) {
      return value == null ? "" : value.getId();
    }
  };
  public static final Convertor<DBAttribute, String> TO_NAME = new Convertor<DBAttribute, String>() {
    @Override
    public String convert(DBAttribute value) {
      return value == null ? "" : value.getName();
    }
  };

  static {
    ID = new DBAttribute<String>(Database.NS.attr("id"), "ID", String.class, SCALAR, false, false);
    NAME = new DBAttribute<String>(Database.NS.attr("name"), "Name", String.class, SCALAR, false, false);
    TYPE = new DBAttribute<Long>(Database.NS.attr("type"), "Type", Long.class, SCALAR, false, false);

    SCALAR_CLASS = new DBAttribute<String>(
      DBItemType.ATTR_NS.attr("scalarClass"), "Scalar Class", String.class, SCALAR, false, false);
    SCALAR_COMPOSITION = new DBAttribute<Integer>(
      DBItemType.ATTR_NS.attr("composition"), "Composition", Integer.class, SCALAR, false, false);
    PROPAGATING_CHANGE = new DBAttribute<Boolean>(
      DBItemType.ATTR_NS.attr("propagating"), "Propagating?", Boolean.class, SCALAR, false, false);

    initializeSystemObjects();
  }

  private static void initializeSystemObjects() {
    for (DBAttribute attribute : Arrays.asList(ID, NAME, TYPE, SCALAR_CLASS, SCALAR_COMPOSITION, PROPAGATING_CHANGE)) {
      attribute.init();
    }
    for (DBItemType type : Arrays.asList(DBItemType.TYPE, DBItemType.ATTRIBUTE)) {
      type.init();
    }
  }

  private void init() {
    initialize(DBAttribute.NAME, getName());
    initialize(DBAttribute.SCALAR_CLASS, myScalarClass.getName());
    initialize(DBAttribute.SCALAR_COMPOSITION, myComposition.ordinal());
    initialize(DBAttribute.PROPAGATING_CHANGE, myPropagatingChange ? Boolean.TRUE : null);
    // more important attributes should be initialized later
    initialize(DBAttribute.ID, getId());
    initialize(DBAttribute.TYPE, DBItemType.ATTRIBUTE);
  }

  // looking for other attributes? see SyncAttributes

  private final Class<?> myScalarClass;
  private final ScalarComposition myComposition;
  private final boolean myPropagatingChange;

  private DBAttribute(String id, String name, Class<?> scalarClass, ScalarComposition composition,
    boolean propagatingChange)
  {
    this(id, name, scalarClass, composition, propagatingChange, true);
  }

  private DBAttribute(String id, String name, Class<?> scalarClass, ScalarComposition composition,
    boolean propagatingChange, boolean initialize)
  {
    super(id, name, initialize);
    assert !propagatingChange || Long.class.equals(scalarClass);
    myScalarClass = scalarClass;
    myComposition = composition;
    myPropagatingChange = propagatingChange;
    if (initialize) {
      init();
    }
  }

  public Class<?> getScalarClass() {
    return myScalarClass;
  }

  public ScalarComposition getComposition() {
    return myComposition;
  }

  public boolean isPropagatingChange() {
    return myPropagatingChange;
  }

  public Class<T> getValueClass() {
    return myComposition.valueClass(myScalarClass);
  }

  public T getValue(long item, DBReader reader) {
    return reader.getValue(item, this);
  }

  public T getValue(long item, DBAttribute<AttributeMap> shadow, DBReader reader, boolean defaultToUser) {
    return shadow == null ? getValue(item, reader) :
      DatabaseUtil.getShadowValue(item, this, shadow, defaultToUser, reader);
  }

  public void setValue(DBWriter writer, long item, T value) {
    writer.setValue(item, this, value);
  }

  public static DBAttribute<?> create(String id, String name, Class<?> scalarClass, ScalarComposition composition,
    boolean propagatingChange)
  {
    return new DBAttribute(id, name, scalarClass, composition, propagatingChange);
  }

  public static <T> DBAttribute<T> Scalar(String id, String name, Class<T> valueClass) {
    return new DBAttribute<T>(id, name, valueClass, SCALAR, false);
  }

  public static DBAttribute<AttributeMap> AttributeMap(String id, String name) {
    return new DBAttribute<AttributeMap>(id, name, AttributeMap.class, SCALAR, false);
  }

  public static DBAttribute<byte[]> ByteArray(String id, String name) {
    return new DBAttribute<byte[]>(id, name, byte[].class, SCALAR, false);
  }

  public static <T> DBAttribute<Set<T>> Set(String id, String name, Class<T> valueClass) {
    return new DBAttribute<Set<T>>(id, name, valueClass, ScalarComposition.SET, false);
  }

  public static DBAttribute<Set<Long>> LinkSet(String id, String name, boolean propagatingChange) {
    return new DBAttribute<Set<Long>>(id, name, Long.class, ScalarComposition.SET, propagatingChange);
  }

  public static <T> DBAttribute<List<T>> List(String id, String name, Class<T> valueClass) {
    return new DBAttribute<List<T>>(id, name, valueClass, ScalarComposition.LIST, false);
  }

  public static DBAttribute<List<Long>> LinkList(String id, String name, boolean propagatingChange) {
    return new DBAttribute<List<Long>>(id, name, Long.class, ScalarComposition.LIST, propagatingChange);
  }

  public static DBAttribute<String> String(String id, String name) {
    return Scalar(id, name, String.class);
  }

  public static DBAttribute<List<String>> StringList(String id, String name) {
    return List(id, name, String.class);
  }

  public static DBAttribute<Long> Link(String id, String name, boolean propagatingChange) {
    return new DBAttribute<Long>(id, name, Long.class, SCALAR, propagatingChange);
  }

  public static DBAttribute<Long> Long(String id, String name) {
    return Scalar(id, name, Long.class);
  }

  public static DBAttribute<Integer> Int(String id, String name) {
    return Scalar(id, name, Integer.class);
  }

  public static DBAttribute<List<Integer>> IntList(String id, String name) {
    return List(id, name, Integer.class);
  }

  public static DBAttribute<Boolean> Bool(String id, String name) {
    return Scalar(id, name, Boolean.class);
  }

  public static DBAttribute<Date> Date(String id, String name) {
    return Scalar(id, name, Date.class);
  }

  public static DBAttribute<BigDecimal> Decimal(String id, String name) {
    return Scalar(id, name, BigDecimal.class);
  }

  /**
   * @param items items to read
   * @return modifiable list with values, including nulls (for illegal items, and null values)
   */
  public List<T> collectValues(Collection<? extends Long> items, DBReader reader) {
    List<T> result = Collections15.arrayList();
    for (Long item : items) result.add(item != null ? getValue(item, reader) : null);
    return result;
  }

  /**
   * @see #collectValues(java.util.Collection, DBReader)
   */
  public List<T> collectValues(LongList items, DBReader reader) {
    List<T> result = Collections15.arrayList();
    for (int i = 0; i < items.size(); i++) {
      long item = items.get(i);
      result.add(item > 0 ? getValue(item, reader) : null);
    }
    return result;
  }

  public static enum ScalarComposition {
    SCALAR,
    SET,
    LIST;

    public <T> Class<T> valueClass(Class<?> scalarClass) {
      if (this == SCALAR)
        return (Class<T>) scalarClass;
      else if (this == SET)
        return (Class<T>) Set.class;
      else if (this == LIST)
        return (Class<T>) List.class;
      throw new Error("" + this);
    }

    public static ScalarComposition byOrdinal(long composition) {
      for (ScalarComposition c : values()) {
        if (c.ordinal() == composition)
          return c;
      }
      return null;
    }
  }
}
