package com.almworks.items.impl.migrations;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBWriter;
import com.almworks.items.dp.DPEqualsIdentified;
import org.almworks.util.Log;

public class ObfuscatedAttributeMap extends DBMigrationProcedure {
  public ObfuscatedAttributeMap() {
    super("com.almworks.items.migrations.ObfuscatedAttributeMap");
  }

  @Override
  protected boolean migrate(DBWriter w) {
    final LongArray attrs = w.query(DPEqualsIdentified.create(DBAttribute.TYPE, DBItemType.ATTRIBUTE)).copyItemsSorted();
    for(final LongIterator it = attrs.iterator(); it.hasNext();) {
      final long attr = it.nextValue();
      final String clazz = w.getValue(attr, DBAttribute.SCALAR_CLASS);
      if (clazz == null) {
        // no scalar class? possibly this attribute has been deleted...
        Log.error("No scalar class for " + attr + " " + w.getAttributeMap(attr));
        continue;
      }
      if(!clazz.startsWith("java.") && !clazz.startsWith("com.almworks.") && !clazz.equals("[B")) {
        w.setValue(attr, DBAttribute.SCALAR_CLASS, "com.almworks.items.util.AttributeMap");
      }
    }
    return true;
  }
}
