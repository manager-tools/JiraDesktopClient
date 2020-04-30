package com.almworks.api.reduction;

import com.almworks.api.constraint.Constraint;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vasya
 */
public class ConstraintTreeLeaf extends ConstraintTreeElement {
  @NotNull
  private final Constraint myConstraint;

  public ConstraintTreeLeaf(@NotNull Constraint constraint) {
    myConstraint = constraint;
  }

  public ConstraintTreeLeaf getCopy() {
    ConstraintTreeLeaf result = new ConstraintTreeLeaf(myConstraint);
    if (isNegated())
      result.negate();
    return result;
  }

  @NotNull
  protected Constraint createConstraintImpl() {
    return myConstraint;
  }

  @NotNull
  public Constraint getConstraint() {
    return myConstraint;
  }

  public String toString() {
    return (isNegated() ? "!" : "") + myConstraint.toString();
  }

  public static ConstraintTreeLeaf createTrue() {
    return new ConstraintTreeLeaf(Constraint.NO_CONSTRAINT);
  }

  public static ConstraintTreeLeaf createFalse() {
    return createTrue(true);
  }

  public static ConstraintTreeLeaf createTrue(boolean negated) {
    ConstraintTreeLeaf aTrue = createTrue();
    if (negated) aTrue.negate();
    return aTrue;
  }

  /**
   * Creates replacement for this element with TRUE or FALSE depending on total negations from tree root.<br>
   * The replacement acts in a such way to make the whole constraint tree means TRUE if the value depend on this leaf value.<br>
   * Example: (assuming X is unknown constraint, others are known and can be calculated.<br>
   * 1. A | X - should be always TRUE, X is replaced with TRUE<br>
   * 2. A & X - should same as A, X replaced with TRUE<br>
   * 3. A | !(B & X) - should be always TRUE, X is replaced with FALSE to make (B & X) be false<br>
   * 4. A | !(B | X) - equal to A | (!B & !X) - should be TRUE if !B, X is replaced with FALSE<br>
   * Since De-Morgan keeps total number of negations X can be replaced with TRUE or FALSE depending on total number of negations.
   * @return if total negation is even (may be 0) returns TRUE, if total negation is odd (may be 1) returns FALSE
   */
  public ConstraintTreeLeaf createUnknowReplacement() {
    ConstraintTreeElement parent = this;
    boolean negatedFromRoot = parent.isNegated();
    while ((parent = parent.getParent()) != null) if (parent.isNegated()) negatedFromRoot = !negatedFromRoot;
    ConstraintTreeLeaf replacement = negatedFromRoot ? ConstraintTreeLeaf.createFalse() : ConstraintTreeLeaf.createTrue();
    if (isNegated()) replacement.negate();
    return replacement;
  }
}
