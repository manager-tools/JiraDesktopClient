package com.almworks.util.ui;

import org.almworks.util.detach.Detach;

public abstract class UIComponentWrapper2Support implements UIComponentWrapper2 {
  @Deprecated
  public void dispose() {
    Detach detach = getDetach();
    if (detach != null)
      detach.detach();
  }
}
