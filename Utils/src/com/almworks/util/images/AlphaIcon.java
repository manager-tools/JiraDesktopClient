package com.almworks.util.images;

import javax.swing.*;
import java.awt.*;

public class AlphaIcon extends EmptyIcon {
  private final Composite myComposite;

  public AlphaIcon(Icon baseIcon, float alpha) {
    super(baseIcon);
    myComposite = AlphaComposite.SrcOver.derive(alpha);
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    final Graphics2D g2 = (Graphics2D) g.create();
    g2.setComposite(myComposite);
    myBaseIcon.paintIcon(c, g2, x, y);
    g2.dispose();
  }
}
