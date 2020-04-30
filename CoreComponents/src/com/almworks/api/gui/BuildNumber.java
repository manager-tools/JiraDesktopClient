package com.almworks.api.gui;

import com.almworks.util.collections.Containers;
import org.almworks.util.Log;
import org.almworks.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.almworks.util.collections.Functional.filter;
import static com.almworks.util.collections.Functional.first;
import static com.almworks.util.commons.Condition.notNull;
import static org.almworks.util.Collections15.arrayList;

/**
 * This class provides build number information and compares build numbers.
 * Build number is composed from major and minor number, minor number could be omitted.
 */
public class BuildNumber implements Comparable<BuildNumber> {
  private final int myMajor;
  private final int myMinor;
  private final String myDisplayable;
  private static final Pattern DOT_FORMAT = Pattern.compile("(\\d{4,5})([.](\\d+))?");

  private BuildNumber(int major, int minor, String displayable) {
    myMajor = major;
    myMinor = minor;
    myDisplayable = displayable;
  }

  public static BuildNumber create(String buildStr) {
    assert buildStr != null : "BN:" + buildStr;    
    return first(filter(arrayList(
      fromDotNotation(buildStr),
      fromOldFormat(buildStr),
      new BuildNumber(0, 0, Util.NN(buildStr))
    ),notNull()));    
  }

  private static BuildNumber fromDotNotation(String buildStr) {
    Matcher matcher = DOT_FORMAT.matcher(buildStr);
    if (!matcher.matches()) {
      Log.warn("BN: no match for " + buildStr);
      return null;
    }
    try {
      int major = Integer.parseInt(matcher.group(1));
      int minor = Integer.parseInt(Util.NN(matcher.group(3), "0"));
      return new BuildNumber(major, minor, createDisplayableRepresentation(major, minor));
    } catch (NumberFormatException ex) {
      Log.warn("BN: can't parse " + ex.getMessage());
      return null;
    }
  }

  private static BuildNumber fromOldFormat(String buildStr) {
    int build;
    try {
      build = Integer.parseInt(buildStr);
    } catch (NumberFormatException e) {
      return null;
    }
    int major;
    int minor;
    if (build > 100000) {
      major = build / 1000;
      minor = build % 1000;
    } else {
      major = build;
      minor = 0;
    }
    return new BuildNumber(major, minor, createDisplayableRepresentation(major, minor));
  }

  private static String createDisplayableRepresentation(int major, int minor) {
    return minor <= 0 ? Integer.toString(major) : major + "." + minor;
  }

  /**
   * @return string that could be displayed, like "844.3"
   */
  public String toDisplayableString() {
    return myDisplayable;
  }

  /**
   * @return major build number that tells where does this product come from on the head branch. Returns 0 if
   *         build information is unavailable or broken.
   */
  public int getMajor() {
    return myMajor;
  }

  /**
   * @return minor number that is kind of "patch level" for a certain major branch. returns 0 if no minor number is there.
   */
  public int getMinor() {
    return myMinor;
  }

  public int compareTo(BuildNumber that) {
    int thisMajor = getMajor();
    int thatMajor = that.getMajor();
    if (thisMajor <= 0 || thatMajor <= 0)
      return toDisplayableString().compareTo(that.toDisplayableString());
    if (thisMajor != thatMajor)
      return Containers.compareInts(thisMajor, thatMajor);
    return Containers.compareInts(getMinor(), that.getMinor());
  }

  public String toString() {
    return myDisplayable;
  }

  public int hashCode() {
    return myDisplayable.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof BuildNumber))
      return false;
    return Util.equals(myDisplayable, ((BuildNumber) obj).myDisplayable);
  }

  public int getCompositeInteger() {
    return myMinor == 0 ? myMajor : myMajor * 1000 + myMinor;
  }
}
