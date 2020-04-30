package com.almworks.util.collections;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class TypedReadAccessor <V, T> implements ReadAccessor<V, T> {
  public static final Convertor<TypedReadAccessor, Class> GET_TYPE_CLASS = new Convertor<TypedReadAccessor, Class>() {
    public Class convert(TypedReadAccessor t) {
      return t.getTypeClass();
    }
  };
  private final Class<T> myTypeClass;

  public TypedReadAccessor(Class<T> typeClass) {
    if (typeClass == null)
      throw new IllegalArgumentException("typeClass == null");
    myTypeClass = typeClass;
  }

  /**
   * Returns type T in runtime.
   */
  public Class<T> getTypeClass() {
    return myTypeClass;
  }
}
