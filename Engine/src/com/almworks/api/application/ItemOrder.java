package com.almworks.api.application;

import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Define possible ordering of ItemKeys.
 * <p/>
 * The order:<br>
 * 1. Those that have most numerics
 * 2. Those that have least value in higher-level numeric.
 * 3. Comparing by string, case-insensitively
 */
public final class ItemOrder implements Comparable<ItemOrder> {
  public static final ItemOrder NO_ORDER = new ItemOrder(null, null, null, false);

  @Nullable
  private final Long myNumeric0;

  @Nullable
  private final long[] myNumerics;

  @Nullable
  private final String myString;
  private final boolean myNoAdjust;

  private ItemOrder(Long numeric0, long[] numerics, String string, boolean noAdjust) {
    myNumeric0 = numeric0;
    myNumerics = numerics;
    myString = string;
    myNoAdjust = noAdjust;
  }

  public int compareTo(ItemOrder that) {
    if (this == that)
      return 0;

    // 1. Instance with greater magnitude will come first
    int m1 = getNumericMagnitude();
    int m2 = that.getNumericMagnitude();
    if (m1 > m2)
      return -1;
    else if (m1 < m2)
      return 1;

    // 2. Instance with lowest value in the first differing numeric order will come first
    assert m1 == m2;
    for (int i = 0; i < m1; i++) {
      long v1 = getNumeric(i);
      long v2 = that.getNumeric(i);
      if (v1 < v2)
        return -1;
      else if (v1 > v2)
        return 1;
    }

    // 3. Instance that has a string order will come before that without a string order.
    // 4. If both have string order then compare it.
    String s1 = getString();
    String s2 = that.getString();
    if (s1 == null) {
      if (s2 == null) {
        return 0;
      } else {
        return 1;
      }
    } else {
      if (s2 == null) {
        return -1;
      } else {
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
      }
    }
  }

  public String getString() {
    return myString;
  }

  public long getNumeric(int i) {
    assert i >= 0;
    if (i == 0) {
      if (myNumeric0 != null) {
        return myNumeric0;
      } else if (myNumerics != null && myNumerics.length > 0) {
        return myNumerics[0];
      } else {
        assert false : i + " " + this;
        return Long.MAX_VALUE;
      }
    } else {
      if (myNumeric0 != null) {
        i--;
      }
      if (myNumerics != null && myNumerics.length > i) {
        return myNumerics[i];
      } else {
        assert false : i + " " + this;
        return Long.MAX_VALUE;
      }
    }
  }

  public int getNumericMagnitude() {
    int result = 0;
    if (myNumeric0 != null)
      result++;
    if (myNumerics != null)
      result += myNumerics.length;
    return result;
  }

  public String toString() {
    StringBuffer buffer = new StringBuffer();
    if (myNumeric0 != null) {
      buffer.append((long) myNumeric0);
    }
    if (myNumerics != null) {
      for (long value : myNumerics) {
        if (buffer.length() > 0)
          buffer.append(':');
        buffer.append(value);
      }
    }
    if (myString != null) {
      if (buffer.length() > 0)
        buffer.append("::");
      buffer.append(myString);
    }
    buffer.insert(0, '[');
    buffer.append(']');
    return buffer.toString();
  }

  @NotNull
  public static ItemOrder byString(String string) {
    return new ItemOrder(null, null, string == null ? string : string.intern(), false);
  }

  public static ItemOrder byStringNoAdjust(String string) {
    return string == null ? byString(string) : new ItemOrder(null, null, string.intern(), true);
  }

  @NotNull
  public static ItemOrder byOrderAndString(long value, String string) {
    return new ItemOrder(value, null, string == null ? string : string.intern(), false);
  }

  @NotNull
  public static ItemOrder byOrder(long value) {
    return new ItemOrder(value, null, null, false);
  }

  @NotNull
  public static ItemOrder adjustString(@Nullable ItemOrder order, @Nullable String string) {
    if (order == null) {
      return byString(string);
    } else {
      if (order.myNoAdjust) return order;
      if (Util.equals(order.getString(), string)) {
        return order;
      } else {
        return new ItemOrder(order.myNumeric0, order.myNumerics, string, false);
      }
    }
  }

  public static ItemOrder byGroup(String string, long[] values) {
    return new ItemOrder(null, values, string, false);
  }

  public static ItemOrder byNumbers(long ... values) {
    return new ItemOrder(null, values, null, false);
  }

  @Nullable
  public static ItemOrder byObject(Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Number) {
      return ItemOrder.byOrder(((Number)value).longValue());
    } else if (value instanceof String) {
      return ItemOrder.byString((String)value);
    } else {
      assert false : value;
      return ItemOrder.byString(String.valueOf(value));
    }
  }

  @NotNull
  public static ItemOrder byNotNullObject(@NotNull Object value) {
    ItemOrder order = byObject(value);
    assert order != null : value;
    return order;
  }


  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    ItemOrder that = (ItemOrder) o;

    if (myNumeric0 != null ? !myNumeric0.equals(that.myNumeric0) : that.myNumeric0 != null)
      return false;
    if (!Arrays.equals(myNumerics, that.myNumerics))
      return false;
    if (myString != null ? !myString.equals(that.myString) : that.myString != null)
      return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = (myNumeric0 != null ? myNumeric0.hashCode() : 0);
    result = 31 * result + (myNumerics != null ? Arrays.hashCode(myNumerics) : 0);
    result = 31 * result + (myString != null ? myString.hashCode() : 0);
    return result;
  }
}
