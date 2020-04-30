package com.almworks.util.images;

import com.almworks.util.ui.EmptyIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class IconHandle extends ImageHandle<IconHandle> implements javax.swing.Icon {
  protected EmptyIcon myEmptyIcon;
  private Icon myIcon;
  private Icon myGrayedIcon;
  private volatile BufferedImage myCachedImage;
  private boolean myImageCached = true;

  private IconHandle(int imageID, int assumedWidth, int assumedHeight, String resourcePath, ClassLoader loader) {
    super(imageID, assumedWidth, assumedHeight, resourcePath, loader);
  }

  public static IconHandle smallIcon(int imageID, String resourcePath) {
    return new IconHandle(imageID, 16, 16, resourcePath, IconHandle.class.getClassLoader());
  }

  public static IconHandle smallIcon(int imageID) {
    return new IconHandle(imageID, 16, 16, getDefaultResource(imageID), IconHandle.class.getClassLoader());
  }

  public static IconHandle mediumIcon(int imageID) {
    return new IconHandle(imageID, 32, 32, getDefaultResource(imageID), IconHandle.class.getClassLoader());
  }

  public static IconHandle icon(int imageID, int width, int height) {
    return new IconHandle(imageID, width, height, getDefaultResource(imageID), IconHandle.class.getClassLoader());
  }

  public void setImageCached(boolean cacheImage) {
    myImageCached = cacheImage;
  }

  public int getIconHeight() {
    return getIcon().getIconHeight();
  }

  public int getIconWidth() {
    return getIcon().getIconWidth();
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    BufferedImage image = myCachedImage;
    if (image != null) {
      g.drawImage(image, x, y, c);
    } else {
      Icon icon = getIcon();
      icon.paintIcon(c, g, x, y);
      int w = icon.getIconWidth();
      int h = icon.getIconHeight();
      if (w > 0 && h > 0) {
        image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        icon.paintIcon(null, image.getGraphics(), 0, 0);
      }
      myCachedImage = image;
    }
  }

  public Icon getEmpty() {
    if (myEmptyIcon == null)
      myEmptyIcon = new EmptyIcon(getIconWidth(), getIconHeight());
    return myEmptyIcon;
  }

  public Icon getIcon() {
    if (myIcon == null) {
      Image image = getImage();
      if (image == null) {
        myIcon = new EmptyIcon(myAssumedWidth, myAssumedHeight);
      } else {
        myIcon = new ImageIcon(image);
      }
    }
    return myIcon;
  }

  public Icon getGrayed() {
    if (myGrayedIcon == null) {
      myGrayedIcon = new ImageIcon(ImageUtil.createGrayscaleImage(((ImageIcon) getIcon()).getImage()));
    }
    return myGrayedIcon;
  }
}
