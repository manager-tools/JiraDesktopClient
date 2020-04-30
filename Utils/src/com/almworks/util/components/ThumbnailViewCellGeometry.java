package com.almworks.util.components;

public class ThumbnailViewCellGeometry {
  public static final ThumbnailViewCellGeometry NO_GEOMETRY = new ThumbnailViewCellGeometry();

  private static final int DEFAULT_CELL_WIDTH = 100;
  private static final int DEFAULT_IMAGE_HEIGHT = 40;
  private static final int DEFAULT_IMAGE_TEXT_GAP = 5;
  private static final int DEFAULT_IMAGE_TOP_MARGIN = 5;
  private static final int DEFAULT_IMAGE_WIDTH = 80;
  private static final int DEFAULT_TEXT_BOTTOM_MARGIN = 5;
  private static final int DEFAULT_TEXT_SIDE_MARGIN = 5;

  private int myCellWidth = DEFAULT_CELL_WIDTH;
  private int myImageHeight = DEFAULT_IMAGE_HEIGHT;
  private int myImageTextGap = DEFAULT_IMAGE_TEXT_GAP;
  private int myImageTopMargin = DEFAULT_IMAGE_TOP_MARGIN;
  private int myImageWidth = DEFAULT_IMAGE_WIDTH;
  private int myTextBottomMargin = DEFAULT_TEXT_BOTTOM_MARGIN;
  private int myTextSideMargin = DEFAULT_TEXT_SIDE_MARGIN;

  public int getCellWidth() {
    return myCellWidth;
  }

  public int getImageHeight() {
    return myImageHeight;
  }

  public int getImageTextGap() {
    return myImageTextGap;
  }

  public int getImageTopMargin() {
    return myImageTopMargin;
  }

  public int getImageWidth() {
    return myImageWidth;
  }

  public int getTextBottomMargin() {
    return myTextBottomMargin;
  }

  public int getTextSideMargin() {
    return myTextSideMargin;
  }

  public void setCellWidth(int cellWidth) {
    myCellWidth = cellWidth;
  }

  public void setImageHeight(int imageHeight) {
    myImageHeight = imageHeight;
  }

  public void setImageTextGap(int imageTextGap) {
    myImageTextGap = imageTextGap;
  }

  public void setImageTopMargin(int imageTopMargin) {
    myImageTopMargin = imageTopMargin;
  }

  public void setImageWidth(int imageWidth) {
    myImageWidth = imageWidth;
  }

  public void setTextBottomMargin(int textBottomMargin) {
    myTextBottomMargin = textBottomMargin;
  }

  public void setTextSideMargin(int textSideMargin) {
    myTextSideMargin = textSideMargin;
  }
}
