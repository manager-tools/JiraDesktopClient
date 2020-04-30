package com.almworks.util.progress;

import org.jetbrains.annotations.NotNull;

public class ProgressActivityFormat {
  public static final ProgressActivityFormat DEFAULT = new ProgressActivityFormat();
  
  private final int myMaximumDepth;
  private final boolean myFlat;

  public ProgressActivityFormat(boolean flat, int maximumDepth) {
    myFlat = flat;
    myMaximumDepth = maximumDepth;
  }

  public ProgressActivityFormat() {
    this(false, -1);
  }

  @NotNull
  public StringBuffer format(ProgressActivity activity, StringBuffer buffer) {
    if (buffer == null)
      buffer = new StringBuffer();
    if (activity != null)
      format(buffer, activity, 0);
    return buffer;
  }

  @NotNull
  public String format(ProgressActivity activity) {
    return format(activity, null).toString();
  }

  private void format(StringBuffer buffer, ProgressActivity activity, int depth) {
    assert depth <= myMaximumDepth || myMaximumDepth < 0 : this;
    int initialOffset = buffer.length();
    Object activityObject = activity.getActivity();
    if (activityObject != null) {
      buffer.append(activityObject);
    }
    int activityOffset = buffer.length();
    if (myMaximumDepth < 0 || depth < myMaximumDepth) {
      int last = -1;
      for (ProgressActivity sub = activity.getSubactivitiesHead(); sub != null; sub = sub.getNext()) {
        format(buffer, sub, depth + 1);
        int len = buffer.length();
        if (last < 0) {
          last = len;
        } else if (last != len) {
          buffer.insert(last, ", ");
          last = len + 2;
        }
      }
    }
    int subOffset = buffer.length();
    if (initialOffset < activityOffset && activityOffset < subOffset) {
      // both sub-activities and this activity have textual representation
      if (myFlat) {
        buffer.insert(activityOffset, ", ");
      } else {
        buffer.insert(activityOffset, " (");
        buffer.append(")");
      }
    }
  }
}
