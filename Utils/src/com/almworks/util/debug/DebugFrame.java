package com.almworks.util.debug;

import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Computable;

import javax.swing.*;
import java.awt.*;

public class DebugFrame extends JFrame {
  public DebugFrame() {
    super("debug");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  public static JFrame show(Component c, int width, int height) {
    return show(c, width, height, false);
  }

  public static JFrame show(final Component c, final int width, final int height, final boolean installLaf) {
    return ThreadGate.AWT_IMMEDIATE.compute(new Computable<JFrame>() {
      public JFrame compute() {
        if (installLaf)
          LAFUtil.initializeLookAndFeel();

        DebugFrame frame = new DebugFrame();
        frame.getContentPane().add(c);

        if (width <= 0 || height <= 0)
          frame.pack();
        else
          frame.setSize(width, height);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = frame.getSize();
        frame.setLocation((screen.width - size.width) / 2, (screen.height - size.height) / 2);

        frame.show();
        return frame;
      }
    });
  }

  public static JFrame show(Component c) {
    return show(c, 0, 0, false);
  }

  public static JFrame show(Component c, boolean installLaf) {
    return show(c, 0, 0, installLaf);
  }
}
