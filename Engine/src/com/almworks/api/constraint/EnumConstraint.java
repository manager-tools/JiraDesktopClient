package com.almworks.api.constraint;

import com.almworks.items.api.DBAttribute;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.util.List;

/**
 * @author dyoma
 */
public class EnumConstraint implements OneFieldConstraint {
  public static final TypedKey<EnumConstraint> ENUM_CONSTRAINT = TypedKey.create("enum");
  private final DBAttribute myAttribute;
  private final List<Long> myValues;

  public EnumConstraint(DBAttribute attribute, List<Long> values) {
    myAttribute = attribute;
    myValues = values.isEmpty() ? Collections15.<Long>emptyList() : Collections15.unmodifiableListCopy(values);
  }

  public TypedKey<EnumConstraint> getType() {
    return ENUM_CONSTRAINT;
  }

  public DBAttribute getAttribute() {
    return myAttribute;
  }

  public List<Long> getValues() {
    return myValues;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("enum ")
      .append(myAttribute.getId())
      .append(" in {");
    String sep = "";
    for (Long value : myValues) {
      builder.append(sep);
      builder.append(value);
      sep = ", ";
    }
    builder.append("}");
    return builder.toString();
  }
}
