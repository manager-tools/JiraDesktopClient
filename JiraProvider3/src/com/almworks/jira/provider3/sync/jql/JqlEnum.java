package com.almworks.jira.provider3.sync.jql;

import com.almworks.api.constraint.*;
import com.almworks.items.api.DBAttribute;
import com.almworks.jira.provider3.sync.jql.impl.JqlQueryBuilder;
import com.almworks.restconnector.jql.JQLCompareConstraint;
import com.almworks.restconnector.jql.JqlConvertorUtil;
import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public abstract class JqlEnum implements JQLConvertor {
  private final DBAttribute<?> myAttribute;
  private final String myJqlName;
  private final String myDisplayName;
  @Nullable private final String myEmptyId;

  public JqlEnum(String jqlName, DBAttribute<?> attribute, @Nullable String emptyId, String displayName) {
    myJqlName = jqlName;
    myAttribute = attribute;
    myEmptyId = emptyId;
    myDisplayName = displayName;
  }

  public static JqlEnum generic(String jqlName, DBAttribute<?> attribute, DBAttribute<?> idAttribute, String displayName) {
    return generic(jqlName, attribute, idAttribute, null, displayName);
  }

  public static JqlEnum generic(String jqlName, DBAttribute<?> attribute, DBAttribute<?> idAttribute, @Nullable String emptyId, String displayName) {
    return new Generic(jqlName, attribute, idAttribute, emptyId, displayName);
  }

  @Override
  public Constraint convert(JqlQueryBuilder context, Constraint constraint, boolean negated) {
    OneFieldConstraint oneField = Util.castNullable(OneFieldConstraint.class, constraint);
    if (oneField == null || !Util.equals(myAttribute, oneField.getAttribute())) return null;
    FieldSubsetConstraint intersection = Constraints.cast(FieldSubsetConstraint.INTERSECTION, constraint);
    if (intersection != null) return convertIntersection(context, intersection, negated);
    FieldEqualsConstraint equalTo = Constraints.cast(FieldEqualsConstraint.EQUALS_TO, constraint);
    if (equalTo != null) return convertEqualTo(context, equalTo, negated);
    LogHelper.error("Unknown constraint", constraint);
    return constraint;
  }

  private Constraint convertEqualTo(JqlQueryBuilder context, FieldEqualsConstraint equalTo, boolean negated) {
    Long enumItem = equalTo.getExpectedValue();
    return createJqlConstraint(context, equalTo, negated, Collections.singleton(enumItem));
  }

  private Constraint convertIntersection(JqlQueryBuilder context, FieldSubsetConstraint constraint, boolean negated) {
    return createJqlConstraint(context, constraint, negated, constraint.getSubset());
  }

  private Constraint createJqlConstraint(JqlQueryBuilder context, Constraint original, boolean negated, Collection<Long> enumItems) {
    JQLCompareConstraint isEmpty;
    if (enumItems.contains(0l)) {
      isEmpty = JQLCompareConstraint.isEmpty(myJqlName, negated, myDisplayName);
      enumItems = Collections15.arrayList(enumItems);
      enumItems.remove(0l);
    } else
      isEmpty = null;
    HashSet<String> enumIds = loadArguments(context, enumItems);
    if (enumIds.isEmpty()) return isEmpty != null ? isEmpty : original;
    if (isEmpty == null && myEmptyId != null && enumIds.remove(myEmptyId)) isEmpty = JQLCompareConstraint.isEmpty(myJqlName, negated, myDisplayName);
    JQLCompareConstraint searchIds;
    if (enumIds.isEmpty()) searchIds = null;
    else searchIds = JQLCompareConstraint.in(myJqlName, enumIds, negated, myDisplayName);
    if (searchIds == null && isEmpty == null) return original;
    if (searchIds != null && isEmpty != null) return CompositeConstraint.Simple.or(searchIds, isEmpty);
    return searchIds != null ? searchIds : isEmpty;
  }

  protected abstract HashSet<String> loadArguments(JqlQueryBuilder context, Collection<Long> enumItems);

  private static class Generic extends JqlEnum {
    private final DBAttribute<?> myIdAttribute;

    private Generic(String jqlName, DBAttribute<?> attribute, DBAttribute<?> idAttribute, @Nullable String emptyId, String displayName) {
      super(jqlName, attribute, emptyId, displayName);
      myIdAttribute = idAttribute;
    }

    protected HashSet<String> loadArguments(JqlQueryBuilder context, Collection<Long> enumItems) {
      List<?> rawIds = myIdAttribute.collectValues(enumItems, context.getReader());
      HashSet<String> result = Collections15.hashSet();
      for (Object id : rawIds) {
        if (id == null) continue;
        result.add(JqlConvertorUtil.maybeQuote(id.toString()));
      }
      return result;
    }
  }
}
