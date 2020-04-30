package com.almworks.util.progress;

import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProgressData {
  public static final ProgressData DONE = new ProgressData(1F, null, null, true);

  private final double myProgress;
  private final ProgressActivity myActivity;
  private final List<String> myErrors;
  private final boolean myDone;

  public ProgressData(double progress, ProgressActivity activity, List<String> errors, boolean done) {
    myProgress = progress;
    myActivity = activity;
    myErrors = errors == null ? null : Collections15.unmodifiableListCopy(errors);
    myDone = done;
  }

  public double getProgress() {
    return myProgress;
  }

  @Nullable
  public ProgressActivity getActivity() {
    return myActivity;
  }

  @Nullable
  public <T> T getActivity(Class<T> activityClass) {
    return myActivity == null ? null : myActivity.getActivity(activityClass);
  }

  @NotNull
  public List<String> getErrors() {
    return myErrors == null ? Collections15.<String>emptyList() : myErrors;
  }

  public boolean isDone() {
    return myDone;
  }
}
