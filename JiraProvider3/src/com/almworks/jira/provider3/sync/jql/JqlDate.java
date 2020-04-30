package com.almworks.jira.provider3.sync.jql;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.DateConstraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JqlConvertorUtil;
import com.almworks.util.LogHelper;
import org.almworks.util.TypedKey;

import java.util.Date;

public class JqlDate implements JQLConvertor {
  private final String myJqlName;
  private final DBAttribute<?> myAttribute;
  private final String myDisplayName;

  public JqlDate(String jqlName, DBAttribute<?> attribute, String displayName) {
    myJqlName = jqlName;
    myAttribute = attribute;
    myDisplayName = displayName;
  }

  @Override
  public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
    if (JqlConvertorUtil.isMyEmpty(constraint, myAttribute)) return JQLCompareConstraint.isEmpty(myJqlName, negated, myDisplayName);
    DateConstraint dateConstraint = JqlConvertorUtil.cast(constraint, DateConstraint.class, myAttribute);
    if (dateConstraint == null) return null;
    Date date = dateConstraint.getDate();
    if (date == null) return constraint;
    TypedKey<? extends DateConstraint> type = dateConstraint.getType();
    boolean after;
    if (type == DateConstraint.AFTER) after = true;
    else if (type == DateConstraint.BEFORE) after = false;
    else {
      LogHelper.error("Unknown date constraint", type, dateConstraint);
      return null;
    }
    context.addDateWarning(myJqlName, myDisplayName);
    if (negated) after = !after;
    return new JQLCompareConstraint.Single(myJqlName, String.valueOf(date.getTime()), after ? ">=" : "<=", myDisplayName);
  }
}
