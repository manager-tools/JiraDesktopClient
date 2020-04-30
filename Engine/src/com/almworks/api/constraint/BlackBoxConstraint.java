package com.almworks.api.constraint;

import org.almworks.util.TypedKey;

/**
 * A marker opaque constraint, meaning "something we can't understand".
 */
public enum BlackBoxConstraint implements Constraint {
  INSTANCE;

  public static final TypedKey<BlackBoxConstraint> BLACK_BOX = TypedKey.create("blackBox");

  @Override
  public TypedKey<? extends BlackBoxConstraint> getType() {
    return BLACK_BOX;
  }
}
