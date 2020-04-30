package com.almworks.screenshot.shooter;

import org.almworks.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Stalex
 */
public class ScreenCapturer implements Capturer {

  private Toolkit myToolkit = Toolkit.getDefaultToolkit();

  private Robot myRobot = null;

  public BufferedImage capture(GraphicsDevice device) {
    if (device == null)
      device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    myToolkit.sync();
    try {
      myRobot = new Robot(device);
    } catch (AWTException e) {
      JOptionPane.showMessageDialog(null, e.getMessage());
      Log.warn(e);
      return null;
    }
    myRobot.setAutoDelay(0);
    myRobot.setAutoWaitForIdle(false);
    myRobot.delay(200);
    Rectangle screenRect = device.getDefaultConfiguration().getBounds();
    return myRobot.createScreenCapture(screenRect);
  }
}

