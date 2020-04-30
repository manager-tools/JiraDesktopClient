package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;

import java.math.BigDecimal;

/**
 * @author dyoma
 */
public interface FieldIntConstraint extends OneFieldConstraint {
  TypedKey<FieldIntConstraint> INT_EQUALS = TypedKey.create("intEquals");
  TypedKey<FieldIntConstraint> INT_NOT_EQUALS = TypedKey.create("intEquals");
  TypedKey<FieldIntConstraint> INT_LESS = TypedKey.create("intLess");
  TypedKey<FieldIntConstraint> INT_GREATER = TypedKey.create("intGreater");
  TypedKey<FieldIntConstraint> INT_GREATER_EQUAL = TypedKey.create("intGreaterOrEqual");
  TypedKey<FieldIntConstraint> INT_LESS_EQUAL = TypedKey.create("intLessOrEqual");

  TypedKey<? extends FieldIntConstraint> getType();

  /**
   * @return valid returns not null
   */
  BigDecimal getIntValue();

  class Simple implements FieldIntConstraint {
    private final TypedKey<? extends FieldIntConstraint> myType;
    private final BigDecimal myValue;
    private final DBAttribute myAttribute;

    public Simple(TypedKey<? extends FieldIntConstraint> type, DBAttribute attribute, BigDecimal value) {
      assert type != null;
      myType = type;
      myValue = value;
      myAttribute = attribute;
    }

    public TypedKey<? extends FieldIntConstraint> getType() {
      return myType;
    }

    public BigDecimal getIntValue() {
      return myValue;
    }

    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public static Simple greater(DBAttribute attribute, BigDecimal lowerBound) {
      return new Simple(INT_GREATER, attribute, lowerBound);
    }

    public static Simple greaterOrEqual(DBAttribute attribute, BigDecimal bound) {
      return new Simple(INT_GREATER_EQUAL, attribute, bound);
    }

    public static Simple equals(DBAttribute attribute, BigDecimal value) {
      return new Simple(INT_EQUALS, attribute, value);
    }

    public static Simple less(DBAttribute attribute, BigDecimal upperBound) {
      return new Simple(INT_LESS, attribute, upperBound);
    }

    public static Simple lessOrEqual(DBAttribute attribute, BigDecimal bound) {
      return new Simple(INT_LESS_EQUAL, attribute, bound);
    }
  }
}
