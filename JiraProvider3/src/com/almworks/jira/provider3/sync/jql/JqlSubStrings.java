package com.almworks.jira.provider3.sync.jql;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.FieldSubstringsConstraint;
import com.almworks.items.api.DBAttribute;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JqlConvertorUtil;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.List;

public class JqlSubStrings implements JQLConvertor {
  private final DBAttribute myAttribute;
  private final String myJQLName;
  private final String myDisplayName;

  public JqlSubStrings(String JQLName, DBAttribute<String> attribute, String displayName) {
    myAttribute = attribute;
    myJQLName = JQLName;
    myDisplayName = displayName;
  }

  @Override
  public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
    if (JqlConvertorUtil.isMyEmpty(constraint, myAttribute)) return JQLCompareConstraint.isEmpty(myJQLName, negated, myDisplayName);
    FieldSubstringsConstraint text = JqlConvertorUtil.cast(constraint, FieldSubstringsConstraint.class, myAttribute);
    if (text == null) return null;
    List<String> strings = text.getSubstrings();
    if (strings == null || strings.isEmpty()) return constraint;
    if (negated) return constraint; // JIRA (atleast 5.0) does not support negated text search (!~ operation)
    context.addTextWarning(myJQLName, myDisplayName);
    ArrayList<Constraint> ors = Collections15.arrayList();
    for (String string : strings) ors.add(JQLCompareConstraint.textPrefix(myJQLName, string, negated, myDisplayName));
    return CompositeConstraint.Simple.or(ors);
  }
}
