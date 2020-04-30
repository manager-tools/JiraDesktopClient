package com.almworks.util.bool;

import org.jetbrains.annotations.Nullable;

/**
 * ConversionRule is a class that is used to convert boolean expressions based on one predicate type
 * to boolean expressions based on another predicate type.
 *
 * @param <S> source predicate type
 * @param <T> target predicate type
 */
public abstract class ConversionRule<S, T> {
  /**
   * Converts S-based expression to T-based expression, using builder to create a new expression.
   * Implementation is free to transform the structure of the expression tree, converting terms to operations
   * and vice versa.
   *
   * @param expression source expression
   * @param builder target expression builder
   * @return false if conversion is not possible; true if it was successful
   */
  public abstract boolean convert(BoolExpr<S> expression, BoolExprBuilder<T> builder);

  /**
   * Converts S-based expression to T-based expression.
   * 
   * @param expression source expression
   * @return target expression or null if conversion is not possible
   */
  @Nullable
  public final BoolExpr<T> convert(@Nullable BoolExpr<S> expression) {
    if (expression == null)
      return null;
    BoolExprBuilder<T> builder = BoolExprBuilder.create();
    boolean success = convert(expression, builder);
    return success ? builder.build() : null;
  }

  /**
   * Utility method for implementing classes. It converts operations and literals to equal
   * constructs in T-based expression. Since terms cannot be converted automatically, you must not pass
   * a {@link BoolExpr.Term} to this method, or it will reject the conversion.
   *
   * @param expression source expression
   * @param builder target builder
   * @param ignoreInconvertible if true, ignore inconverible children when building T-based operations
   * @return conversion success
   */
  protected boolean convertDefault(BoolExpr<S> expression, BoolExprBuilder<T> builder,
    boolean ignoreInconvertible)
  {
    BoolExpr.Operation<S> operation = expression.asOperation();
    if (operation != null) {
      return convertOperation(operation, builder, ignoreInconvertible);
    } else if (expression instanceof BoolExpr.Literal) {
      return convertLiteral((BoolExpr.Literal<S>) expression, builder);
    } else {
      return false;
    }
  }

  /**
   * Utility method for converting literals.
   *
   * @param literal source literal
   * @param builder target builder, where the same literal will be set
   * @return conversion success
   */
  protected boolean convertLiteral(BoolExpr.Literal<S> literal, BoolExprBuilder<T> builder) {
    if (literal == BoolExpr.TRUE())
      builder.setExpression(BoolExpr.<T>TRUE());
    else if (literal == BoolExpr.FALSE())
      builder.setExpression(BoolExpr.<T>FALSE());
    else
      return false;
    return true;
  }

  /**
   * Utility method for converting boolean operations. Builds a similar operation in the target builder,
   * then recursively calls {@link #convert} for children.
   * <p>
   * If any child conversion returns false, the consequent behavior depends on ignoreInconvertible parameter.
   * If it is true, the child is ignored and conversion continues (you can possibly get zero-children node then).
   * If ignoreInconvertible is false, conversion stops and false is returned
   *
   * @param operation source boolean operation
   * @param builder target builder
   * @param ignoreInconvertible if true, inconvertible children are ignored
   * @return conversion success
   */
  protected boolean convertOperation(BoolExpr.Operation<S> operation, BoolExprBuilder<T> builder,
    boolean ignoreInconvertible)
  {
    builder.likeOperation(operation);
    for (BoolExpr<S> child : operation.getArguments()) {
      builder.add();
      if (convert(child, builder)) {
        builder.up();
      } else {
        builder.remove();
        if (!ignoreInconvertible) {
          return false;
        }
      }
    }
    return true;
  }
}
