package com.almworks.util.components.layout;

import com.almworks.util.threads.Threads;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class WidthDrivenStackedCouple implements WidthDrivenComponent {
  private final WidthDrivenComponent myNorth;
  private final WidthDrivenComponent myCenter;
  private final int myGap;

  @Nullable
  private JPanel myPanel;

  public WidthDrivenStackedCouple(WidthDrivenComponent north, WidthDrivenComponent center) {
    this(north, center, 0);
  }

  public WidthDrivenStackedCouple(WidthDrivenComponent north, WidthDrivenComponent center, int gap) {
    myNorth = north;
    myCenter = center;
    myGap = gap;
  }

  public int getPreferredWidth() {
    int north = myNorth.isVisibleComponent() ? myNorth.getPreferredWidth() : 0;
    int center = myCenter.isVisibleComponent() ? myCenter.getPreferredWidth() : 0;
    return Math.max(north, center);
  }

  public int getPreferredHeight(int width) {
    int size = 0;
    boolean north = myNorth.isVisibleComponent();
    boolean center = myCenter.isVisibleComponent();
    if (north)
      size += myNorth.getPreferredHeight(width);
    if (center)
      size += myCenter.getPreferredHeight(width);
    if (north && center)
      size += myGap;
    return size;
  }

  @NotNull
  public JComponent getComponent() {
    Threads.assertAWTThread();
    JPanel panel = myPanel;
    if (panel == null) {
      panel = createPanel();
      panel.setLayout(new BorderLayout(0, myGap));
      panel.add(myNorth.getComponent(), BorderLayout.NORTH);
      panel.add(myCenter.getComponent(), BorderLayout.CENTER);
      myPanel = panel;
    }
    return panel;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible() && (myNorth.isVisibleComponent() || myCenter.isVisibleComponent());
  }

  protected JPanel createPanel() {
    return new JPanel();
  }
}
