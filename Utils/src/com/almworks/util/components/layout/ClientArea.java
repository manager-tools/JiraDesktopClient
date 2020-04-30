package com.almworks.util.components.layout;

import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author dyoma
 */
public class ClientArea extends ContainerArea implements ContainerArea.ContainerAccessor {
  public ClientArea(JComponent component) {
    super(new MyContainerAccessor(component));
  }

  public Dimension toWhole(Dimension result) {
    Insets insets = getComponent().getInsets();
    result.width += AwtUtil.getInsetWidth(insets);
    result.height += AwtUtil.getInsetHeight(insets);
    return result;
  }

  public Dimension toWhole(int width, int height) {
    return toWhole(new Dimension(width, height));
  }

  public int widthToWhole(int width) {
    return width + getInsetsWidth();
  }

  public int heightToWhole(int height) {
    return height + getInsetsHeight();
  }

  public int getInsetsHeight() {
    return AwtUtil.getInsetHeight(getComponent());
  }

  public int getInsetsWidth() {
    return AwtUtil.getInsetWidth(getComponent());
  }

  private static class MyContainerAccessor implements ContainerAccessor {
    private final JComponent myComponent;

    public MyContainerAccessor(JComponent component) {
      myComponent = component;
    }

    public int getWidth() {
      return myComponent.getWidth() - AwtUtil.getInsetWidth(myComponent);
    }

    public int getHeight() {
      return myComponent.getHeight() - AwtUtil.getInsetHeight(myComponent);
    }

    public Dimension getSize() {
      Dimension size = myComponent.getSize();
      Insets insets = myComponent.getInsets();
      size.width -= AwtUtil.getInsetWidth(insets);
      size.height -= AwtUtil.getInsetHeight(insets);
      return size;
    }

    public void placeChild(Component child, int x, int y, int width, int height) {
      width = Math.max(width, 0);
      height = Math.max(height, 0);
      Insets insets = myComponent.getInsets();
      child.setBounds(insets.left + x, insets.top + y, width, height);
    }

    public JComponent getComponent() {
      return myComponent;
    }
  }
}
