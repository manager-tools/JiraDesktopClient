package com.almworks.util.ui.actions.dnd;

import com.almworks.util.commons.Factory;
import com.almworks.util.ui.swing.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public class DragImageComponent extends JComponent {
  private final Image myImage;
  private final Factory<Image> myImageFactory;

  public DragImageComponent(Factory<Image> imageFactory, Image image) {
    myImageFactory = imageFactory;
    myImage = image;
    setCursor(null);
  }

  public void paintComponent(Graphics g) {
    AwtUtil.applyRenderingHints(g);
    g.drawImage(myImage, 0, 0, this);
  }

  public Dimension getPreferredSize() {
    return new Dimension(myImage.getWidth(this), myImage.getHeight(this));
  }

  public Factory<Image> getImageFactory() {
    return myImageFactory;
  }

  public void setMousePoint(MouseEvent event, DragContext currentDrag) {
    Container parent = getParent();
    if (!(parent instanceof JLayeredPane)) {
      assert false : this;
      return;
    }
    Point mousePoint = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), parent);
    Dimension size = getPreferredSize();
    int dx;
    int dy;
    Point offset = currentDrag.getValue(DndUtil.DRAG_SOURCE_OFFSET);
    dx = offset != null ? offset.x : size.width / 2;
    dy = offset != null ? offset.y : size.height;
    setBounds(mousePoint.x - dx, mousePoint.y - dy, size.width, size.height);
  }

  public void setRelative(DragImageComponent preceding) {
    assert getParent() == preceding.getParent() : this + " " + preceding;
    Rectangle bounds = preceding.getBounds();
    Dimension size = getPreferredSize();
    setBounds(bounds.x + bounds.width, bounds.y, size.width, size.height);
  }
}
