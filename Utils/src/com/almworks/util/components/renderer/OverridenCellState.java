package com.almworks.util.components.renderer;

import javax.swing.border.Border;
import java.awt.*;

public class OverridenCellState extends DelegatingCellState {
  private Color myForeground = null;
  private Border myNullBorder = null;
  private Boolean myExpanded = null;

  public OverridenCellState(CellState state) {
    super(state);
  }

  public OverridenCellState setForeground(Color foreground) {
    myForeground = foreground;
    return this;
  }

  public OverridenCellState setNullBorder(Border nullBorder) {
    myNullBorder = nullBorder;
    return this;
  }

  public OverridenCellState setExpanded(boolean expanded) {
    myExpanded = expanded;
    return this;
  }

  public Color getForeground() {
    return myForeground != null ? myForeground : super.getForeground();
  }

  public Border getBorder() {
    Border border = super.getBorder();
    return border != null ? border : myNullBorder;
  }

  public boolean isEnabled() {
    return myExpanded != null ? myExpanded : super.isEnabled();
  }

  public static CellState overrideForeground(CellState state, Color foreground) {
    return foreground != null ? new OverridenCellState(state).setForeground(foreground) : state;
  }
}
