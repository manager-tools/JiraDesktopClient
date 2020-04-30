package com.almworks.tracker.eapi.alpha;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains predefined keys and utility methods for them.
 */
public class GenericKeys {
  public static final ArtifactPresentationKey<String> ID = id();
  public static final ArtifactPresentationKey<String> SUMMARY = longString("Summary", true);
  public static final ArtifactPresentationKey<String> LONG_DESCRIPTION_HTML =
    longString("LONG_DESCRIPTION_HTML", false);
  public static final ArtifactPresentationKey<String> SHORT_DESCRIPTION_HTML =
    longString("SHORT_DESCRIPTION_HTML", false);

  private static Map<String, ArtifactPresentationKey<?>> myKeyRegistry;

  /**
   * Use this method to serialize an artifact key to a string, so it could be stored away.
   *
   * @return null if the key cannot be serialized, or a externalizable string
   */
  public static String toExternalForm(ArtifactPresentationKey<?> key) {
    String id = key.getName();
    if (!myKeyRegistry.containsKey(id)) {
      assert false : id;
      return null;
    }
    return id;
  }

  /**
   * Use this method to deserialize previously store key.
   *
   * @return null if the key cannot be serialized or it is of an incompatible class, or a valid key otherwise
   */
  public static <T> ArtifactPresentationKey<T> fromExternalForm(String id, Class<T> valueClass) {
    ArtifactPresentationKey<?> key = myKeyRegistry.get(id);
    if (key != null && !valueClass.isAssignableFrom(key.getValueClass())) {
      assert false : valueClass + " " + key.getValueClass();
      key = null;
    }
    return (ArtifactPresentationKey<T>) key;
  }


  private static ArtifactPresentationKey<String> longString(String name, boolean cellPresentable) {
    return string(name, String.CASE_INSENSITIVE_ORDER, cellPresentable);
  }

  private static Map<String, ArtifactPresentationKey<?>> getRegistry() {
    if (myKeyRegistry == null)
      myKeyRegistry = Collections.synchronizedMap(new HashMap<String, ArtifactPresentationKey<?>>());
    return myKeyRegistry;
  }

  private static ArtifactPresentationKey<String> id() {
    return string("ID", new IdComparator(), true);
  }

  private static void register(ArtifactPresentationKey<?> key) {
    Map<String, ArtifactPresentationKey<?>> registry = getRegistry();
    registry.put(key.getName(), key);
  }

  private static ArtifactPresentationKey<String> string(String name, Comparator<String> comparator, boolean cellable) {
    ArtifactPresentationKey<String> key = new ArtifactPresentationKey<String>(name, String.class, comparator, cellable);
    register(key);
    return key;
  }

  /**
   * This comparator orders String artifact IDs that may take form of [prefix]number[suffix].
   * First order is prefix. Second order is number. Third order is suffix.
   * Plain number without prefix go after prefixed numbers.
   */
  public static class IdComparator implements Comparator<String> {
    public int compare(String id1, String id2) {
      if (id1 == null)
        id1 = "";
      if (id2 == null)
        id2 = "";
      if (id1 == id2 || id1.equals(id2))
        return 0;
      if (id1.length() == 0) {
        return id2.length() == 0 ? 0 : 1;
      }
      if (id2.length() == 0) {
        return -1;
      }
      int d1 = getFirstDigitIndex(id1);
      int d2 = getFirstDigitIndex(id2);
      if (d1 > 0) {
        if (d2 == 0) {
          // prefixed first
          return -1;
        } else {
          int diff = String.CASE_INSENSITIVE_ORDER.compare(id1.substring(0, d1), id2.substring(0, d2));
          if (diff != 0) {
            return diff;
          }
        }
      } else {
        if (d2 > 0) {
          return 1;
        }
      }
      assert d1 == d2 : d1 + " " + d2;
      int e1 = getFirstNonDigitIndex(id1, d1);
      int e2 = getFirstNonDigitIndex(id2, d2);
      int val1, val2;
      try {
        val1 = Integer.parseInt(id1.substring(d1, e1));
      } catch (NumberFormatException e) {
        assert false : id1;
        return 1;
      }
      try {
        val2 = Integer.parseInt(id2.substring(d2, e2));
      } catch (NumberFormatException e) {
        assert false : id2;
        return -1;
      }
      if (val1 != val2) {
        return val1 < val2 ? -1 : 1;
      } else {
        return String.CASE_INSENSITIVE_ORDER.compare(id1.substring(e1), id2.substring(e2));
      }
    }

    private int getFirstDigitIndex(String s) {
      int len = s.length();
      for (int i = 0; i < len; i++) {
        char c = s.charAt(i);
        if (c >= '0' && c <= '9') {
          return i;
        }
      }
      return len;
    }

    private int getFirstNonDigitIndex(String s, int digitStart) {
      int len = s.length();
      for (int i = digitStart; i < len; i++) {
        char c = s.charAt(i);
        if (c < '0' || c > '9') {
          return i;
        }
      }
      return len;
    }
  }
}
