package com.almworks.items.api;

import com.almworks.items.util.InitConst;
import com.almworks.items.util.InitIdentified;
import com.almworks.util.commons.Function;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;

public class DBIdentifiedObject implements ItemReference {
  public static final Comparator<DBIdentifiedObject> BY_ID_ORDER = new Comparator<DBIdentifiedObject>() {
    @Override
    public int compare(DBIdentifiedObject o1, DBIdentifiedObject o2) {
      String id1 = getId(o1);
      String id2 = getId(o2);
      if (id1 == id2) return 0;
      if (id1 == null || id2 == null) return id1 == null ? -1 : 1;
      return id1.compareTo(id2);
    }

    private String getId(@Nullable DBIdentifiedObject object) {
      return object != null ? object.getId() : null;
    }
  };

  private final String myId;
  private final String myName;
  private final Map<DBAttribute<?>, Function<DBWriter, ?>> myInitializers = Collections15.linkedHashMap();

  public DBIdentifiedObject(String id) {
    this(id, null, true);
  }

  public DBIdentifiedObject(String id, String name) {
    this(id, name, true);
  }

  /**
   * Special constructor for initializing bootstrap attributes
   */
  DBIdentifiedObject(String id, String name, boolean initialize) {
    myId = id;
    myName = name;
    if (initialize) {
      initialize(DBAttribute.ID, id);
      initialize(DBAttribute.NAME, name);
    }
  }

  public final String getId() {
    return myId;
  }

  public String getName() {
    return myName;
  }

  public final <T> void initialize(DBAttribute<T> attribute, Function<DBWriter, T> function) {
    if (attribute == null)
      throw new NullPointerException();
    if (function == null)
      throw new NullPointerException();
    myInitializers.put(attribute, function);
  }

  public final <T> void initialize(DBAttribute<T> attribute, T value) {
    initialize(attribute, InitConst.create(value));
  }

  public final void initialize(DBAttribute<Long> attribute, DBIdentifiedObject object) {
    initialize(attribute, new InitIdentified(object));
  }

  @Override
  public final long findItem(DBReader reader) {
    return reader.findMaterialized(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    DBIdentifiedObject that = (DBIdentifiedObject) o;

    if (!myId.equals(that.myId))
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myId.hashCode();
  }

  @Override
  public String toString() {
    return myName != null ? myName : myId;
  }

  public void reinitializeItem(long item, DBWriter writer) {
    assert writer.findMaterialized(this) == item : item + " " + this;
    for (Map.Entry<DBAttribute<?>, Function<DBWriter, ?>> e : myInitializers.entrySet()) {
      DBAttribute attribute = e.getKey();
      Object value = e.getValue().invoke(writer); // tris could call materialize in recursion
      writer.setValue(item, attribute, value);
    }
  }
}
