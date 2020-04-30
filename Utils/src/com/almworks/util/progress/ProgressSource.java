package com.almworks.util.progress;

import com.almworks.util.collections.RemoveableModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface ProgressSource {
  ProgressSource STUB = ProgressStub.INSTANCE;

  double getProgress();

  @Nullable
  ProgressActivity getActivity();

  @NotNull
  RemoveableModifiable getModifiable();

  boolean isDone();

  @Nullable
  List<String> getErrors(@Nullable List<String> target);

  @NotNull
  ProgressData getProgressData();
}
