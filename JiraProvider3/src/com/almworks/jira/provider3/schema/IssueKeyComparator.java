package com.almworks.jira.provider3.schema;

import java.util.Comparator;

public class IssueKeyComparator implements Comparator<String> {
  // quick comparator
  public static final Comparator<String> INSTANCE = new IssueKeyComparator();

  public int compare(String o1, String o2) {
    if (o1 == null)
      return o2 == null ? 0 : -1;
    if (o2 == null)
      return 1;
    int length1 = o1.length();
    int length2 = o2.length();
    int minLength = length1 < length2 ? length1 : length2;
    int i;

    // compare projects in letter order
    for (i = 0; i < minLength; i++) {
      char c1 = o1.charAt(i);
      char c2 = o2.charAt(i);
      if (c1 == c2) {
        if (c1 == '-') {
          break;
        } else {
          continue;
        }
      }
      char uc1 = Character.toUpperCase(c1);
      char uc2 = Character.toUpperCase(c2);
      if (uc1 < uc2)
        return -1;
      else if (uc1 > uc2)
        return 1;
    }

    if (i == minLength) {
      // no '-' encountered, but one string contains another.
      if (length2 > minLength)
        return -1;
      else if (length1 > minLength)
        return 1;
      else
        return 0;
    } else {
      // same projects, there might be IDs.
      assert o1.charAt(i) == '-' && o2.charAt(i) == '-';

      // check if any ID has fewer digits.
      if (length1 < length2) {
        return -1;
      } else if (length1 > length2) {
        return 1;
      }

      assert minLength == length1 && minLength == length2;
      for (i++; i < minLength; i++) {
        char c1 = o1.charAt(i);
        char c2 = o2.charAt(i);
        if (c1 < c2)
          return -1;
        else if (c1 > c2)
          return 1;
      }

      return 0;
    }
  }
}
