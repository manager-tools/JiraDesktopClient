package com.almworks.export.csv;

import com.almworks.api.application.ExportValueType;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;

public class CSVUtil {
  public static String escapeString(@NotNull String value, ExportValueType type, boolean useQuotes, char delimiter,
    boolean protectFormula)
  {
    if (protectFormula) {
      boolean formulaProtectionRequired = isFormulaProtectionRequired(value, type);
      if (formulaProtectionRequired) {
        value = "=\"" + doubleQuote(value) + "\"";
        useQuotes = true;
      }
    }
    if (!useQuotes) {
      useQuotes = areQuotesRequired(value, delimiter);
    }
    if (useQuotes) {
      value = '"' + doubleQuote(value) + '"';
    }
    return value;
  }

  static boolean isFormulaProtectionRequired(String value, ExportValueType type) {
    if (type != ExportValueType.STRING && type != ExportValueType.LARGE_STRING) {
      // dates and numbers should be formatted according to local style
      return false;
    }
    int length = value.length();
    for (int i = 0; i < length; i++) {
      char c = value.charAt(i);
      if (c == '@' || c == '=' || c == '\'' || c == '.' || c == ',' || c == '-' || c == '+' || c == ':')
        return true;
    }
    return false;
  }

  static boolean areQuotesRequired(String value, char delimiter) {
    int len = value.length();
    if (len == 0)
      return false;
    if (Character.isWhitespace(value.charAt(0)) || Character.isWhitespace(value.charAt(len - 1))) {
      // whitespace in the beginning or in the end of text
      return true;
    }
    for (int i = 0; i < len; i++) {
      char c = value.charAt(i);
      if (c == ',' || c == ';' || c == '\'' || c == delimiter || c == '"' || c == '\n' || c == '\r') {
        // special chars
        return true;
      }
    }
    return false;
  }

  static String doubleQuote(String value) {
    StringBuffer buffer = new StringBuffer(value.length() * 3 / 2);
    int len = value.length();
    for (int i = 0; i < len; i++) {
      char c = value.charAt(i);
      if (c == '"')
        buffer.append("\"\"");
      else
        buffer.append(c);
    }
    return buffer.toString();
  }

  public static char guessDefaultListSeparator() {
    NumberFormat format = NumberFormat.getInstance();
    String s = format.format(0.1F);
    if (s.indexOf(',') >= 0)
      return ';';
    else
      return ',';
  }
}
