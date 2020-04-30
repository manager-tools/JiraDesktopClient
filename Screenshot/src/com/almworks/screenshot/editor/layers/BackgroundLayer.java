package com.almworks.screenshot.editor.layers;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

public class BackgroundLayer implements Layer {
  private BufferedImage myImage;
  private BufferedImage myTransformedImage;
  private final Color myBackgroundColor = Color.WHITE;
  private Rectangle myImageRect;

  public BackgroundLayer(BufferedImage image, LayerImageControl imageControl) {
    myImage = image;
    myImageRect = new Rectangle(0, 0, myImage.getWidth(), myImage.getHeight());
  }

  public BufferedImage getSubimage(Rectangle rect) {
    rect = getBounds().intersection(rect);
    return myImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
  }

  public Rectangle getBounds() {
    return new Rectangle(myImage.getWidth(), myImage.getHeight());
  }

  public void paint(Graphics2D g2, Area clip) {    
    Rectangle bound = clip.getBounds();
    
    BufferedImage image;
    if (bound.x == 0 && bound.y == 0 && bound.width == myImage.getWidth() && bound.height == myImage.getHeight()) {
      image = myImage;
    } else {
      bound = bound.intersection(myImageRect);

      image = myImage.getSubimage(bound.x, bound.y, bound.width, bound.height);
    }

    g2.drawImage(image, bound.x, bound.y, null);
  }

  public void processMouseEvent(MouseEvent e) {
  }

  public void processKeyEvent(KeyEvent e) {
  }

  public void resize(Rectangle oldBounds, Rectangle newBounds) {
  }

  /*private static class MyImageObserver implements ImageObserver {
    private boolean myDone;

    public synchronized boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
      boolean stop = (flags & (ALLBITS | FRAMEBITS | ABORT | ERROR)) != 0;
      if (!myDone && stop) {
        myDone = stop;
        notify();
      }
      return stop;
    }

    public synchronized void clear() {
      myDone = false;
    }

    public synchronized void waitDone() {
      try {
        while (!myDone) {
          wait(100);
        }
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }

    }
  }*/
}
