package com.almworks.util.images;

import com.almworks.util.Enumerable;
import com.almworks.util.Env;
import org.almworks.util.Log;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Collection;

/**
 * :todoc:
 *
 * @author sereda
 */
public class ImageHandle <E extends ImageHandle> extends Enumerable<E> {
  private static Image ourStubImage;

  static {
    // todo move somewhere?
    IconHandle handle = IconHandle.smallIcon(-1, getDefaultResource(28));
    Image image = handle.getImage();
    setStubImage(image);
  }

  protected final int myAssumedWidth;
  protected final int myAssumedHeight;
  protected final String myResourcePath;
  protected final ClassLoader myLoader;
  protected final int myImageID;
  private Image myImage = null;
  private URL myURL = null;

  protected ImageHandle(int imageID, int assumedWidth, int assumedHeight, String resourcePath, ClassLoader loader) {
    // makes sure imageID is unique at runtime
    super("im." + imageID);

    myImageID = imageID;
    myAssumedWidth = assumedWidth;
    myAssumedHeight = assumedHeight;
    myResourcePath = resourcePath;
    myLoader = loader;
  }

  public synchronized Image getImage() {
    if (myImage == null)
      myImage = loadImage();
    return myImage;
  }

  private Image loadImage() {
    Image image = null;
    myURL = myLoader.getResource(myResourcePath);
    if (myURL != null) {
      image = Toolkit.getDefaultToolkit().createImage(myURL);
    }
    if (image == null)
      image = createStubImage();
    if (image == null)
      Log.warn("cannot provide image #" + myImageID);
    return image;
  }

  private synchronized Image createStubImage() {
    if (Env.isDebugging() || Env.isRunFromIDE())
      return ImageUtil.createStubImage(myImageID, myAssumedWidth, myAssumedHeight);
    if (ourStubImage == null) {
      ourStubImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
    }
    return ourStubImage;
  }

  public int getImageID() {
    return myImageID;
  }

  protected static String getDefaultResource(int imageID) {
    return "com/almworks/rc/i" + imageID + ".png";
  }

  public static Collection<? extends ImageHandle> getAll() {
    return (Collection<? extends ImageHandle>) getAll(ImageHandle.class);
  }

  static void setStubImage(Image stubImage) {
    ourStubImage = stubImage;
  }

  public static ImageHandle image(int id, int assumedWidth, int assumedHeight) {
    return new ImageHandle(id, assumedWidth, assumedHeight, getDefaultResource(id), ImageHandle.class.getClassLoader());
  }

  public URL getURL() {
    if (myURL != null)
      return myURL;
    getImage();
    return myURL;
  }
}
