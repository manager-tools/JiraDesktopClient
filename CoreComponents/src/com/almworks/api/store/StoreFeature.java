package com.almworks.api.store;

import org.almworks.util.Collections15;

import java.util.Map;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreFeature {
  private static final Map<String, StoreFeature> KNOWN_FEATURES = Collections15.hashMap();
  public static final StoreFeature ENCRYPTED = new StoreFeature("E");

  public static final StoreFeature[] PLAIN_STORE = {};
  public static final StoreFeature[] SECURE_STORE = {ENCRYPTED};
  public static final StoreFeature[] EMPTY_FEATURES_ARRAY = {};

  public static StoreFeature findByName(String name) {
    return KNOWN_FEATURES.get(name);
  }


  private final String myName;

  private StoreFeature(String name) {
    myName = name;
    KNOWN_FEATURES.put(name, this);
  }

  public String getName() {
    return myName;
  }

  public String toString() {
    return myName;
  }

  public int hashCode() {
    return myName.hashCode() * 23;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof StoreFeature))
      return false;
    StoreFeature that = (StoreFeature) obj;
    return myName.equals(that.myName);
  }
}
