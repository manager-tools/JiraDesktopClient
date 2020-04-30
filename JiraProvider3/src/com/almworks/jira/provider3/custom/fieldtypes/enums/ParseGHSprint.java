package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.util.LogHelper;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParseGHSprint {
  private static final Pattern PREFIX = Pattern.compile("^com.atlassian.greenhopper.service.sprint.Sprint@[0-9A-Fa-f]+");
  private static final Pattern ANY_KEY = Pattern.compile(",([^=]*)=");
  private static final List<String> KEYS = Collections15.unmodifiableListCopy("rapidViewId", "state", "name", "startDate", "endDate", "completeDate", "id", "closed");
  private final String myValue;
  private int myPosition = 0;

  private String myName = null;
  private String myId = null;

  private ParseGHSprint(String value) {
    myValue = value;
  }

  @NotNull
  public static ParseGHSprint perform(String value) {
    Matcher m = PREFIX.matcher(value);
    if (!m.find()) {
      LogHelper.warning("Wrong GH-Sprint prefix", value);
      return new ParseGHSprint("");
    }
    int start = m.end();
    if (start >= value.length() || value.charAt(start) != '[' || value.charAt(value.length() - 1) != ']') {
      LogHelper.warning("Wrong GH-Sprint value", value);
      return new ParseGHSprint("");
    }
    ParseGHSprint parser = new ParseGHSprint(value.substring(start + 1, value.length() - 1));
    parser.perform();
    return parser;
  }

  public String getName() {
    return myName;
  }

  public String getId() {
    return myId;
  }

  private void perform() {
    String prevKey = null;
    while (myPosition < myValue.length() && (myName == null || myId == null)) {
      if (prevKey == null) prevKey = readKey();
      if (prevKey == null) return;
      int valueStart = myPosition;
      String nextKey = nextKey();
      int valueEnd;
      if (nextKey != null) valueEnd = myPosition - nextKey.length() - 2;
      else valueEnd = myValue.length();
      if ("name".equals(prevKey)) myName = myValue.substring(valueStart, valueEnd);
      else if ("id".equals(prevKey)) myId = myValue.substring(valueStart, valueEnd);
      prevKey = nextKey;
    }
  }

  private String nextKey() {
    int bestStart = myValue.length();
    int keyIndex = -1;
    for (int i = 0, keysSize = KEYS.size(); i < keysSize; i++) {
      String key = KEYS.get(i);
      int index = myPosition;
      while (index < myValue.length()) {
        index = myValue.indexOf(key, index);
        if (index < 0 || index >= bestStart) break;
        int eqIndex = index + key.length();
        if (eqIndex >= myValue.length() - 1) {
          index = -1;
          break;
        }
        if (myValue.charAt(index - 1) != ',' || myValue.charAt(eqIndex) != '=') {
          index++;
          continue;
        }
        break;
      }
      if (index < 0 || index >= bestStart) continue;
      bestStart = index;
      keyIndex = i;
    }
    if (keyIndex < 0) {
      Matcher m = ANY_KEY.matcher(myValue);
      if (m.find(myPosition)) {
        myPosition = m.end();
        return m.group(1);
      }
      myPosition = myValue.length();
      return null;
    }
    String key = KEYS.get(keyIndex);
    myPosition = bestStart + key.length() + 1;
    return key;
  }

  @Nullable
  private String readKey() {
    if (myPosition >= myValue.length()) {
      LogHelper.warning("GH-Sprint: no key at end", myValue);
      return null;
    }
    int eqIndex = myValue.indexOf('=', myPosition);
    if (eqIndex < 0) {
      LogHelper.warning("GH-Sprint: key not found", myPosition, myValue);
      return null;
    }
    String key = myValue.substring(myPosition, eqIndex);
    myPosition = eqIndex + 1;
    return key;
  }
}
