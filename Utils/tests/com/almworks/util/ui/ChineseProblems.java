package com.almworks.util.ui;

import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.debug.DebugFrame;
import com.almworks.util.exec.ThreadGate;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ChineseProblems implements Runnable {
  public static void main(String[] args) throws IOException {
    ThreadGate.AWT.execute(new ChineseProblems());
  }

  public void run() {
    LAFUtil.initializeLookAndFeel();
    FileInputStream in = null;
    try {
      in = new FileInputStream("c:\\x");
      Charset UTF = Charset.forName("UTF-8");
      String s = new BufferedReader(new InputStreamReader(in, UTF)).readLine();
      Box box = new Box(BoxLayout.Y_AXIS);
      GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      Font[] allFonts = ge.getAllFonts();
      for (Font font : allFonts) {
        JTextField f = new JTextField(s);
        f.setFont(font.deriveFont(11F));
        box.add(new JLabel(font.getName() + ":"));
        box.add(f);
      }
      DebugFrame.show(new JScrollPane(box), 500, 500);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (in != null)
        try {
          in.close();
        } catch (IOException e) {
          //
        }
    }
  }
}
