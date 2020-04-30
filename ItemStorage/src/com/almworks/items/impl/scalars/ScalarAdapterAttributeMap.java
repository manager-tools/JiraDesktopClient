package com.almworks.items.impl.scalars;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.impl.AttributeAdapter;
import com.almworks.items.impl.AttributeCache;
import com.almworks.items.impl.DBWriterImpl;
import com.almworks.items.impl.ScalarValueAdapter;
import com.almworks.items.impl.dbadapter.DBColumn;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.items.util.AttributeMap;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Log;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.*;
import java.util.Set;

// written attributes should be materialized!
public class ScalarAdapterAttributeMap extends ScalarValueAdapter<AttributeMap> {
  private static final int SIGNATURE = 0xA1151B0E;

  @Override
  public Class<AttributeMap> getAdaptedClass() {
    return AttributeMap.class;
  }

  @Override
  public DBColumn getScalarColumn() {
    return BLOB_VALUE;
  }

  @Override
  public boolean isIndexable() {
    return false;
  }

  @Override
  public AttributeMap loadUserValue(SQLiteStatement select, int columnIndex, TransactionContext context)
    throws SQLiteException
  {
    InputStream is = select.columnStream(columnIndex);
    if (is == null)
      return null;
    try {
      DataInput in = new DataInputStream(is);
      return readValueFromStream(in, context);
    } catch (IOException e) {
      Log.warn("cannot read value", e);
      return null;
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // ignore
      }
    }
  }

  @Override
  protected AttributeMap readValueFromStream(DataInput in, TransactionContext context) throws IOException {
    int signature = in.readInt();
    if (signature != SIGNATURE)
      throw new IOException("bad signature");
    AttributeMap r = new AttributeMap();
    int mapLength = CompactInt.readInt(in);
    if (mapLength < 0)
      throw null;
    for (int i = 0; i < mapLength; i++) {
      String id = CompactChar.readString(in);
      DBAttribute attribute = AttributeCache.getAttribute(id, context);
      if (attribute == null) {
        throw new IOException("cannot locate attribute " + id + "");
      }
      AttributeAdapter adapter = context.getDatabaseContext().getAttributeAdapter(attribute);
      if (adapter == null) {
        throw new IOException("cannot read " + attribute.getId());
      }
      Object value = adapter.readValueFromStream(in, context);
      r.put(attribute, value);
    }
    return r;
  }

  @Override
  protected void writeValueToStream(DataOutput out, AttributeMap userValue, TransactionContext context)
    throws IOException
  {
    out.writeInt(SIGNATURE);
    if (userValue == null) {
      CompactInt.writeInt(out, -1);
      return;
    }
    Set<DBAttribute<?>> attributes = userValue.keySet();
    CompactInt.writeInt(out, attributes.size());
    for (DBAttribute attribute : attributes) {
      CompactChar.writeString(out, attribute.getId());
      AttributeAdapter adapter = context.getDatabaseContext().getAttributeAdapter(attribute);
      if (adapter == null) {
        throw new IOException("cannot write " + attribute.getId());
      }
      adapter.writeValueToStream(out, userValue.get(attribute), context);
    }
  }

  @Override
  public void bindParameter(SQLiteStatement statement, int bindIndex, AttributeMap userValue,
    TransactionContext context) throws SQLiteException
  {
    OutputStream os = statement.bindStream(bindIndex);
    try {
      DataOutputStream out = new DataOutputStream(os);
      writeValueToStream(out, userValue, context);
      out.close();
    } catch (IOException e) {
      Log.warn("cannot write value", e);
      statement.bindZeroBlob(bindIndex, 0);
    } finally {
      try {
        os.close();
      } catch (IOException e) {
        // ignore
      }
    }
 
    // materialize attributes
    if (userValue != null) {
      Set<DBAttribute<?>> attributes = userValue.keySet();
      if (!attributes.isEmpty()) {
        DBWriterImpl writer = new DBWriterImpl(context, null);
        for (DBAttribute<?> attribute : attributes) {
          writer.materialize(attribute);
        }
      }
    }
  }

  @Override
  public Object toSearchValue(AttributeMap userValue) {
    return null;
  }
}
