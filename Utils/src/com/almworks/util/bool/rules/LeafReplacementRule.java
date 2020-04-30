package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ReductionRule;
import com.almworks.util.commons.Condition;
import org.jetbrains.annotations.NotNull;

public class LeafReplacementRule<P> extends ReductionRule<P> {
  private final Condition<P> myCondition;
  private final BoolExpr<P> myReplacement;

  public LeafReplacementRule(Condition<P> condition, BoolExpr<P> replacement) {
    myCondition = condition;
    myReplacement = replacement;
  }

  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    BoolExpr.Term<P> term = expression.asTerm();
    if (term == null)
      return false;
    P p = term.getTerm();
    if (!myCondition.isAccepted(p))
      return false;
    builder.setExpression(myReplacement).setNegated(term.isNegated());
    return true;
  }
}
