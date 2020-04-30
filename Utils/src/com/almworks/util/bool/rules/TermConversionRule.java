package com.almworks.util.bool.rules;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.BoolExprBuilder;
import com.almworks.util.bool.ConversionRule;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for conversion rules that apply only to terms, leaving operations and literals intact.
 *
 * @param <S> source predicate type
 * @param <T> target predicate type
 */
public abstract class TermConversionRule<S, T> extends ConversionRule<S, T> {
  private final boolean myIgnoreInconvertible;

  /**
   * @param ignoreInconvertible if true, inconvertible children of operations will be ignored
   */
  protected TermConversionRule(boolean ignoreInconvertible) {
    myIgnoreInconvertible = ignoreInconvertible;
  }

  public boolean convert(BoolExpr<S> expression, BoolExprBuilder<T> builder) {
    BoolExpr.Term<S> term = expression.asTerm();
    if (term != null) {
      S source = term.getTerm();
      T converted = convertTerm(source);
      if (converted == null)
        return false;
      builder.setTerm(converted, term.isNegated());
      return true;
    } else {
      return convertDefault(expression, builder, myIgnoreInconvertible);
    }
  }

  /**
   * Converts source predicate to target predicate, if possible
   *
   * @param source source predicate
   * @return target predicate, or null if conversion is not possible
   */
  @Nullable
  protected abstract T convertTerm(S source);
}
