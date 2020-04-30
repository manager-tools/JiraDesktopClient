package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;

public class DPNotNull<V> extends DPAttribute<V> {
  public DPNotNull(DBAttribute<V> attribute) {
    super(attribute);
  }

  public static BoolExpr<DP> create(DBAttribute<?> attribute) {
    return new DPNotNull(attribute).term();
  }

  @Override
  protected boolean acceptValue(V value, DBReader reader) {
    return value != null;
  }

  @Override
  public String toString() {
    return getAttribute() + " is set";
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    return true;
  }

  @Override
  protected int hashCodeDPA() {
    return DPNotNull.class.hashCode();
  }
}
