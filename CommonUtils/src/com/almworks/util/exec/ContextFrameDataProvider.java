package com.almworks.util.exec;

import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ContextFrameDataProvider extends ContextDataProvider {
  private final ContextFrame myTopFrame;

  public ContextFrameDataProvider(@NotNull ContextFrame topFrame) {
    myTopFrame = topFrame;
    assert topFrame.getProvider() != null : topFrame + " " + this;
  }

  @Nullable
  public static ContextFrameDataProvider create(@Nullable ContextFrame topFrame) {
    return topFrame == null ? null : new ContextFrameDataProvider(topFrame);
  }

  public <T> T getObject(TypedKey<T> key, int depth) throws ContextDepthException {
    return ContextFrame.getObject(myTopFrame, null, key, depth);
  }

  public <T> T getObject(Class<T> objectClass, int depth) throws ContextDepthException {
    return ContextFrame.getObject(myTopFrame, objectClass, null, depth);
  }

  ContextFrame getTopFrame() {
    return myTopFrame;
  }
}
