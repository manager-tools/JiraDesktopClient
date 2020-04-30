package com.almworks.restconnector.jql;

import com.almworks.api.constraint.Constraint;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public abstract class JQLCompareConstraint implements JQLConstraint {
  public static final TypedKey<JQLCompareConstraint> COMPARE = TypedKey.create("compare");

  private final String myFieldId;
  private final String myOperation;
  @Nullable private final String myDisplayName;

  public JQLCompareConstraint(String fieldName, String operation, @Nullable String displayName) {
    myFieldId = fieldName;
    myOperation = operation;
    myDisplayName = displayName;
  }

  protected abstract String getTextArgument();

  @NotNull
  @Override
  public TypedKey<? extends Constraint> getType() {
    return COMPARE;
  }

  @Override
  public void appendTo(StringBuilder builder) {
    builder.append(myFieldId).append(" ").append(myOperation).append(" ").append(getTextArgument());
  }

  @Override
  public String getFieldId() {
    return myFieldId;
  }

  @Override
  @Nullable
  public String getDisplayName() {
    return myDisplayName;
  }

  public static JQLCompareConstraint isEmpty(String jqlName, boolean negated, @Nullable String displayName) {
    return new JQLCompareConstraint.Single(jqlName, Single.EMPTY, negated ? "is not" : "is", displayName);
  }

  /**
   * Intended to use with enum and integers values which requires quotes. This method should not be used with text filters.
   * @see #textPrefix(String, String, boolean, String)
   */
  public static JQLCompareConstraint quote(String field, String argument, String operation, @Nullable String displayName) {
    argument = argument.replaceAll("\"", "\\\"");
    return new JQLCompareConstraint.Single(field, quote(argument), operation, displayName);
  }

  public static JQLCompareConstraint equal(String field, int argument) {
    return equal(field, String.valueOf(argument));
  }

  public static JQLCompareConstraint equal(String field, String argument) {
    String quoted = JqlConvertorUtil.maybeQuote(argument);
    return new JQLCompareConstraint.Single(field, quoted, "=", String.format("'%s = %s'", field, argument));
  }

  /**
   * Create "prefix" filter for text search (JQL now does not allow to search for substring)<br>
   * JQL text docs: https://confluence.atlassian.com/display/JIRA/Performing+Text+Searches
   * JQL search substring feature: https://jira.atlassian.com/browse/JRA-6218
   * JQL special chars bug: https://jira.atlassian.com/browse/JRA-25092
   */
  public static JQLCompareConstraint textPrefix(String id, String text, boolean negated, String displayName) {
    text = text.replaceAll("[!(){}\\[\\]^?\\\\:]", "?"); // See https://jira.atlassian.com/browse/JRA-25092
    text = text.replaceAll("\"", "\\\\\\\\\\\\\""); // Replace " with \\\"
    text = text.replaceAll("\\+", "\\\\\\\\+");
    text = text.replaceAll("-", "\\\\\\\\-");
    text = text.replaceAll("\\|", "\\\\\\\\|");
    text = text.replaceAll("~", "\\\\\\\\~");
    text = text.replaceAll("\\*", "\\\\\\\\*");
    return new JQLCompareConstraint.Single(id, "\"" + text + "*\"", negated ? "~!" : "~", displayName);
  }

  /**
   * Search enums
   * @see #quote(String, String, String, String)
   */
  public static JQLCompareConstraint in(String field, Collection<String> argument, boolean negated, @Nullable String displayName) {
    return new JQLCompareConstraint.Multi(field, negated ? "not in" : "in", displayName, new ArrayList<>(argument));
  }

  public static String separateQuoted(Iterable<String> values, boolean surroundWithParenthesis) {
    StringBuilder result = new StringBuilder();
    if (surroundWithParenthesis) result.append("(");
    String sep = "";
    for (String v : values) {
      result.append(sep);
      sep = ",";
      result.append(quote(v));
    }
    if (surroundWithParenthesis) result.append(")");
    return result.toString();
  }

  private static String quote(String argument) {
    return "\"" + argument + "\"";
  }

  public static class Single extends JQLCompareConstraint {
    private static final String EMPTY = "EMPTY";

    private final String myArgument;

    public Single(String fieldName, String argument, String operation, @Nullable String displayName) {
      super(fieldName, operation, displayName);
      myArgument = argument;
    }

    @Override
    public String getTextArgument() {
      return myArgument;
    }

    public boolean isEmpty() {
      return EMPTY.equals(myArgument);
    }
  }

  public static class Multi extends JQLCompareConstraint {
    private final List<String> myArguments;

    public Multi(String fieldName, String operation, @Nullable String displayName, List<String> arguments) {
      super(fieldName, operation, displayName);
      myArguments = Collections15.unmodifiableListCopy(arguments);
    }

    public List<String> getArguments() {
      return myArguments;
    }

    @Override
    protected String getTextArgument() {
      StringBuilder list = new StringBuilder("(");
      TextUtil.separate(myArguments.iterator(), ",", Function.identity(), list);
      list.append(")");
      return list.toString();
    }
  }
}
