package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ConstraintNegation;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vasya
 */
public abstract class ConstraintTreeElement {
  @Nullable
  private ConstraintTreeNode myParent;
  private boolean myNegated = false;

  @NotNull
  public final Constraint createConstraint() {
    Constraint constraint = createConstraintImpl();
    return myNegated ? new ConstraintNegation.Simple(constraint) : constraint;
  }

  @NotNull
  protected abstract Constraint createConstraintImpl();

  public final boolean isNegated() {
    return myNegated;
  }

  public final void negate() {
    myNegated = !myNegated;
  }

  public final void setParent(@Nullable ConstraintTreeNode parent) {
    myParent = parent;
  }

  @Nullable
  public final ConstraintTreeNode getParent() {
    return myParent;
  }

  public abstract ConstraintTreeElement getCopy();

  public static ConstraintTreeElement createTree(@NotNull Constraint root) {
    boolean negate = false;
    while (root.getType() == ConstraintNegation.NEGATION) {
      negate = !negate;
      ConstraintNegation negation = ConstraintNegation.NEGATION.cast(root);
      assert negation != null;
      root = negation.getNegated();
    }
    //noinspection RawUseOfParameterizedType
    TypedKey type = root.getType();
    final ConstraintTreeElement result;
    if (type != CompositeConstraint.AND && type != CompositeConstraint.OR)
      result = new ConstraintTreeLeaf(root);
    else {
      ConstraintTreeNode node = new ConstraintTreeNode();
      node.setType(type);
      for (Constraint constraint : ((CompositeConstraint) root).getChildren())
        node.addChild(createTree(constraint));
      result = node;
    }
    if (negate)
      result.negate();
    return result;
  }
}
