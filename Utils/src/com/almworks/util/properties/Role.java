package com.almworks.util.properties;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.Nullable;

public class Role <T> extends TypedKey<T> {
  private static int ourAnonymousCounter;

  protected Role(String name, @Nullable Class<T> roleInterface) {
    super(name, roleInterface, null);
  }

  public static <T> Role<T> anonymous() {
    int c;
    synchronized (Role.class) {
      c = ++ourAnonymousCounter;
    }
    return role("anonymous#" + c);
  }

  public static <T> Role<T> role(String name) {
    return new Role<T>(name, null);
  }

  public static <T> Role<T> role(Class<T> actorInterface) {
    return new Role<T>(actorInterface.getName(), actorInterface);
  }

  public static <T> Role<T> role(String name, Class<T> actorInterface) {
    assert actorInterface != null;
    return new Role<T>(name, actorInterface);
  }
}
