package com.almworks.api.reduction;

import com.almworks.api.constraint.CompositeConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author dyoma
 */
public class ReductionUtil {
  public static final List<Rule> CNF = normalForm(CompositeConstraint.AND);
  public static final List<Rule> DNF = normalForm(CompositeConstraint.OR);
  public static final List<Rule> SIMPLIFY =
    Arrays.asList(RemoveDummyNodes.INSTANCE, FlattenSameRule.FLATTEN_ANDS, FlattenSameRule.FLATTEN_ORS);

  public static boolean applyToChildren(@NotNull ConstraintTreeNode node, @NotNull Rule rule) {
    boolean wasChanged = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      ConstraintTreeElement child = node.getChildAt(i);
      ConstraintTreeElement result = rule.process(child);
      if (result != null) {
        node.replaceChild(child, result);
        wasChanged = true;
      }
    }
    return wasChanged;
  }

  @Nullable
  public static ConstraintTreeElement applyRecursive(@NotNull ConstraintTreeElement element, @NotNull Rule rule) {
    ConstraintTreeElement result = rule.process(element);
    if (result != null)
      return result;
    if (!(element instanceof ConstraintTreeNode))
      return null;
    ConstraintTreeNode node = (ConstraintTreeNode) element;
    boolean wasChanged = false;
    for (int i = 0; i < node.getChildCount(); i++) {
      ConstraintTreeElement child = node.getChildAt(i);
      ConstraintTreeElement newChild = applyRecursive(child, rule);
      if (newChild != null) {
        node.replaceChild(child, newChild);
        wasChanged = true;
      }
    }
    return wasChanged ? node : null;
  }

  /**
   * <h3>How to use</h3>
   * This method should be used to:
   * <ol>
   * <li>Avoid unacceptable types of constraints</li>
   * <li>Convert composite constraint to desired form</li>
   * </ol>
   * If the composite constraint may contains unacceptable ubconstraints than this method first should be invoked with
   * set of rules to replace them with acceptable ones.
   * If some combinations (for example negation of some constraints) of constraints isn't accesptable than rules replacing
   * then should be appended to the list of to-form-convertion rules.<p>
   * <h3>How it works</h3>
   * Applies given rules to the result of last application in given order. If rule performs any convertion this method
   * continues to apply rules starting with first rule. In other words it mean that the rule with index I will be applied
   * only if rules with lesser indecies are inapplicable.
   *
   * @param element tree root to reduce. This method may change the parameter
   * @param rules   rules to apply
   * @return reduced tree, or new tree built while reducing original
   *         todo check for infinite cycle (igor)
   */
  @NotNull
  public static ConstraintTreeElement reduce(@NotNull ConstraintTreeElement element,
    @NotNull List<? extends Rule> rules)
  {
    int i = 0;
    while (i < rules.size()) {
      Rule rule = rules.get(i);
      ConstraintTreeElement result = applyRecursive(element, rule);
      if (result == null) {
        i++;
      } else {
        element = result;
        i = 0;
      }
    }
    return element;
  }

  /**
   * @see #reduce(ConstraintTreeElement, java.util.List<? extends com.almworks.api.reduction.Rule>)
   */
  @NotNull
  public static ConstraintTreeElement reduce(@NotNull ConstraintTreeElement element, Rule ...  rules) {
    return reduce(element, Arrays.asList(rules));
  }

  @NotNull
  public static ConstraintTreeElement toCnf(@NotNull ConstraintTreeElement element) {
    return reduce(element, CNF);
  }

  private static List<Rule> normalForm(CompositeConstraint.ComplementaryKey type) {
    return Arrays.asList(new Rule[] {
      RemoveDummyNodes.INSTANCE,
      FlattenSameRule.FLATTEN_ANDS,
      FlattenSameRule.FLATTEN_ORS,
      new DeMorganRule(type),
      new DeMorganRule(type.getComplementary()),
      new DistributionRule(type.getComplementary())
    });
  }
}
