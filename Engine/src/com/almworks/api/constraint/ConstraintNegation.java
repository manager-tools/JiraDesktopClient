package com.almworks.api.constraint;

import org.almworks.util.TypedKey;

/**
 * @author dyoma
 */
public interface ConstraintNegation extends Constraint {
  TypedKey<ConstraintNegation> NEGATION = TypedKey.create("negation");

  TypedKey<? extends ConstraintNegation> getType();

  Constraint getNegated();

  class Simple implements ConstraintNegation {
    private final Constraint myNegated;

    public Simple(Constraint negated) {
      myNegated = negated;
    }

    public TypedKey<? extends ConstraintNegation> getType() {
      return NEGATION;
    }

    public Constraint getNegated() {
      return myNegated;
    }
  }
}
