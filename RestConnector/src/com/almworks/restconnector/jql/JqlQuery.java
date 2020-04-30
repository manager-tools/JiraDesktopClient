package com.almworks.restconnector.jql;

import com.almworks.api.constraint.CompositeConstraint;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.ConstraintNegation;
import com.almworks.api.constraint.Constraints;
import com.almworks.util.LogHelper;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class JqlQuery {
  public static final JqlQuery EMPTY = new JqlQuery(null);

  private final Constraint myJqlConstrain;
  @Nullable
  private final String myOrderBy;
  private String myText;

  public JqlQuery(Constraint jqlConstrain) {
    this(jqlConstrain, null);
  }

  public JqlQuery(Constraint jqlConstrain, @Nullable  String orderBy) {
    myJqlConstrain = jqlConstrain;
    myOrderBy = orderBy;
  }

  public JqlQuery orderBy(String orderBy) {
    if (Objects.equals(myOrderBy, orderBy)) return this;
    return new JqlQuery(myJqlConstrain, orderBy);
  }

  public Constraint getJqlConstrain() {
    return myJqlConstrain;
  }

  @NotNull
  public String getJqlText() {
    String jql = myText;
    if (jql == null) {
      jql = myJqlConstrain == null ? null : createJqlText(myJqlConstrain);
      if (jql == null) jql = "";
      if (myOrderBy != null) jql += " " + myOrderBy;
      myText = jql;
    }
    return jql;
  }

  private static String createJqlText(Constraint jqlConstraint) {
    Boolean constant = Constraints.checkSimpleConstant(jqlConstraint);
    if (constant != null) return constant ? "" : null;
    StringBuilder builder = new StringBuilder();
    createJqlText(jqlConstraint, builder);
    return builder.toString();
  }

  private static void createJqlText(Constraint constraint, StringBuilder builder) {
    ConstraintNegation negated = Constraints.cast(ConstraintNegation.NEGATION, constraint);
    CompositeConstraint composite = Util.castNullable(CompositeConstraint.class, negated != null ? negated.getNegated() : constraint);
    if (composite != null) {
      createComposite(composite, builder, negated != null);
      return;
    }
    if (negated != null) {
      LogHelper.error("Negation not supported", negated);
      return;
    }
    JQLConstraint jqlConstraint = Util.castNullable(JQLConstraint.class, constraint);
    if (jqlConstraint == null) {
      LogHelper.error("Unsupported constraint", constraint);
      return;
    }
    jqlConstraint.appendTo(builder);
  }

  private static void createComposite(CompositeConstraint composite, StringBuilder builder, boolean negated) {
    TypedKey<? extends CompositeConstraint> type = composite.getType();
    boolean and;
    if (type == CompositeConstraint.AND) and = true;
    else if (type == CompositeConstraint.OR) and = false;
    else {
      LogHelper.error("Unknown constraint", composite, type);
      return;
    }
    List<? extends Constraint> children = composite.getChildren();
    if (children == null || children.isEmpty()) return;
    if (negated) builder.append(" NOT ");
    if (children.size() == 1) {
      createJqlText(children.get(0), builder);
      return;
    }
    builder.append("(");
    String sep = "";
    for (Constraint child : children) {
      builder.append(sep);
      createJqlText(child, builder);
      sep = and ? " AND " : " OR ";
    }
    builder.append(")");
  }

  @Override
  public String toString() {
    return "JQL(" + getJqlText() + ")";
  }
}
