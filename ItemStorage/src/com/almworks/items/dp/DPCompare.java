package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;

import java.util.Arrays;

public class DPCompare<V extends Comparable> extends DPAttribute<V> {
  private final V myValue;
  private final boolean[] myAcceptable;
  private final boolean myAcceptNull;

  private DPCompare(DBAttribute<V> attribute, V value, boolean acceptLess, boolean acceptEqual, boolean acceptGreater, boolean acceptNull) {
    super(attribute);
    assert acceptLess ^ acceptGreater;
    myValue = value;
    myAcceptNull = acceptNull;
    myAcceptable = new boolean[] {acceptLess, acceptEqual, acceptGreater};
  }

  public static <V extends Comparable> BoolExpr<DP> less(DBAttribute<V> attribute, V sample, boolean acceptNull) {
    return new DPCompare<V>(attribute, sample, true, false, false, acceptNull).term();
  }

  public static <V extends Comparable> BoolExpr<DP> lessOrEqual(DBAttribute<V> attribute, V sample, boolean acceptNull) {
    return new DPCompare<V>(attribute, sample, true, true, false, acceptNull).term();
  }

  public static <V extends Comparable> BoolExpr<DP> greater(DBAttribute<V> attribute, V sample, boolean acceptNull) {
    return new DPCompare<V>(attribute, sample, false, false, true, acceptNull).term();
  }

  public static <V extends Comparable> BoolExpr<DP> greaterOrEqual(DBAttribute<V> attribute, V sample, boolean acceptNull) {
    return new DPCompare<V>(attribute, sample, false, true, true, acceptNull).term();
  }

  public boolean isLess() {
    assert myAcceptable[0] ^ myAcceptable[2];
    return myAcceptable[0];
  }

  public boolean isAcceptEqual() {
    return myAcceptable[1];
  }

  public boolean isAcceptNull() {
    return myAcceptNull;
  }

  public V getValue() {
    return myValue;
  }

  @Override
  protected boolean acceptValue(V value, DBReader reader) {
    if (value == null) return myAcceptNull;
    int c = value.compareTo(myValue);
    return myAcceptable[Math.min(Math.max(c, -1), 1) + 1];
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(String.valueOf(getAttribute())).append(' ');
    if (myAcceptable[0]) {
      b.append('<');
    } else if (myAcceptable[2]) {
      b.append('>');
    } else if (myAcceptable[1]) {
      b.append('=');
    }
    if (myAcceptable[1]) {
      b.append('=');
    }
    if (myAcceptNull) b.append('*');
    b.append(' ').append(myValue);
    return b.toString();
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    DPCompare dpCompare = (DPCompare) other;

    if (myAcceptNull != dpCompare.myAcceptNull)
      return false;
    if (!Arrays.equals(myAcceptable, dpCompare.myAcceptable))
      return false;
    if (!myValue.equals(dpCompare.myValue))
      return false;

    return true;
  }

  @Override
  protected int hashCodeDPA() {
    int result = 0;
    result = 31 * result + myValue.hashCode();
    result = 31 * result + Arrays.hashCode(myAcceptable);
    result = 31 * result + (myAcceptNull ? 1 : 0);
    return result;
  }
}