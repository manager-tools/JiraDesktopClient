package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import org.almworks.util.Util;

import java.util.Collection;

public abstract class DPAttribute<V> extends DP {
  private final DBAttribute<V> myAttribute;

  protected DPAttribute(DBAttribute<V> attribute) {
    myAttribute = attribute;
  }

  public DBAttribute<V> getAttribute() {
    return myAttribute;
  }

  @Override
  public final boolean accept(long item, DBReader reader) {
    V value = myAttribute.getValue(item, reader);
    return acceptValue(value, reader);
  }

  @Override
  public boolean addAffectingAttributes(Collection<? super DBAttribute> target) {
    target.add(myAttribute);
    return true;
  }

  protected abstract boolean acceptValue(V value, DBReader reader);

  @Override
  protected boolean equalDP(DP other) {
    if (!Util.equals(myAttribute, ((DPAttribute)other).myAttribute))
      return false;
    return equalDPA((DPAttribute)other);
  }

  @Override
  protected int hashCodeDP() {
    return (myAttribute == null ? 0 : myAttribute.hashCode()) * 37 + hashCodeDPA();
  }

  protected abstract boolean equalDPA(DPAttribute other);

  protected abstract int hashCodeDPA();
}
