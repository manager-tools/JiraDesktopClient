package com.almworks.screenshot.editor.tools.blur;

import com.almworks.screenshot.editor.image.WorkingImage;
import com.almworks.screenshot.editor.shapes.AbstractShape;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;

/**
 * @author Stalex
 */
public class BluredRegion extends AbstractShape {
  private final int[] WEIGHTS = {2, 3, 5, 8, 13, 21, 34, 55, 89, 144};
  private WorkingImage myWorkingImage;
  private BufferedImage myImage;
  private int myWeight;
  private int myTransormedWeight;

  public int getWeight() {
    return myWeight;
  }

  public void setWeight(int myWeight) {
    if (this.myWeight != myWeight) {
      this.myWeight = myWeight;
      myTransormedWeight = WEIGHTS[myWeight - 1];
      /*int maxDim = Math.max(myRect.width, myRect.height);
      double y = Math.pow(maxDim, 1.0f / myMaxWeight);
      myTransormedWeight = (int) Math.round(Math.pow(y, myWeight));*/
      invalidateImage();
    }
  }

  public BluredRegion(Rectangle rect, WorkingImage image, int weight) {
    super(image.getLayerControl());

    setRect(rect);
    myImage = null;
    myWorkingImage = image;
    setWeight(weight);
  }

  public void invalidateImage() {
    myImage = null;
  }

  @Override
  public Object clone() {
    invalidateImage();
    return super.clone();
  }

  public void paintShape(Graphics2D g2, Area clip) {
    Rectangle rect = getRect();
    if (myImage == null) {
      Filter filter = new PixelizeFilter();
      myImage = filter.filter(myWorkingImage.getInitialSubImage(rect), rect, myTransormedWeight);
    }
    g2.drawImage(myImage, rect.x, rect.y, null);
  } 

}