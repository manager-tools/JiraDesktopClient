package com.almworks.util.components.plaf.patches;

import com.almworks.util.components.plaf.LAFExtension;

import javax.swing.*;

public abstract class WindowsLAFPatch extends LAFExtension {

  public boolean isExtendingLookAndFeel(LookAndFeel laf) {
    return isWindowsLAF(laf);
  }

}
