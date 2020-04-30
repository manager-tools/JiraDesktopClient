package com.almworks.jira.provider3.sync.jql.impl;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.Constraints;
import com.almworks.api.reduction.ConstraintTreeElement;
import com.almworks.api.reduction.ConstraintTreeLeaf;
import com.almworks.api.reduction.Rule;
import com.almworks.jira.provider3.sync.jql.JQLConvertor;
import com.almworks.restconnector.jql.JQLConstraint;
import com.almworks.util.LogHelper;
import org.almworks.util.Util;

import java.util.Collection;

class JQLRule implements Rule {
  private final Collection<JQLConvertor> myConvertors;
  private final JqlQueryBuilder myContext;

  JQLRule(Collection<JQLConvertor> convertors, JqlQueryBuilder context) {
    myConvertors = convertors;
    myContext = context;
  }

  @Override
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeLeaf leaf = Util.castNullable(ConstraintTreeLeaf.class, element);
    if (leaf == null) return null;
    Constraint constraint = leaf.getConstraint();
    if (constraint instanceof JQLConstraint || Constraints.checkSimpleConstant(constraint) != null) return null;
    boolean negated = leaf.isNegated();
    Constraint candidate = null;
    for (JQLConvertor convertor : myConvertors) {
      Constraint replacement = convertor.convert(myContext, constraint, negated);
      if (!(replacement instanceof JQLConstraint) && Constraints.checkSimpleConstant(constraint) != null) {
        LogHelper.error("Wrong replacement", convertor, constraint, replacement);
        continue;
      }
      if (replacement == null) continue;
      if (replacement == constraint) {
        if (candidate == null) candidate = constraint;
      }
      else if (candidate == null || candidate == constraint) candidate = replacement;
      else { // Two not trivial candidates
        Boolean replaceConst = Constraints.checkSimpleConstant(replacement);
        Boolean candidateConst = Constraints.checkSimpleConstant(candidate);
        if (replaceConst != null || candidateConst != null) {
          if (replaceConst != null && candidateConst != null) LogHelper.assertError(replaceConst.equals(candidateConst), constraint, "Incompatible conversions");
          else candidate = replaceConst != null ? replacement : candidate;
        } else LogHelper.error("Two distinct conversions", constraint, candidate, replacement, convertor);
      }
    }
    if (candidate == null || candidate == constraint) return leaf.createUnknowReplacement();
    return ConstraintTreeElement.createTree(candidate);
  }
}
