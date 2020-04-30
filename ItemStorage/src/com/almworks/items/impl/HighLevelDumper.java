package com.almworks.items.impl;

import com.almworks.integers.LongArray;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.dp.DPNotNull;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.DumperUtil;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighLevelDumper {
  private final DBReader myReader;

  public HighLevelDumper(DBReader reader) {
    myReader = reader;
  }

  public void dump(PrintStream writer) {
    dumpIdentified(writer);
    dumpArtifactsByKind(writer);
  }

  private void dumpArtifactsByKind(PrintStream writer) {
    LongArray kinds =
      myReader.query(DPEqualsIdentified.create(DBAttribute.TYPE, DBItemType.TYPE)).copyItemsSorted();
    for (int i = 0; i < kinds.size(); i++) {
      long kind = kinds.get(i);
      String name = DBAttribute.NAME.getValue(kind, myReader);
      String id = DBAttribute.ID.getValue(kind, myReader);
      header(writer, name + " (kind:" + kind + " id:" + id + ")");
      dumpArtifacts(writer, DPEquals.create(DBAttribute.TYPE, kind));
      footer(writer);
    }

    header(writer, "NULL KIND");
    dumpArtifacts(writer, DPNotNull.create(DBAttribute.TYPE).negate());
    footer(writer);
  }

  private void dumpArtifacts(PrintStream writer, BoolExpr<DP> filter) {
    LongArray artifacts = myReader.query(filter).copyItemsSorted();
    for (int i = 0; i < artifacts.size(); i++) {
      long a = artifacts.get(i);
      dumpArtifact(writer, a);
      writer.println();
    }
  }

  private void dumpArtifact(PrintStream writer, long a) {
    dumpSnapshot(writer, a);
  }

  private void dumpSnapshot(PrintStream writer, long a) {
    AttributeMap map = myReader.getAttributeMap(a);
    String prefix = "A:" + a;
    dumpMap(writer, map, prefix);
  }

  private void dumpMap(PrintStream writer, AttributeMap map, String prefix) {
    if (map == null) return;
    Map<String, String> vmap = new HashMap<String, String>();
    List<DBAttribute<?>> after = Collections15.arrayList();
    for (DBAttribute<?> attribute : map.keySet()) {
      String key = attribute.getName();
      if (key == null) key = attribute.getId();
      if (attribute.getScalarClass().equals(AttributeMap.class)) {
        after.add(attribute);
        key = "| " + key;
//        continue;
      }
      String value = decorate(map.get(attribute));
      vmap.put(key, value);
    }
    if (vmap.isEmpty()) {
      writer.println(prefix);
      writer.println("  <empty>");
    } else {
      writer.println(prefix);
      DumperUtil.printMap(writer, vmap, 2);
    }
//    for (DBAttribute<?> databaseAttribute : after) {
//      AttributeMap m = (AttributeMap) map.get(databaseAttribute);
//      if (m != null) {
//        writer.println();
//        dumpMap(writer, m, prefix + " ++shadow++ " + databaseAttribute.getName());
//      }
//    }
  }

  private String decorate(Object value) {
    if (value instanceof String) {
      return "\"" + value + "\"";
    } else if (value instanceof Long) {
      long v = (long) (Long) value;
      String r = String.valueOf(value);
      Object name = DBAttribute.NAME.getValue(v, myReader);
      Object kind = DBAttribute.TYPE.getValue(v, myReader);
      if (name == null && kind == null)
        return r;
      if (kind instanceof Long) {
        kind = DBAttribute.NAME.getValue((Long)kind, myReader);
      }
      r = r + " (" + (kind == null ? "" : kind + ":") + (name == null ? "" : name + "") + ")";
      return r;
    } else if (value instanceof byte[]) {
      return DumperUtil.getBytesRepresentation((byte[]) value);
    } else if (value instanceof AttributeMap) {
      try {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(b, true, "UTF-8");
        dumpMap(ps, (AttributeMap) value, "---------------");
        ps.close();
        return new String(b.toByteArray(), "UTF-8");
      } catch (IOException e) {
        return "? " + e;
      }
    }
    return String.valueOf(value);
  }


  private void dumpIdentified(PrintStream writer) {
    header(writer, "IDENTIFIED MAP");
    Map<String, String> map = new HashMap<String, String>();
    LongArray artifacts = myReader.query(DPNotNull.create(DBAttribute.ID)).copyItemsSorted();
    for (int i = 0; i < artifacts.size(); i++) {
      long a = artifacts.get(i);
      String id = DBAttribute.ID.getValue(a, myReader);
      assert a != 0 && id != null;
      map.put("" + a, id);
    }
    DumperUtil.printMap(writer, map, 2);
    footer(writer);
  }

  private void header(PrintStream writer, String h) {
    writer.println("=====================================================================================");
    writer.println("*** " + h);
  }

  private void footer(PrintStream writer) {
    writer.println("=====================================================================================");
    writer.println();
  }
}
