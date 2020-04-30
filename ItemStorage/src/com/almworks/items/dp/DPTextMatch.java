package com.almworks.items.dp;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DPTextMatch extends DPAttribute<String> {
  private final Pattern myPattern;
  private final boolean myWhole;

  public DPTextMatch(DBAttribute<String> attribute, Pattern pattern, boolean whole) {
    super(attribute);
    myPattern = pattern;
    myWhole = whole;
  }

  public static BoolExpr<DP> contains(DBAttribute<String> attribute, String text) {
    if (text.length() == 0)
      throw new IllegalArgumentException();
    return new DPTextMatch(attribute, Pattern.compile(text, Pattern.LITERAL | Pattern.CASE_INSENSITIVE), false).term();
  }

  @Override
  protected boolean acceptValue(String value, DBReader reader) {
    if (value == null)
      return false;
    Matcher matcher = myPattern.matcher(value);
    return myWhole ? matcher.matches() : matcher.find();
  }

  @Override
  public String toString() {
    return getAttribute() + " ~= " + myPattern;
  }

  @Override
  protected boolean equalDPA(DPAttribute other) {
    DPTextMatch that = (DPTextMatch) other;
    return myPattern.equals(that.myPattern) && myWhole == that.myWhole;
  }

  @Override
  protected int hashCodeDPA() {
    return myPattern.hashCode() * 39 + (myWhole ? 1 : 0);
  }
}