package com.almworks.util.components.layout;

import com.almworks.util.Pair;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class ContainerArea {
  private final ContainerAccessor myAccessor;

  public ContainerArea(ContainerAccessor accessor) {
    myAccessor = accessor;
  }

  public int getWidth() {
    return myAccessor.getWidth();
  }

  public int getHeight() {
    return myAccessor.getHeight();
  }

  public Dimension getSize() {
    return myAccessor.getSize();
  }

  public void placeChild(Component child, int x, int y, int width, int height) {
    myAccessor.placeChild(child, x, y, width, height);
  }

  public JComponent getComponent() {
    return myAccessor.getComponent();
  }

  public void placeChild(Component child, Rectangle rect) {
    placeChild(child, rect.x, rect.y, rect.width, rect.height);
  }

  public void placeChild(Component child, int x, int y, Dimension size) {
    placeChild(child, x, y, size.width, size.height);
  }

  public Dimension placeChildPrefSize(Component child, int x, int y) {
    if (!child.isVisible())
      return new Dimension();
    Dimension size = child.getPreferredSize();
    placeChild(child, x, y, size);
    return size;
  }

  public Dimension placeLeftTopChild(JComponent child) {
    Dimension size = child.getPreferredSize();
    placeChild(child, 0, 0, size);
    return size;
  }

  public Dimension placeRightTopChild(JComponent child) {
    Dimension size = child.getPreferredSize();
    placeChild(child, getWidth() - size.width, 0, size);
    return size;
  }

  public Dimension placeRightChild(JComponent child, int y) {
    Dimension size = child.getPreferredSize();
    placeChild(child, getWidth() - size.width, y, size);
    return size;
  }

  public int fillUptoRightVCentering(WidthDrivenComponent slave, int x, int y, int centerHeight) {
    if (!slave.isVisibleComponent())
      return 0;
    int width = spaceToRight(x);
    int height = slave.getPreferredHeight(width);
    if (height < centerHeight)
      y += (centerHeight - height) / 2;
    subRectangle(x, y, width, height).fillWhole(slave.getComponent());
    return height;
  }

  public int fillUptoRight(Component child, int x, int y) {
    if (!child.isVisible())
      return 0;
    int height = child.getPreferredSize().height;
    int width = spaceToRight(x);
    placeChild(child, x, y, width, height);
    return height;
  }

  private int spaceToRight(int x) {
    return Math.max(0, getWidth() - x);
  }

  public void fillWidthFromTop(JComponent child, int bottomLine) {
    placeChild(child, 0, 0, getWidth(), bottomLine);
  }

  public int fillWidth(JComponent child, int y) {
    if (!child.isVisible())
      return 0;
    Dimension size = child.getPreferredSize();
    placeChild(child, 0, y, getWidth(), size.height);
    return size.height;
  }

  public void fillWidthFromBottom(JComponent child, int yTop) {
    placeChild(child, 0, yTop, getWidth(), getHeight() - yTop);
  }

  public int fillWidthFromBottom(JComponent child) {
    int height = child.getPreferredSize().height;
    placeChild(child, 0, getHeight() - height, getWidth(), height);
    return height;
  }

  public ContainerArea subRectangle(int x, int y, int width, int height) {
    return new ContainerArea(new SubRectangle(this, x, y, width, height));
  }

  public ContainerArea subRectangle(Rectangle bounds) {
    return subRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
  }

  public Pair<ContainerArea, ContainerArea> verticalProportionalSplit(float proportion, int gap) {
    assert proportion > 0 : proportion;
    assert proportion < 1 : proportion;
    int width = getWidth();
    int left = Math.max(0, (int) ((width - gap) * proportion));
    return verticalSplit(left, gap);
  }

  public Pair<ContainerArea, ContainerArea> verticalSplit(int leftWidth, int gap) {
    int width = getWidth();
    leftWidth = Math.min(leftWidth, width);
    int right = Math.max(0, width - gap - leftWidth);
    int height = getHeight();
    return Pair.create(subRectangle(0, 0, leftWidth, height), subRectangle(width - right, 0, right, height));
  }

  public void fillWhole(JComponent component) {
    placeChild(component, 0, 0, getWidth(), getHeight());
  }

  public interface ContainerAccessor {
    int getWidth();

    int getHeight();

    Dimension getSize();

    void placeChild(Component child, int x, int y, int width, int height);

    JComponent getComponent();
  }

  private static class SubRectangle implements ContainerAccessor {
    private final ContainerArea myOutter;
    private final int myX;
    private final int myY;
    private final int myWidth;
    private final int myHeight;

    public SubRectangle(ContainerArea outter, int x, int y, int width, int height) {
      myOutter = outter;
      myX = x;
      myY = y;
      myWidth = width;
      myHeight = height;
    }

    public int getWidth() {
      return myWidth;
    }

    public int getHeight() {
      return myHeight;
    }

    public Dimension getSize() {
      return new Dimension(myWidth, myHeight);
    }

    public void placeChild(Component child, int x, int y, int width, int height) {
      width = clipSize(x, width, myWidth);
      height = clipSize(y, height, myHeight);
      myOutter.placeChild(child, x + myX, y + myY, width, height);
    }

    private int clipSize(int position, int size, int bound) {
      return Math.max(0, Math.min(size, bound - position));
    }

    public JComponent getComponent() {
      return myOutter.getComponent();
    }
  }
}
