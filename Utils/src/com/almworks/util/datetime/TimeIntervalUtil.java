package com.almworks.util.datetime;

import com.almworks.util.Pair;
import com.almworks.util.collections.Containers;
import com.almworks.util.text.TextUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeIntervalUtil {
  private static final Pattern PATTERN;
  private static final List<Pair<String, Integer>> SUFFIXES;
  private static final List<Pair<Pattern, Integer>> MULTIPLIERS;

  static {
    List<Pair<String, Integer>> suffixes = Collections15.arrayList();
    suffixes.add(Pair.create("w", 60 * 60 * 24 * 7));
    suffixes.add(Pair.create("d", 60 * 60 * 24));
    suffixes.add(Pair.create("h", 60 * 60));
    suffixes.add(Pair.create("m", 60));
    suffixes.add(Pair.create("s", 1));
    Collections.sort(suffixes, Containers.convertingComparator(Pair.<Integer>convertorGetSecond()));
    Collections.reverse(suffixes);
    SUFFIXES = Collections.unmodifiableList(suffixes);
    List<Pair<Pattern, Integer>> multipliers = Collections15.arrayList();
    for (Pair<String, Integer> suffix : SUFFIXES)
      multipliers.add(Pair.create(Pattern.compile("(\\d+)\\s*" + suffix.getFirst()), suffix.getSecond()));
    MULTIPLIERS = Collections.unmodifiableList(multipliers);
    String allSuffixes = TextUtil.separate(SUFFIXES, "|", Pair.<String>convertorGetFirst());
    PATTERN = Pattern.compile("\\s*((\\d+)\\s*(" + allSuffixes + ")\\s*)*");
  }

  public static Integer parseText(String text, @Nullable ArrayList<String> errorCollector) {
    text = Util.NN(text).trim();
    if (text.isEmpty()) return null;
    try {
      if (Integer.parseInt(text) == 0) return 0;
    } catch (NumberFormatException e) {
      // ignore
    }
    int sum = 0;
    boolean anyFound = false;
    for (Pair<Pattern, Integer> pair : MULTIPLIERS) {
      Matcher m = pair.getFirst().matcher(text);
      if (!m.find()) continue;
      String number = m.group(1);
      int unit;
      try {
        unit = Integer.parseInt(number);
      } catch (NumberFormatException e) {
        if (errorCollector != null) errorCollector.add("not a number '" + number + "'");
        continue;
      }
      if (unit <= 0) continue;
      sum += unit * pair.getSecond();
      anyFound = true;
      if (errorCollector != null) {
        if (m.find()) errorCollector.add("multiple occurrences of " + pair.getFirst().pattern());
      }
    }
    if (errorCollector != null && !PATTERN.matcher(text).matches()) errorCollector.add("syntax error detected");
    return anyFound ? sum : null;
  }

  public static String toTextDuration(Integer seconds) {
    if (seconds == null || seconds < 0) return "";
    if (seconds == 0) return "0";
    int remainder = seconds;
    StringBuilder builder = new StringBuilder();
    for (Pair<String, Integer> suffix : SUFFIXES) {
      int divisor = suffix.getSecond();
      int val = remainder / divisor;
      remainder = remainder % divisor;
      if (val != 0) {
        if (builder.length() > 0) builder.append(" ");
        builder.append(val).append(suffix.getFirst());
      }
    }
    return builder.toString();
  }

  public static Integer parseText(String text) {
    return parseText(text, null);
  }
}
