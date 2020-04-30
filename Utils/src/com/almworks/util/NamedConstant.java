package com.almworks.util;

/**
 * :todoc:
 *
 * @author sereda
 */
public abstract class NamedConstant <T> {
  protected final String myName;

  protected NamedConstant(String name) {
    assert name != null;
    myName = name;
  }

  protected NamedConstant(String name, NamedConstantRegistry registry) {
    this(name);
    registry.register(this);
  }

  public final String name() {
    return myName;
  }

  public abstract T value();
}
