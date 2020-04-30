package com.almworks.util.bool;

import com.almworks.util.bool.rules.*;
import com.almworks.util.commons.Condition;

import java.util.Collections;
import java.util.List;

/**
 * This class defines common reductions and provides convenience methods for calling them.
 */
@SuppressWarnings({
  "UtilityClass", "StaticMethodNamingConvention", "MethodNamesDifferingOnlyByCase",
  "UnnecessarilyQualifiedStaticUsage"})
public class Reductions {
  private static final ReductionRule SIMPLIFY =
    RuleChainRule.create(RemoveEmptyOperationsRule.INSTANCE(), SimplifyTrivialOperationsRule.instance(),
      FlattenRule.instance(), AbsorptionRule.instance());
  private static final ReductionRule CNF = normalForm(BoolOperation.AND);
  private static final ReductionRule DNF = normalForm(BoolOperation.OR);

  /**
   * Returns reduction to Conjunctive Normal Form
   */
  public static <P> ReductionRule<P> CNF() {
    return CNF;
  }

  /**
   * Returns reduction to Disjunctive Normal Form
   */
  public static <P> ReductionRule<P> DNF() {
    return DNF;
  }

  /**
   * Returns reduction that simplifies the expression
   */
  public static <P> ReductionRule<P> SIMPLIFY() {
    return SIMPLIFY;
  }

  /**
   * Applies DNF to expression
   */
  public static <P> BoolExpr<P> toDnf(BoolExpr<P> expression) {
    return Reductions.<P>DNF().reduce(expression);
  }

  /**
   * Applies CNF to expression
   */
  public static <P> BoolExpr<P> toCnf(BoolExpr<P> expression) {
    return Reductions.<P>CNF().reduce(expression);
  }

  /**
   * Simplifies the expression
   */
  public static <P> BoolExpr<P> simplify(BoolExpr<P> expression) {
    return Reductions.<P>SIMPLIFY().reduce(expression);
  }

  public static <P> List<BoolExpr<P>> toOperandList(BoolExpr<P> expression, BoolOperation operation) {
    BoolExpr.Operation<P> op = expression.asOperation(operation);
    if (op != null) {
      return op.getArguments();
    } else {
      return Collections.singletonList(expression);
    }
  }

  /**
   * Iterates through expression composite and replaces term predicates that satisfy a condition.
   * <p>
   * NB: If term is negated, the replacement will be negated as well.
   *
   * @param expression boolean expression
   * @param condition condition that should return true for terms that are to be replaced
   * @param replacement replacement expression to be inserted instead of source term
   * @param <P> predicate type
   * @return resulting expression
   */
  public static <P> BoolExpr<P> replaceTerms(BoolExpr<P> expression, Condition<P> condition, BoolExpr<P> replacement) {
    return new LeafReplacementRule<P>(condition, replacement).reduce(expression);
  }

  /**
   * Goes through expression composite to find a term that satisfies the condition. As soon as one satsfying term is
   * found, returns true.
   *
   * @param expression boolean expression
   * @param condition a condition on predicates
   * @return true if expression contains at least one term that satisifies the condition 
   */
  public static <P> boolean searchTermsForTruth(BoolExpr<P> expression, Condition<P> condition) {
    P term = expression.getTerm();
    if (term != null)
      return condition.isAccepted(term);
    BoolExpr.Operation<P> op = expression.asOperation();
    if (op == null)
      return false;
    for (BoolExpr<P> arg : op.getArguments()) {
      if (searchTermsForTruth(arg, condition))
        return true;
    }
    return false;
  }


  private Reductions() {
  }

  private static <P> RuleChainRule<P> normalForm(BoolOperation operation) {
    return RuleChainRule.create(Reductions.<P>SIMPLIFY(), DeMorganRule.<P>instance(),
      DistributivityRule.<P>instance(operation.getComplementary()));
  }
}
