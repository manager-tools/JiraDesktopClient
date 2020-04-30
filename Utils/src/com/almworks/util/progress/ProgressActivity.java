package com.almworks.util.progress;

import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProgressActivity {
  @Nullable
  private final Object myActivity;

  // reference to linked list of activities of delegate progresses
  @Nullable
  private final ProgressActivity mySubactivitiesHead;

  // next activity in delegate activities list of parent activity
  @Nullable
  private ProgressActivity myNext;


  ProgressActivity(@Nullable Object activity, @Nullable ProgressActivity subactivitiesHead) {
    myActivity = activity;
    mySubactivitiesHead = subactivitiesHead;
  }

  void setNext(@NotNull ProgressActivity next) {
    assert myNext == null;
    myNext = next;
  }

  @Nullable
  public Object getActivity() {
    return myActivity;
  }

  @Nullable
  public ProgressActivity getSubactivitiesHead() {
    return mySubactivitiesHead;
  }

  @Nullable
  public ProgressActivity getNext() {
    return myNext;
  }

  public String toString() {
    return ProgressActivityFormat.DEFAULT.format(this);
  }

  public <T> T getActivity(Class<T> activityClass) {
    if (myActivity != null) {
      T cast = Util.castNullable(activityClass, myActivity);
      if (cast != null) return cast;
    }
    for (ProgressActivity pa = mySubactivitiesHead; pa != null; pa = pa.getNext()) {
      T value = pa.getActivity(activityClass);
      if (value != null)
        return value;
    }
    return null;
  }
}
