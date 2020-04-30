package com.almworks.tracker.eapi.alpha;

import com.almworks.dup.util.TypedKey;

/**
 * User-level property key for identifying Deskzilla client.
 */
public class ConnectorProperty<T> extends TypedKey<T> {
  public static final ConnectorProperty<String> NAME = create("NAME");
  public static final ConnectorProperty<String> SHORT_NAME = create("SHORT_NAME");

  private ConnectorProperty(String name) {
    super(name);
  }

  private static <T> ConnectorProperty<T> create(String name) {
    return new ConnectorProperty<T>(name);
  }
}
