package com.almworks.api.application.field;

import com.almworks.util.collections.Containers;
import org.almworks.util.Util;

import java.math.BigDecimal;
import java.util.Comparator;

public class AdaptiveComparator<T extends Comparable> implements Comparator<T> {
  private static final AdaptiveComparator INSTANCE = new AdaptiveComparator();

  public static <T> Comparator<T> instance() {
    return INSTANCE;
  }

  public int compare(T o1, T o2) {
    if (o1 == o2) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    if (o1 instanceof String && o2 instanceof String) {
      return compareStrings((String) o1, (String) o2);
    } else if (o1 instanceof Comparable) {
      return ((Comparable) o1).compareTo(o2);
    } else {
      assert false : o1 + " " + o2;
      return 0;
    }
  }

  private int compareStrings(String s1, String s2) {
    boolean num1 = isNumber(s1);
    boolean num2 = isNumber(s2);
    if (num1) {
      if (num2) {
        return compareNumbers(s1, s2);
      } else {
        return -1;
      }
    } else {
      if (num2) {
        return 1;
      } else {
        return s1.compareToIgnoreCase(s2);
      }
    }
  }

  private int compareNumbers(String s1, String s2) {
    long n1 = Util.toLong(s1, Long.MIN_VALUE);
    long n2 = Util.toLong(s2, Long.MIN_VALUE);
    if (n1 > Long.MIN_VALUE && n2 > Long.MIN_VALUE) {
      return Containers.compareLongs(n1, n2);
    }
    try {
      BigDecimal d1 = new BigDecimal(s1);
      BigDecimal d2 = new BigDecimal(s2);
      return d1.compareTo(d2);
    } catch (Exception e) {
      // ignore
    }
    // fallback
    return s1.compareToIgnoreCase(s2);
  }

  /**
   * Automaton:
   * <table>
   * <tr><th></th>      <th>init</th> <th>n</th> <th>d</th> <th>ns</th> <th>nd</th> <th>nds</th> <th>p</th>
   * <tr><td>space</td> <td>init</td> <td>p</td> <td>FALSE</td> <td>p</td> <td>p</td> <td>p</td> <td>p</td></tr>
   * <tr><td>dot</td> <td>d</td> <td>nd</td> <td>FALSE</td> <td>FALSE</td> <td>FALSE</td> <td>FALSE</td> <td>FALSE</td></tr>
   * <tr><td>sign</td> <td>FALSE</td> <td>ns</td> <td>FALSE</td> <td>FALSE</td> <td>nds</td> <td>FALSE</td> <td>FALSE</td></tr>
   * <tr><td>digit</td> <td>n</td> <td>n</td> <td>nd</td> <td>FALSE</td> <td>nd</td> <td>FALSE</td> <td>FALSE</td></tr>
   * </table>
   */
  static boolean isNumber(String s) {
    // states
    final int init = 0;
    // current tail is [0-9]+[ ]*
    final int n = 1;
    // current tail is [.][ ]*
    final int d = 2;
    // current tail is [+-][0-9]+[ ]*
    final int ns = 3;
    // current tail is [0-9]+[.][0-9]+[ ]*
    final int nd = 4;
    // current tail is [+-][0-9]+[.][0-9]+[ ]*
    final int nds = 5;
    // eating up whitespaces in the beginning
    final int p = 6;
    int state = init;
    for (int k = s.length() - 1; k >= 0; --k) {
      char c = s.charAt(k);
      if (c == ' ') {
        if (state == d) {
          return false;
        } else if (state != init) {
          state = p;
        }
      } else if (c == '.') {
        if (state == init) {
          state = d;
        } else if (state == n) {
          state = nd;
        } else {
          return false;
        }
      } else if (c == '+' || c == '-') {
        if (state == n) {
          state = ns;
        } else if (state == nd) {
          state = nds;
        } else {
          return false;
        }
      } else if (c >= '0' && c <= '9') {
        if (state == init || state == n) {
          state = n;
        } else if (state == d || state == nd) {
          state = nd;
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
    return state == n || state == ns || state == nd || state == nds || state == p;
  }
}
