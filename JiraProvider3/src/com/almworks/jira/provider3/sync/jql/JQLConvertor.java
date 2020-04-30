package com.almworks.jira.provider3.sync.jql;

import com.almworks.api.constraint.*;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JQLConstraint;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface JQLConvertor {
  /**
   *
   * @param constraint the constraint to convert to JQL
   * @param negated if negated constraint should be converted
   * @return <b>null</b> - this convertor can not convert the constraint, because of does not know the constraint kind<br>
   *   <b>argument constraint</b> - the convertor understands constraint kind but can not convert to JQL<br>
   *   <b>{@link JQLConstraint instance of jql constraint}</b> - the convertor can convert the argument, returns conversion.<br>
   *   <b>{@link com.almworks.api.constraint.Constraint#NO_CONSTRAINT TRUE} or {@link com.almworks.api.constraint.Constraint#FALSE FALSE}</b> - the convertor understands the constraint and sure that it is constant true or false.
   */
  @Nullable
  Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated);

  JQLConvertor CONNECTION = new JQLConvertor() {
    @Override
    public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
      Long connection = getConnection(constraint);
      if (connection == null) return null;
      return connection == context.getConnectionItem() ? Constraint.NO_CONSTRAINT : Constraint.FALSE;
    }

    private Long getConnection(Constraint constraint) {
      FieldEqualsConstraint equals = Constraints.cast(FieldEqualsConstraint.EQUALS_TO, constraint);
      if (equals == null || !SyncAttributes.CONNECTION.equals(equals.getAttribute())) return null;
      return Math.max(0, Util.NN(equals.getExpectedValue(), 0l));
    }
  };

  JQLConvertor TEXT = new JQLConvertor() {
    private final String myId = "text";
    private final String myDisplayName = "Text Search";
    @Override
    public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
      ContainsTextConstraint text = Constraints.cast(ContainsTextConstraint.CONTAINS_TEXT, constraint);
      if (text == null) return null;
      List<String> words = text.getWords();
      if (negated || words == null || words.isEmpty()) return constraint;
      if (words.size() == 1) return jqlText(words.get(0));
      context.addTextWarning(myId, myDisplayName);
      ArrayList<Constraint> ors = Collections15.arrayList();
      for (String word : words) ors.add(jqlText(word));
      return CompositeConstraint.Simple.or(ors);
    }

    private JQLCompareConstraint jqlText(String string) {
      return JQLCompareConstraint.textPrefix(myId, string, false, myDisplayName);
    }
  };
}
