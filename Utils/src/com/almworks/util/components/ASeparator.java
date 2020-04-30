package com.almworks.util.components;

import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * A simple separator consisting of a label and a bevel.
 */
public class ASeparator extends JComponent {
  private final JLabel myLabel = new JLabel();
  private final Bevel myBevel = new Bevel();

  private ASeparator() {
    setLayout(new BorderLayout(5, 0));
    add(myLabel, BorderLayout.WEST);
    add(myBevel, BorderLayout.CENTER);
  }

  /**
   * @param text Separator text.
   */
  public ASeparator(String text) {
    this();
    myLabel.setText(text);
  }

  private static final Border BEVEL = UIUtil.createNorthBevel(UIManager.getColor("Panel.background"));

  private static class Bevel extends JComponent {
    @Override
    protected void paintComponent(Graphics g) {
      AwtUtil.applyRenderingHints(g);
      final Rectangle bounds = getBounds();
      BEVEL.paintBorder(this, g, 0, bounds.height / 2 + 1, bounds.width, 0);
    }
  }

  /**
   * This is needed because JComponent is abstract.
   */
  private static class Component extends JComponent {}

  /**
   * Left margin for wrapped components. 
   */
  private static final Dimension MARGIN;
  static {
    final Dimension pref = new JLabel("M").getPreferredSize();
    MARGIN = new Dimension(pref.width, 0);
  }

  /**
   * Wrap a given component with a separator.
   * @param text Separator text.
   * @param section The component to wrap with a separator.
   * @return A new component.
   */
  public static JComponent wrap(String text, @NotNull JComponent section) {
    final JComponent margin = new Component();
    margin.setPreferredSize(MARGIN);

    final JComponent wrapped = new Component();
    wrapped.setLayout(new BorderLayout(0, 5));
    wrapped.add(new ASeparator(text), BorderLayout.NORTH);
    wrapped.add(margin, BorderLayout.WEST);
    wrapped.add(section, BorderLayout.CENTER);
    return wrapped;
  }
}
