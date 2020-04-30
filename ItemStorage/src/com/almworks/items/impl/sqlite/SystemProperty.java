package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.DBProperty;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import org.almworks.util.Log;
import org.almworks.util.TypedKeyRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SystemProperty<T> extends DBProperty<T> {
  private static final TypedKeyRegistry<SystemProperty> REGISTRY = TypedKeyRegistry.create();

  private final int myId;

  protected SystemProperty(int id, @NotNull String guid, @Nullable Class<T> valueClass) {
    super(verify(id, guid), valueClass, REGISTRY);
    myId = id;
  }

  private static String verify(int id, String guid) {
    for (SystemProperty property : REGISTRY.getRegisteredKeys()) {
      if (id == property.getId()) {
        throw new IllegalArgumentException("id " + id + " reserved for " + property);
      }
      if (guid.equals(property.getName())) {
        throw new IllegalArgumentException("name " + guid + " reserved for " + property);
      }
    }
    return guid;
  }

  void initialize(SQLiteConnection db) throws SQLiteException {
    T oldValue = Schema.getProperty(db, this);
    T newValue = initializeValue(db, oldValue);
    if (newValue != null) {
      Log.debug(this + ": " + formatValue(newValue) + " [reset]");
      Schema.setProperty(db, this, newValue);
    } else {
      Log.debug(this + ": " + formatValue(oldValue));
    }
  }

  protected T initializeValue(SQLiteConnection db, T dbValue) throws SQLiteException {
    return null;
  }

  public int getId() {
    return myId;
  }

  public String formatValue(T value) {
    return String.valueOf(value);
  }

  @NotNull
  public String toString() {
    return super.toString() + "[" + myId + "]";
  }

  public static Collection<SystemProperty> all() {
    return REGISTRY.getRegisteredKeys();
  }
}
