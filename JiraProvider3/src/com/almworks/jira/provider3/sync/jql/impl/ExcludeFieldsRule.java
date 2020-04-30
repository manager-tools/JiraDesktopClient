package com.almworks.jira.provider3.sync.jql.impl;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.reduction.ConstraintTreeElement;
import com.almworks.api.reduction.ConstraintTreeLeaf;
import com.almworks.api.reduction.ReductionUtil;
import com.almworks.api.reduction.Rule;
import com.almworks.jira.provider3.sync.schema.ServerCustomField;
import com.almworks.restconnector.jql.JQLConstraint;
import com.almworks.util.Pair;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Removes all custom field constraint except allowed.
 */
class ExcludeFieldsRule implements Rule {
  private final Set<String> myAllowedCustomFields;
  private final Map<String, String> myExcluded = Collections15.hashMap();

  ExcludeFieldsRule(@Nullable Collection<String> allowedCustomFields) {
    myAllowedCustomFields = Collections15.hashSet(Util.NN(allowedCustomFields, Collections15.<String>emptySet()));
  }

  public List<Rule> createRules() {
    ArrayList<Rule> rules = Collections15.arrayList();
    rules.addAll(ReductionUtil.SIMPLIFY);
    rules.add(this);
    return rules;
  }

  @Override
  public ConstraintTreeElement process(ConstraintTreeElement element) {
    ConstraintTreeLeaf leaf = Util.castNullable(ConstraintTreeLeaf.class, element);
    if (leaf == null) return null;
    Constraint constraint = leaf.getConstraint();
    JQLConstraint jqlConstraint = Util.castNullable(JQLConstraint.class, constraint);
    if (jqlConstraint == null) return null;
//    JQLCompareConstraint compare = Constraints.cast(JQLCompareConstraint.COMPARE, jqlConstraint);
//    if (compare == null) {
//      LogHelper.error("Unknown constraint", jqlConstraint);
//      return null;
//    }
    String fieldId = jqlConstraint.getFieldId();
    if (!ServerCustomField.JQL_ID_PATTERN.matcher(fieldId).matches()) return null; // static fields are searchable
    if (!myAllowedCustomFields.contains(fieldId)) {
      if (!myExcluded.containsKey(fieldId)) myExcluded.put(fieldId, jqlConstraint.getDisplayName());
      return leaf.createUnknowReplacement();
    }
    return null;
  }

  public Collection<Pair<String, String>> getExcluded() {
    ArrayList<Pair<String, String>> result = Collections15.arrayList();
    for (Map.Entry<String, String> entry : myExcluded.entrySet()) result.add(Pair.create(entry.getKey(), entry.getValue()));
    return result;
  }
}
