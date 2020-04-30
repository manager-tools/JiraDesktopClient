package com.almworks.api.reduction;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.Constraints;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dyoma
 */
public abstract class SpecificLeafRule<T extends Constraint> implements Rule {
  private final TypedKey<T> myType;

  protected SpecificLeafRule(TypedKey<T> type) {
    myType = type;
  }

  @Nullable
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeLeaf leaf = Util.castNullable(ConstraintTreeLeaf.class, element);
    if (leaf == null) return null;
    T constraint = Constraints.cast(myType, leaf.getConstraint());
    if (constraint == null)
      return null;
    return process(constraint, leaf.isNegated());
  }

  @Nullable
  protected abstract ConstraintTreeElement process(@NotNull T constraint, boolean negated);
}
