package com.almworks.restconnector.json;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelfIdExtractor extends Convertor<Object, Integer> {
  private final Pattern myPattern;

  public SelfIdExtractor(String prefix) {
    myPattern = Pattern.compile(prefix + "(\\d+)$");
  }

  @Override
  public Integer convert(Object value) {
    String string = JSONKey.TEXT.convert(value);
    if (string == null) return null;
    Matcher m = myPattern.matcher(string);
    if (!m.find()) {
      LogHelper.error("Failed to extract ID", string);
      return null;
    }
    return JSONKey.INTEGER.convert(m.group(1));
  }
}
