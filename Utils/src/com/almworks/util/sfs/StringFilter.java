package com.almworks.util.sfs;

import com.almworks.util.collections.Convertor;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringFilter {
  public static final Convertor<StringFilter, String> TO_STRING = new Convertor<StringFilter, String>() {
    public String convert(StringFilter value) {
      return value.writeToString();
    }
  };
  public static final Convertor<String, StringFilter> FROM_STRING = new Convertor<String, StringFilter>() {
    public StringFilter convert(String value) {
      return readFromString(value);
    }
  };

  private static final String TYPE_SETTING = "type";
  private static final String FILTER_SETTING = "filter";
  private static final String IGNORE_CASE_SETTING = "ignoreCase";

  private final MatchType myMatchType;
  private final String myFilterString;
  private final boolean myIgnoreCase;

  @Nullable
  private final Pattern myPattern;
  private static final String ENCODE_CASE_SENSITIVE = "case-sensitive ";

  public StringFilter(MatchType matchType, String filterString, boolean ignoreCase) {
    Pattern pattern;
    MatchType type;
    try {
      pattern = buildPattern(matchType, filterString, ignoreCase);
      type = matchType;
    } catch (PatternSyntaxException e) {
      Log.warn("bad pattern: " + filterString);
      pattern = null;
      type = MatchType.INVALID;
    }
    myPattern = pattern;
    myMatchType = type;
    myFilterString = filterString;
    myIgnoreCase = ignoreCase;
  }


  public MatchType getMatchType() {
    return myMatchType;
  }

  public String getFilterString() {
    return myFilterString;
  }

  public boolean isIgnoreCase() {
    return myIgnoreCase;
  }

  private Pattern buildPattern(MatchType matchType, String filterString, boolean ignoreCase)
    throws PatternSyntaxException
  {
    String regexp;
    filterString = filterString.trim();
    switch (matchType) {
    case CONTAINS:
      regexp = ".*" + escapeSimple(filterString) + ".*";
      break;
    case ENDS_WITH:
      regexp = ".*" + escapeSimple(filterString) + "$";
      break;
    case STARTS_WITH:
      regexp = "^" + escapeSimple(filterString) + ".*";
      break;
    case REGEXP:
      // add .* on both sides of regexp unless there is border-symbols ^ and $
      StringBuffer buffer = new StringBuffer(filterString);
      int length = filterString.length();
      if (length == 0 || filterString.charAt(0) != '^')
        buffer.insert(0, ".*");
      if (length == 0 || filterString.charAt(length - 1) != '$')
        buffer.append(".*");
      regexp = buffer.toString();
      break;
    default:
      regexp = null;
      break;
    }
    if (regexp == null) {
      return null;
    } else {
      int flags = Pattern.DOTALL;
      if (ignoreCase)
        flags |= Pattern.CASE_INSENSITIVE;
      return Pattern.compile(regexp, flags);
    }
  }

  // makes a pattern-parseable string, with changing * to .*
  private String escapeSimple(String filterString) {
    int length = filterString.length();
    StringBuffer result = new StringBuffer(length << 1);
    for (int i = 0; i < length; i++) {
      char c = filterString.charAt(i);
      if (Character.isWhitespace(c) || Character.isLetterOrDigit(c)) {
        result.append(c);
      } else if (c == '*') {
        result.append(".*");
      } else {
        result.append('\\').append(c);
      }
    }
    return result.toString();
  }

  public boolean isAccepted(@NotNull String string) {
    if (myPattern != null) {
      return myPattern.matcher(string).matches();
    }
    switch (myMatchType) {
    case EXACT:
      return myIgnoreCase ? myFilterString.equalsIgnoreCase(string) : myFilterString.equals(string);
    case INVALID:
      return false;
    default:
      assert false : this + " should have pattern";
      return false;
    }
  }

  @NotNull
  public static StringFilter readFrom(ReadonlyConfiguration config) {
    MatchType matchType = MatchType.fromInt(config.getIntegerSetting(TYPE_SETTING, -1));
    String filter = config.getSetting(FILTER_SETTING, "").trim();
    if (filter.length() == 0)
      matchType = MatchType.INVALID;
    boolean ignoreCase = config.getBooleanSetting(IGNORE_CASE_SETTING, true);
    return new StringFilter(matchType, filter, ignoreCase);
  }

  public void writeTo(Configuration config) {
    config.setSetting(TYPE_SETTING, myMatchType.myId);
    config.setSetting(FILTER_SETTING, myFilterString);
    config.setSetting(IGNORE_CASE_SETTING, myIgnoreCase);
  }

  @Nullable
  public static StringFilter readFromString(String string) {
    if (string == null)
      return null;
    string = string.trim();
    boolean caseSensitive = false;
    MatchType type = null;

    if (string.startsWith(ENCODE_CASE_SENSITIVE)) {
      caseSensitive = true;
      string = string.substring(ENCODE_CASE_SENSITIVE.length());
    }

    for (MatchType matchType : MatchType.values()) {
      String encoded = matchType.myEncoded;
      if (encoded.length()> 0 && string.startsWith(encoded)) {
        type = matchType;
        string = string.substring(encoded.length());
        break;
      }
    }

    if (type == MatchType.INVALID)
      return null;

    if (type == null)
      type = MatchType.EXACT;

    string = string.trim();
    if (string.length() == 0)
      return null;

    return new StringFilter(type, string, !caseSensitive);
  }

  @Nullable
  public String writeToString() {
    if (myMatchType == MatchType.INVALID || myMatchType == null)
      return null;
    StringBuffer buffer = new StringBuffer();
    if (!myIgnoreCase)
      buffer.append(ENCODE_CASE_SENSITIVE);
    buffer.append(myMatchType.myEncoded);
    buffer.append(myFilterString);
    return buffer.toString();
  }

  public static enum MatchType {
    EXACT(0, "E&xact match (no wildcards)", "", "equals "),
    CONTAINS(1, "Co&ntains filter string (* - any characters)", "containing", "containing "),
    ENDS_WITH(2, "&Ends with filter string (* - any characters)", "ending with", "ending with "),
    STARTS_WITH(3, "&Starts with filter string (* - any characters)", "starting with", "starting with "),
    REGEXP(4, "&Regular expression match", "matching regexp", "matching regexp "),
    INVALID(5, "", "", "invalid");

    private final int myId;
    private final String myLongDescriptionWithMnemonic;
    private final String myLangPrefix;
    private final String myEncoded;

    private MatchType(int id, String longDescriptionWithMnemonic, String langPrefix, String encoded) {
      myId = id;
      myLongDescriptionWithMnemonic = longDescriptionWithMnemonic;
      myLangPrefix = langPrefix;
      myEncoded = encoded;
    }

    public static MatchType fromInt(int code) {
      for (MatchType type : values()) {
        if (type.myId == code)
          return type;
      }
      return INVALID;
    }

    public int getId() {
      return myId;
    }

    public String getLongDescriptionWithMnemonic() {
      return myLongDescriptionWithMnemonic;
    }

    public String getLangPrefix() {
      return myLangPrefix;
    }
  }
}
