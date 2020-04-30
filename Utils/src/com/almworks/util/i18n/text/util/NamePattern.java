package com.almworks.util.i18n.text.util;

import com.almworks.util.i18n.text.LocalizedAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamePattern {
  private final LocalizedAccessor.MessageIntStr myPattern;
  /**
   * Regexp to split the name into user-entered name (group 1) and name index (group 2)
   */
  private final LocalizedAccessor.Value myAppliedRegexp;

  public NamePattern(LocalizedAccessor.MessageIntStr pattern, LocalizedAccessor.Value applied) {
    myPattern = pattern;
    myAppliedRegexp = applied;
  }

  public static <T> String nextName(Set<String> names, LocalizedAccessor.MessageInt pattern) {
    int i = 0;
    while (true) {
      String generated = pattern.formatMessage(i);
      if (!names.contains(generated)) return generated;
      i++;
    }
  }

  public String generateName(Set<String> names, String sourceName) {
    Pattern pattern = getPattern();
    Matcher m = pattern.matcher(sourceName);
    if (m.matches()) sourceName = m.group(1);
    return nextName(names, myPattern.applyStr(sourceName));
  }

  @Nullable
  public Integer getNameIndex(String name) {
    Matcher m = getPattern().matcher(name);
    String strIndex = m.matches() ? m.group(2) : null;
    if (strIndex == null || strIndex.isEmpty()) return null;
    try {
      return Integer.parseInt(strIndex);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public String generateName(String name, int index) {
    return myPattern.formatMessage(index, name);
  }

  private Pattern getPattern() {
    return Pattern.compile(myAppliedRegexp.create());
  }
}
