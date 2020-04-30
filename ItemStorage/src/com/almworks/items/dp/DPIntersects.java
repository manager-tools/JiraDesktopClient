package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.Set;

public class DPIntersects<V> extends DPAttribute<Collection<V>> {
  private final Set<V> myValues;

  private DPIntersects(DBAttribute<Collection<V>> attribute, Set<V> values) {
    super(attribute);
    // we're using collections and equals(), so:
    if (attribute.getScalarClass().equals(byte[].class))
      throw new IllegalArgumentException("cannot intersect byte[]");
    myValues = values;
  }

  public Set<V> getValues() {
    return myValues;
  }

  public static <T, V extends T> BoolExpr<DP> create(DBAttribute<? extends Collection<T>> attribute,
    Collection<V> values)
  {
    return new DPIntersects(attribute, Collections15.hashSet(values)).term();
  }

  @Override
  protected boolean acceptValue(Collection<V> value, DBReader reader) {
    if (value == null)
      return false;
    for (V v : value) {
      if (myValues.contains(v))
        return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return getAttribute() + " X " + myValues;
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    return myValues.equals(((DPIntersects) other).myValues);
  }

  @Override
  protected int hashCodeDPA() {
    return myValues.hashCode();
  }
}