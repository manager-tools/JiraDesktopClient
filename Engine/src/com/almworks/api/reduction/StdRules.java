package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.FieldIntConstraint;
import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

/**
 * @author dyoma
 */
public class StdRules {
  public static final Rule NEGATED_GREATER = new SpecificLeafRule<FieldIntConstraint>(FieldIntConstraint.INT_GREATER) {
    protected ConstraintTreeElement process(@NotNull FieldIntConstraint constraint, boolean negated) {
      if (!negated)
        return null;
      return equalsOr(FieldIntConstraint.INT_LESS, constraint);
    }
  };

  public static final Rule NEGATED_LESS = new SpecificLeafRule<FieldIntConstraint>(FieldIntConstraint.INT_LESS) {
    protected ConstraintTreeElement process(@NotNull FieldIntConstraint constraint, boolean negated) {
      if (!negated)
        return null;
      return equalsOr(FieldIntConstraint.INT_GREATER, constraint);
    }
  };

  public static final Rule REMOVE_GREATER_EQUAL = new ReplaceWithSymmetric(FieldIntConstraint.INT_GREATER_EQUAL, FieldIntConstraint.INT_LESS);
  public static final Rule REMOVE_LESS_EQUAL = new ReplaceWithSymmetric(FieldIntConstraint.INT_LESS_EQUAL, FieldIntConstraint.INT_GREATER);
  public static final Rule REMOVE_LESS = new ReplaceWithSymmetric(FieldIntConstraint.INT_LESS, FieldIntConstraint.INT_GREATER_EQUAL);
  public static final Rule REMOVE_GREATER = new ReplaceWithSymmetric(FieldIntConstraint.INT_GREATER, FieldIntConstraint.INT_LESS_EQUAL);
  
  @NotNull
  private static ConstraintTreeElement equalsOr(TypedKey<FieldIntConstraint> type, @NotNull FieldIntConstraint intConstraint) {
    ConstraintTreeNode node = new ConstraintTreeNode();
    node.setType(CompositeConstraint.OR);
    DBAttribute attribute = intConstraint.getAttribute();
    BigDecimal value = intConstraint.getIntValue();
    node.addChild(new ConstraintTreeLeaf(FieldIntConstraint.Simple.equals(attribute, value)));
    node.addChild(new ConstraintTreeLeaf(new FieldIntConstraint.Simple(type, attribute, value)));
    return node;
  }

  private static class ReplaceWithSymmetric extends SpecificLeafRule<FieldIntConstraint> {
    private final TypedKey<FieldIntConstraint> myReplacement;

    public ReplaceWithSymmetric(TypedKey<FieldIntConstraint> original, TypedKey<FieldIntConstraint> replacement) {
      super(original);
      myReplacement = replacement;
    }

    protected ConstraintTreeElement process(@NotNull FieldIntConstraint constraint, boolean negated) {
      ConstraintTreeLeaf symmetric = new ConstraintTreeLeaf(new FieldIntConstraint.Simple(
        myReplacement, constraint.getAttribute(), constraint.getIntValue()));
      if (!negated)
        symmetric.negate();
      return symmetric;
    }
  }
}
