package com.almworks.util.ui;

import com.almworks.util.components.ScrollablePanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * :todoc:
 *
 * @author sereda
 */
public class WelcomePanel extends JPanel {
  private static final Border EMPTY = new EmptyBorder(0, 0, 0, 0);

  public WelcomePanel(JComponent component, float alignmentX, float alignmentY, Color bg) {
    super(new SingleChildLayout());
    setOpaque(true);
    setBackground(bg == null ? getDefaultBackground() : bg);
    setBorder(EMPTY);
    setComponent(component, alignmentX, alignmentY);
  }

  private static Color getDefaultBackground() {
    // zen!
    return ColorUtil.between(UIManager.getColor("EditorPane.background"), GlobalColors.CORPORATE_COLOR_1, 0.00F);
  }

  private void setComponent(JComponent component, float alignmentX, float alignmentY) {
    removeAll();
    component.setAlignmentX(alignmentX);
    component.setAlignmentY(alignmentY);
    add(component);
  }

  public static JComponent create(JComponent component) {
    return create(component, null);
  }

  public static JComponent create(JComponent component, Color bg) {
    return new JScrollPane(new ScrollablePanel(new WelcomePanel(component, 0, 0, bg)));
  }
}
