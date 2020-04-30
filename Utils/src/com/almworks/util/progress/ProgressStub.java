package com.almworks.util.progress;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.RemoveableModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProgressStub implements ProgressSource {
  public static final ProgressStub INSTANCE = new ProgressStub();

  private ProgressStub() {}

  public boolean isDone() {
    return true;
  }

  @Nullable
  public List<String> getErrors(List<String> target) {
    return target;
  }

  public double getProgress() {
    return 1D;
  }

  @Nullable
  public ProgressActivity getActivity() {
    return null;
  }

  @NotNull
  public RemoveableModifiable getModifiable() {
    return Modifiable.NEVER;
  }

  public String toString() {
    return ProgressStub.class.getName();
  }

  @NotNull
  public ProgressData getProgressData() {
    return ProgressData.DONE;
  }
}
