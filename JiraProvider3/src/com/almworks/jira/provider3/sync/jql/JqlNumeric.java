package com.almworks.jira.provider3.sync.jql;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldIntConstraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JqlConvertorUtil;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JqlNumeric implements JQLConvertor {
  private final DBAttribute myAttribute;
  private final String myJQLName;
  private final String myDisplayName;

  public JqlNumeric(String jqlName, DBAttribute attribute, String displayName) {
    myAttribute = attribute;
    myJQLName = jqlName;
    myDisplayName = displayName;
  }

  private final Map<TypedKey<? extends FieldIntConstraint>, TypedKey<? extends FieldIntConstraint>> NEGATION;
  private final Map<TypedKey<? extends FieldIntConstraint>, String> OPERATIONS;
  {
    HashMap<TypedKey<? extends FieldIntConstraint>,TypedKey<? extends FieldIntConstraint>> map = Collections15.hashMap();
    map.put(FieldIntConstraint.INT_EQUALS, FieldIntConstraint.INT_NOT_EQUALS);
    map.put(FieldIntConstraint.INT_GREATER, FieldIntConstraint.INT_LESS_EQUAL);
    map.put(FieldIntConstraint.INT_GREATER_EQUAL, FieldIntConstraint.INT_LESS);
    HashMap<TypedKey<? extends FieldIntConstraint>,TypedKey<? extends FieldIntConstraint>> map2 = Collections15.hashMap();
    map2.putAll(map);
    for (Map.Entry<TypedKey<? extends FieldIntConstraint>, TypedKey<? extends FieldIntConstraint>> entry : map.entrySet())
      map2.put(entry.getValue(), entry.getKey());
    NEGATION = Collections.unmodifiableMap(map2);

    HashMap<TypedKey<? extends FieldIntConstraint>, String> operations = Collections15.hashMap();
    operations.put(FieldIntConstraint.INT_NOT_EQUALS, "!=");
    operations.put(FieldIntConstraint.INT_EQUALS, "=");
    operations.put(FieldIntConstraint.INT_GREATER, ">");
    operations.put(FieldIntConstraint.INT_GREATER_EQUAL, ">=");
    operations.put(FieldIntConstraint.INT_LESS, "<");
    operations.put(FieldIntConstraint.INT_LESS_EQUAL, "<=");
    OPERATIONS = operations;
  }

  @Override
  public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
    if (JqlConvertorUtil.isMyEmpty(constraint, myAttribute)) return JQLCompareConstraint.isEmpty(myJQLName, negated, myDisplayName);
    FieldIntConstraint fic = JqlConvertorUtil.cast(constraint, FieldIntConstraint.class, myAttribute);
    if (fic == null) return null;
    BigDecimal intValue = fic.getIntValue();
    if (intValue == null) return constraint;
    TypedKey<? extends FieldIntConstraint> type = fic.getType();
    if (negated) {
      TypedKey<? extends FieldIntConstraint> negation = NEGATION.get(type);
      if (negation == null) {
        LogHelper.error("Unknown type", type);
        return constraint;
      }
      type = negation;
    }
    String operation = OPERATIONS.get(type);
    if (operation == null) {
      LogHelper.error("Unknown type", type);
      return constraint;
    }
    return JQLCompareConstraint.quote(myJQLName, intValue.toPlainString(), operation, myDisplayName);
  }
}
