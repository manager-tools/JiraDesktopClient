package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.items.api.ItemReference;
import com.almworks.util.bool.BoolExpr;

public class DPEqualsIdentified extends DPAttribute<Long> {
  private final ItemReference myValue;

  private DPEqualsIdentified(DBAttribute<Long> attribute, ItemReference value) {
    super(attribute);
    myValue = value != null ? value : ItemReference.NO_ITEM;
  }

  public ItemReference getValue() {
    return myValue;
  }

  public static BoolExpr<DP> create(DBAttribute<Long> attribute, ItemReference value) {
    return new DPEqualsIdentified(attribute, value).term();
  }

  @Override
  protected boolean acceptValue(Long value, DBReader reader) {
    if (value == null) return false;
    long m = myValue.findItem(reader);
    if (m == 0) return false;
    return m == value;
  }

  @Override
  public String toString() {
    return getAttribute() + " == " + myValue;
  }

  @Override
  protected int hashCodeDPA() {
    return myValue.hashCode();
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    return myValue.equals(((DPEqualsIdentified)other).myValue);
  }
}