package com.almworks.dbproperties;

import com.almworks.items.api.*;
import com.almworks.items.sync.DBDrain;
import com.almworks.items.sync.ItemProxy;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import org.jetbrains.annotations.Nullable;

public class DBPropertiesComponent {
  private static final DBNamespace NS = DBNamespace.moduleNs("com.almworks.dbproperties");
  private static final DBNamespace PROPERTY_NS = NS.subNs("properties");
  private static final DBAttribute<byte[]> PROPERTY_VALUE = NS.bytes("propertyValue");

  @Nullable
  public static byte[] getValue(DBReader reader, long property) {
    return PROPERTY_VALUE.getValue(property, reader);
  }

  public static byte[] getValue(DBReader reader, ItemReference property) {
    long item = property.findItem(reader);
    return item > 0 ? getValue(reader, item) : null;
  }

  public static void setValue(DBDrain db, long property, byte[] value) {
    db.changeItem(property).setValue(PROPERTY_VALUE, value);
  }

  public static void setValue(DBWriter writer, long property, byte[] value) {
    PROPERTY_VALUE.setValue(writer, property, value);
  }

  public static void setValue(DBDrain db, ItemProxy property, @Nullable byte[] value) {
    long item = property.findOrCreate(db);
    if (item > 0) setValue(db, item, value);
  }

  public static void setValue(DBDrain db, DBIdentifiedObject property, byte[] value) {
    long item = db.materialize(property);
    if (item > 0) setValue(db, item, value);
  }

  public static ItemProxy createProperty(String uniqueId) {
    return DBIdentity.fromDBObject(PROPERTY_NS.object(uniqueId));
  }
}
