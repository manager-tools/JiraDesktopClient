package com.almworks.jira.provider3.sync.jql.impl;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldSubstringsConstraint;
import com.almworks.jira.provider3.schema.Issue;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import org.almworks.util.Util;

import java.util.List;

class KeyJqlConvertor implements JQLConvertor {
  public static final JQLConvertor INSTANCE = new KeyJqlConvertor();

  @Override
  public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
    FieldSubstringsConstraint text = Util.castNullable(FieldSubstringsConstraint.class, constraint);
    if (text == null || !Util.equals(Issue.KEY, text.getAttribute())) return null;
    List<String> keys = text.getSubstrings();
    if (keys == null || keys.isEmpty()) return constraint;
    return JQLCompareConstraint.in("key", keys, false, "Issue Key");
  }
}
