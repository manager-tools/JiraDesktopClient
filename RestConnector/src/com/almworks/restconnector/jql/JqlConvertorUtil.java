package com.almworks.restconnector.jql;

import com.almworks.api.constraint.Constraint;
import com.almworks.api.constraint.Constraints;
import com.almworks.api.constraint.IsEmptyConstraint;
import com.almworks.api.constraint.OneFieldConstraint;
import com.almworks.items.api.DBAttribute;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

public class JqlConvertorUtil {
  public static boolean isMyEmpty(Constraint constraint, DBAttribute<?> attribute) {
    return cast(constraint, IsEmptyConstraint.IS_EMPTY, attribute) != null;
  }

  public static <T extends OneFieldConstraint> T cast(Constraint constraint, Class<T> constraintClass, DBAttribute<?> attribute) {
    T casted = Util.castNullable(constraintClass, constraint);
    return casted != null && Util.equals(casted.getAttribute(), attribute) ? casted : null;
  }

  public static <T extends OneFieldConstraint> T cast(Constraint constraint, TypedKey<T> constraintKey, DBAttribute<?> attribute) {
    T casted = Constraints.cast(constraintKey, constraint);
    return casted != null && Util.equals(casted.getAttribute(), attribute) ? casted : null;
  }

  @Nullable
  public static String queryKeys(Collection<String> keys) {
    if (keys == null || keys.isEmpty()) return null;
    StringBuilder jql = new StringBuilder();
    for (String key : keys) {
      if (jql.length() > 0) jql.append(" OR ");
      jql.append("key=").append(key);
    }
    return jql.toString();
  }

  private static final Pattern QUOTES = Pattern.compile("\"");
  private static final Pattern NEEDS_QUOTES = Pattern.compile("[\\s+.,;?|*/%^$#@\\[\\]\"]");
  public static String maybeQuote(String strId) {
    return NEEDS_QUOTES.matcher(strId).find() ? "\"" + QUOTES.matcher(strId).replaceAll("\\\\\\\\\\\"") + "\"" : strId;
  }
}
