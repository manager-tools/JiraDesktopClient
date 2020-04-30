package com.almworks.items.util;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.WritableLongList;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.util.List;

public class SlaveUtils {
  public static final DBAttribute<Boolean> MASTER_REFERENCE  = DBItemType.ATTR_NS.bool("masterReference", "Master?", false);

  private static final BoolExpr<DP> EXPR_MASTER_REF = BoolExpr.and(
    DPEqualsIdentified.create(DBAttribute.TYPE, DBItemType.ATTRIBUTE),
    DPEquals.create(MASTER_REFERENCE, Boolean.TRUE));

  public static List<DBAttribute<Long>> getMasterAttributes(DBReader reader) {
    List<DBAttribute<Long>> attributes = Collections15.arrayList();
    LongList attrs = reader.query(EXPR_MASTER_REF).copyItemsSorted();
    for (int i = 0; i < attrs.size(); i++) {
      long a = attrs.get(i);
      DBAttribute attribute = BadUtil.getAttribute(reader, a);
      if (attribute == null) continue;
      if (!Long.class.equals(attribute.getScalarClass())) continue;
      if (attribute.getComposition() != DBAttribute.ScalarComposition.SCALAR) continue;
      if (!isMasterReference(reader, a)) continue;
      if (!attribute.isPropagatingChange()) continue;
      attributes.add(attribute);
    }
    return attributes;
  }

  public static LongList getSlaves(DBReader reader, long item) {
    LongArray slaves = new LongArray();
    collectSlaves(reader, item, slaves, getMasterAttributes(reader));
    return slaves.isEmpty() ? LongList.EMPTY : slaves;
  }

  private static void collectSlaves(DBReader reader, long item, WritableLongList target, List<DBAttribute<Long>> masterRefs) {
    LongList slaves = reader.query(querySlaves(item, masterRefs)).copyItemsSorted();
    for (int i = 0; i < slaves.size(); i++) {
      long slave = slaves.get(i);
      if (target.addSorted(slave)) collectSlaves(reader, slave, target, masterRefs);
    }
  }

  private static BoolExpr<DP> querySlaves(long artifact, List<DBAttribute<Long>> masterAttributes) {
    if (masterAttributes.isEmpty()) return BoolExpr.FALSE();
    BoolExpr<DP>[] exprs = new BoolExpr[masterAttributes.size()];
    for (int i = 0, attributesSize = masterAttributes.size(); i < attributesSize; i++) {
      DBAttribute<Long> attribute = masterAttributes.get(i);
      exprs[i] = DPEquals.create(attribute, artifact);
    }
    return BoolExpr.or(exprs);
  }

  private static boolean isMasterReference(DBReader reader, long attrItem) {
    return Boolean.TRUE.equals(reader.getValue(attrItem, MASTER_REFERENCE));
  }

  public static DBAttribute<Long> masterReference(String id, String name) {
    DBAttribute<Long> attribute = DBAttribute.Link(id, name, true);
    attribute.initialize(MASTER_REFERENCE, true);
    return attribute;
  }
}
