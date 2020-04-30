package com.almworks.screenshot.editor.tools.blur;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SmudgeFilter implements Filter {

  public static final float SMUDGE_WEIGHT = 0.5F;
  private static final int DEFAULT_SMUDGE_SIZE = 30;

  public BufferedImage filter(BufferedImage srcImage, Rectangle rect, int weightK) {

    float weight = weightK / 10.0f;
    int smudgeSize;
    if (rect.height >= DEFAULT_SMUDGE_SIZE * 2) smudgeSize = DEFAULT_SMUDGE_SIZE;
    else smudgeSize = rect.height;
    int shift = smudgeSize / 2;
    int xbeg = Math.max(rect.x, shift) - shift;
    int dstWidth = rect.width + rect.x - xbeg;
    BufferedImage dstImage = new BufferedImage(dstWidth, rect.height, BufferedImage.TYPE_INT_ARGB);
    dstImage.getGraphics().drawImage(srcImage.getSubimage(xbeg, rect.y, dstWidth, rect.height), 0, 0, null);

    Color[][] smudgeInit = new Color[smudgeSize][smudgeSize];
    for (int xi = 0; xi < smudgeSize; xi++) {
      for (int yi = 0; yi < smudgeSize; yi++) {
        int rgb = dstImage.getRGB(xi, yi);
        smudgeInit[xi][yi] = new Color(rgb);
      }
    }
    for (int y = 0; y <= rect.height - smudgeSize; y = y + smudgeSize) {
      for (int x = shift; x < dstWidth; x++) {
        for (int xi = 0; xi < smudgeSize; xi++) {
          int xx = x + xi - shift;
          if (xx >= dstWidth) break;
          for (int yi = 0; yi < smudgeSize; yi++) {
            int yy = y + yi;
            int rgb = dstImage.getRGB(xx, yy);
            Color srcColor = new Color(rgb);
            int dstRed = Math.round(srcColor.getRed() * weight + smudgeInit[xi][yi].getRed() * (1 - weight));
            int dstBlue = Math.round(srcColor.getBlue() * weight + smudgeInit[xi][yi].getBlue() * (1 - weight));
            int dstGreen = Math.round(srcColor.getGreen() * weight + smudgeInit[xi][yi].getGreen() * (1 - weight));
            int initRed = Math.round(srcColor.getRed() * (1 - weight) + smudgeInit[xi][yi].getRed() * weight);
            int initBlue = Math.round(srcColor.getBlue() * (1 - weight) + smudgeInit[xi][yi].getBlue() * weight);
            int initGreen = Math.round(srcColor.getGreen() * (1 - weight) + smudgeInit[xi][yi].getGreen() * weight);
            Color dstColor = new Color(dstRed, dstGreen, dstBlue);
            Color initColor = new Color(initRed, initGreen, initBlue);
            //          if (xx >= rect.x)
            dstImage.setRGB(xx, yy, dstColor.getRGB());
            smudgeInit[xi][yi] = initColor;
          }
        }
      }
    }
    dstImage = dstImage.getSubimage((dstWidth - rect.width), 0, rect.width, rect.height);
    return dstImage;

  }

}
