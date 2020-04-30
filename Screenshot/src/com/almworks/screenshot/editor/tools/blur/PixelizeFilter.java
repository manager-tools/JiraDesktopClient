package com.almworks.screenshot.editor.tools.blur;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Stalex
 */
public class PixelizeFilter implements Filter {

  public BufferedImage filter(BufferedImage srcImage, Rectangle rect, int weight) {
    int height = rect.height;
    int width = rect.width;

    BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    for (int i = 0; i < width / weight; i++) {
      for (int j = 0; j < height / weight; j++) {
        fillWithAverange(srcImage, dstImage, weight, weight, i * weight, j * weight);
      }
    }

    int hRest = height % weight;
    int wRest = width % weight;
    if (hRest != 0) {
      for (int i = 0; i < width / weight; i++) {
        fillWithAverange(srcImage, dstImage, weight, hRest, i * weight, height - hRest);
      }
    }

    if (wRest != 0) {
      for (int i = 0; i < height / weight; i++) {
        fillWithAverange(srcImage, dstImage, wRest, weight, width - wRest, i * weight);
      }
    }

    if (wRest != 0 && hRest != 0) {
      fillWithAverange(srcImage, dstImage, wRest, hRest, width - wRest, height - hRest);
    }

    return dstImage;

  }

  private void fillWithAverange(BufferedImage srcImage, BufferedImage dstImage, int weight, int heightWeight, int startX, int startY) {

    int rgbSum[] = new int[3];

    for (int k = startX; k < startX + weight; k++) {
      for (int n = startY; n < startY + heightWeight; n++) {

        int rgb = srcImage.getRGB(k, n);
        //srcImage.get
        rgbSum[0] += (rgb >> 16) & 0xFF;
        rgbSum[1] += (rgb >> 8) & 0xFF;
        rgbSum[2] += rgb & 0xFF;

      }
    }

    int num = weight * heightWeight;
    rgbSum[0] /= num;
    rgbSum[1] /= num;
    rgbSum[2] /= num;

    Color c = new Color(rgbSum[0], rgbSum[1], rgbSum[2]);
    int res = c.getRGB();

    for (int k = startX; k < startX + weight; k++) {
      for (int n = startY; n < startY + heightWeight; n++) {
        dstImage.setRGB(k, n, res);
      }
    }
  }
}
