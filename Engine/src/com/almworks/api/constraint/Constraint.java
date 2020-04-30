package com.almworks.api.constraint;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Some Constraint instances are valid, and some are not. See subinterfaces javadoc.
 *
 * @author dyoma
 */
public interface Constraint {
  TypedKey<Constraint> TRUE = TypedKey.create("true");

  Constraint NO_CONSTRAINT = new Constraint() {
    public TypedKey<? extends Constraint> getType() {
      return TRUE;
    }

    public String toString() {
      return "true";
    }
  };

  Constraint FALSE = new ConstraintNegation.Simple(NO_CONSTRAINT);

  @NotNull
  TypedKey<? extends Constraint> getType();
}
