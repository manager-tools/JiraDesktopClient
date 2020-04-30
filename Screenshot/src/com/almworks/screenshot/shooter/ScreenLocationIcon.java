package com.almworks.screenshot.shooter;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class ScreenLocationIcon implements Icon {
  private final int mySize;
  private final Rectangle[] myDevices;
  private final int myMainIndex;

  public ScreenLocationIcon(int size, Rectangle[] devices, int mainIndex) {
    mySize = size;
    myDevices = devices;
    myMainIndex = mainIndex;
  }

  public static Icon[] createIcons(int size, GraphicsDevice[] devices) {
    Rectangle[] rectangles = new Rectangle[devices.length];
    for (int i = 0; i < devices.length; i++) rectangles[i] = devices[i].getDefaultConfiguration().getBounds();
    Icon[] icons = new Icon[devices.length];
    for (int i = 0; i < rectangles.length; i++) icons[i] = new ScreenLocationIcon(size, rectangles, i);
    return icons;
  }

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    Rectangle area = new Rectangle(myDevices[0]);
    for (int i = 1; i < myDevices.length; i++) {
      Rectangle r = myDevices[i];
      int minX = Math.min(area.x, r.x);
      int minY = Math.min(area.y, r.y);
      int maxX = Math.max(area.x + area.width, r.x + r.width);
      int maxY = Math.max(area.y + area.height, r.y + r.height);
      area.setBounds(minX, minY, maxX - minX, maxY - minY);
    }
    Color savedColor = g.getColor();
    g.setColor(Color.GRAY);
    g.fillRect(x, y, mySize, mySize);
    for (int i = 0; i < myDevices.length; i++) {
      if (i == myMainIndex) continue;
      drawDevice(x, y, g, area, myDevices[i], Color.BLACK);
    }
    drawDevice(x, y, g, area, myDevices[myMainIndex], Color.RED);
    g.setColor(savedColor);
  }

  private void drawDevice(int x, int y, Graphics g, Rectangle area, Rectangle device, Color color) {
    float maxDimension = (float) Math.max(area.getWidth(), area.getHeight());
    float scale = mySize / maxDimension;
    device = new Rectangle(device);
    device.setLocation(device.x - area.x, device.y - area.y);
    g.setColor(color);
    g.fillRect(x+Math.round(device.x * scale), y+Math.round(device.y*scale), Math.round(device.width * scale), Math.round(device.height*scale));
  }

  @Override
  public int getIconWidth() {
    return mySize;
  }

  @Override
  public int getIconHeight() {
    return mySize;
  }
}
