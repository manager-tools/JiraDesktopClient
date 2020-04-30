package com.almworks.items.impl;

import com.almworks.items.impl.sqlite.TransactionContext;

import java.io.PrintStream;

public class Dumper {
  private final TransactionContext myContext;

  public Dumper(TransactionContext context) {
    myContext = context;
  }
/*

  public void dump(PrintStream out) {
//    out.println("*** DB STATS ***");
//    Map stats = new HashMap();
//    stats.put("Next Artifact", myNextArtifact);
//    DumperUtil.printMap(out, stats, 0);
//    out.println();

    out.println("*** IDENTIFIED ***");

    DumperUtil.printMap(out, myIdentifiedObjects, 0);
    out.println();

    LongArray kinds =
      filter(DPEqualsIdentified.create(DBAttribute.KIND, DatabaseArtifactKind.KIND)).copyArtifacts();

    for (int i = 0; i < kinds.size(); i++) {
      LongArray aa = filter(DPEquals.create(DBAttribute.KIND, kinds.get(i))).copyArtifacts();
      out.println("*** " + nameOrId(kinds.get(i)) + " (" + aa.size() + " artifacts) ***");
      for (int j = 0; j < aa.size(); j++) {
        dumpArtifact(out, aa.get(j));
      }
      out.println("");
    }

    LongArray aa = filter(DPNotNull.create(DBAttribute.KIND).negate()).copyArtifacts();
    if (aa.size() > 0) {
      out.println("*** <no kind> ***");
      for (int j = 0; j < aa.size(); j++) {
        dumpArtifact(out, aa.get(j));
      }
      out.println("");
    }
  }

  private String nameOrId(long a) {
    Object name = v(a, DBAttribute.NAME);
    if (name != null)
      return String.valueOf(name);
    name = v(a, DBAttribute.ID);
    if (name != null)
      return String.valueOf(name);
    return "R:" + a;
  }

  private void dumpArtifact(PrintStream out, long artifact) {
    out.println("A:" + artifact);
    Map values = new HashMap();
    Map valuesSync = new HashMap();
    Map valuesServer = new HashMap();
    for (Map.Entry<String, TLongObjectHashMap> e : myValues.entrySet()) {
      DBAttribute attr = myAttributes.get(e.getKey());
      TLongObjectHashMap vm = e.getValue();
      Object value = vm.get(artifact);
      if (value != null) {
        values.put(attr, decorate(value));
      }
      value = vm.get(ImplUtil.p(artifact, Database.SYNCED_OLD));
      if (value != null) {
        valuesSync.put(attr, value);
      }
      value = vm.get(ImplUtil.p(artifact, Database.SERVER_SHADOW_OLD));
      if (value != null) {
        valuesServer.put(attr, value);
      }
    }
    if (values.isEmpty()) {
      out.println("  <empty>");
    } else {
      DumperUtil.printMap(out, values, 2);
    }
    if (!valuesSync.isEmpty()) {
      out.println("+ SYNCED_OLD");
      DumperUtil.printMap(out, valuesSync, 2);
    }
    if (!valuesSync.isEmpty()) {
      out.println("+ SERVER SHADOW");
      DumperUtil.printMap(out, valuesServer, 2);
    }
    out.println();
  }

  private String decorate(Object value) {
    if (value instanceof String) {
      return "\"" + value + "\"";
    } else if (value instanceof Long) {
      long v = (long) (Long) value;
      String r = String.valueOf(value);
      if (!myExistingArtifacts.get((int) v)) {
        return r;
      }
      Object name = v(v, DBAttribute.NAME);
      Object kind = v(v, DBAttribute.KIND);
      if (name == null && kind == null)
        return r;
      if (kind instanceof Long) {
        kind = v((Long) kind, DBAttribute.NAME);
      }
      r = r + " (" + (kind == null ? "" : kind + ":") + (name == null ? "" : name + "") + ")";
      return r;
    }
    return String.valueOf(value);
  }

  private Object v(long v, DBAttribute attr) {
    TLongObjectHashMap map = myValues.get(attr.getId());
    Object name = map == null ? null : map.get(v);
    return name;
  }
*/

  public void dump(PrintStream writer) {
    
  }
}
