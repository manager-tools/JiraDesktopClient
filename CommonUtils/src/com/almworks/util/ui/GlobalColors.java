package com.almworks.util.ui;

import com.almworks.util.Pair;

import java.awt.*;

public class GlobalColors {
  public static final Color CORPORATE_COLOR_1 = new Color(0x4C6889);
  public static final Color ERROR_COLOR = new Color(0x991111);
  public static final Color DRAG_AND_DROP_COLOR = new Color(0xFF3F3F);
  public static final Color DRAG_AND_DROP_DARK_COLOR = new Color(0x991111);
  public static final Color HIGHLIGHT_COLOR = ColorUtil.between(Color.WHITE, Color.GREEN, 0.7f);
  
  public static final Pair<Color, Color> DND_DESCRIPTION_DARK_FG_BG = Pair.create(Color.WHITE, DRAG_AND_DROP_DARK_COLOR);

}
