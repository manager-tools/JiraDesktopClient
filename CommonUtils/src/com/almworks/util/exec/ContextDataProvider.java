package com.almworks.util.exec;

import org.almworks.util.TypedKey;

/**
 * Must be immutable and thread-safe!
 */
public abstract class ContextDataProvider {
  public static final ContextDataProvider EMPTY = EmptyContextDataProvider.INSTANCE;

  public abstract <T> T getObject(Class<T> objectClass, int depth) throws ContextDepthException;

  public abstract <T> T getObject(TypedKey<T> key, int depth) throws ContextDepthException;

  private static class EmptyContextDataProvider extends ContextDataProvider {
    public static final EmptyContextDataProvider INSTANCE = new EmptyContextDataProvider();

    private EmptyContextDataProvider() {
    }

    public <T> T getObject(Class<T> objectClass, int depth) {
      return null;
    }

    public <T> T getObject(TypedKey<T> key, int depth) {
      return null;
    }
  }
}
