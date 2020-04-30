package com.almworks.util.components;

import java.awt.*;

public interface SizeDelegating {
  void setSizeDelegate(SizeDelegate delegate);

  Dimension getSuperSize(SizeKey sizeKey);

  Dimension getSize(SizeKey sizeKey);
}
