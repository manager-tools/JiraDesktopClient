package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;

/**
 * @author dyoma
 */
public interface FieldEqualsConstraint extends OneFieldConstraint {
  TypedKey<FieldEqualsConstraint> EQUALS_TO = TypedKey.create("equalsTo");

  TypedKey<? extends FieldEqualsConstraint> getType();

  /**
   * @return valid constraint returns not null
   */
  Long getExpectedValue();

  class Simple implements FieldEqualsConstraint {
    private final DBAttribute myAttribute;
    private final Long myValue;

    public Simple(DBAttribute attribute, Long value) {
      myAttribute = attribute;
      myValue = value;
    }

    public TypedKey<? extends FieldEqualsConstraint> getType() {
      return EQUALS_TO;
    }

    public Long getExpectedValue() {
      return myValue;
    }

    public DBAttribute getAttribute() {
      return myAttribute;
    }

    public boolean equals(Object obj) {
      if (!(obj instanceof Simple))
        return false;
      Simple other = (Simple) obj;
      return Util.equals(myAttribute, other.myAttribute) && Util.equals(myValue, other.myValue);
    }

    public int hashCode() {
      return myAttribute.hashCode() ^ myValue.hashCode();
    }

    public String toString() {
      return myAttribute.getId() + "=" + myValue;
    }

    public static Simple create(DBAttribute attribute, Long value) {
      return new Simple(attribute, value);
    }
  }
}
