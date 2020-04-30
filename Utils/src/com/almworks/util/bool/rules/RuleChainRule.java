package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ReductionRule;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Composite rule that tries to apply a sequence of rules one by one, until one of the rules applies.
 * <p>
 * Recursive behaviour of this rule is altered: see {@link #applyRecursive}.
 */
public class RuleChainRule<P> extends ReductionRule<P> {
  private final Collection<ReductionRule<P>> myRules;

  private RuleChainRule(Collection<ReductionRule<P>> rules) {
    myRules = rules;
  }

  /**
   * Constructs an instance of RuleChainRule
   *
   * @param rules     collection of rules, order is significant
   * @param reuseList if false, the list of rules should be copied; otherwise it can be used in the instance
   */
  public static <P> RuleChainRule<P> create(Collection<? extends ReductionRule<P>> rules, boolean reuseList) {
    Collection<ReductionRule<P>> c =
      reuseList ? Collections.unmodifiableCollection(rules) : Collections15.unmodifiableListCopy(rules);
    return new RuleChainRule<P>(c);
  }

  public static <P> RuleChainRule<P> create(ReductionRule<P>... rules) {
    return create(Arrays.asList(rules), true);
  }

  /**
   * Applies rules one by one, until one of them succeeds
   */
  public boolean apply(@NotNull BoolExpr<P> expression, @NotNull BoolExprBuilder<P> builder) {
    boolean applied = false;
    for (ReductionRule<P> rule : myRules) {
      applied = rule.apply(expression, builder);
      if (applied)
        break;
    }
    return applied;
  }

  /**
   * Applies chain of rules recursively. Unlike standard rules, this does not mean calling apply() on
   * the root, then on all the children: that would make all the rules try root first, then all rules
   * try children.
   * <p>
   * Instead, the call is iteratively delegated to chained rules, as in {@link #apply} method.
   *
   * @param source source expression
   * @param builder target builder
   */
  @SuppressWarnings({"RefusedBequest"})
  @Override
  public boolean applyRecursive(@NotNull BoolExpr<P> source, @NotNull BoolExprBuilder<P> builder) {
    boolean applied = false;
    for (ReductionRule<P> rule : myRules) {
      applied = rule.applyRecursive(source, builder);
      if (applied)
        break;
    }
    return applied;
  }
}
