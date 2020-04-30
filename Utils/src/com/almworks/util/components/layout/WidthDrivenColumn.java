package com.almworks.util.components.layout;


import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public class WidthDrivenColumn extends JComponent implements Scrollable, WidthDrivenComponent {
  private final List<WidthDrivenComponent> myComponents = Collections15.arrayList();
  private final ClientArea myArea = new ClientArea(this);
  private int myVericalGap = 0;
  private boolean myLastFillsAll = true;

  public int getPreferredWidth() {
    return getMaxPrefWidth(myComponents) + AwtUtil.getInsetWidth(this);
  }

  public int getPreferredHeight(int width) {
    return getSumPreferedHeight(width, myComponents, myVericalGap);
  }

  @NotNull
  public JComponent getComponent() {
    return this;
  }

  public boolean isVisibleComponent() {
    if (!isVisible())
      return false;
    for (WidthDrivenComponent component : myComponents)
      if (component.isVisibleComponent())
        return true;
    return false;
  }

  public void addComponent(WidthDrivenComponent component) {
    myComponents.add(component);
    add(component.getComponent());
  }

  public void removeAllComponents() {
    myComponents.clear();
    removeAll();
  }

  public void addRegularComponent(JComponent component) {
    addComponent(new WidthDrivenComponentAdapter(component));
  }

  public int getVericalGap() {
    return myVericalGap;
  }

  public void setVericalGap(int vericalGap) {
    myVericalGap = vericalGap;
  }


  public boolean isLastFillsAll() {
    return myLastFillsAll;
  }

  public void setLastFillsAll(boolean lastFillsAll) {
    myLastFillsAll = lastFillsAll;
    invalidate();
  }

  public Dimension getPreferredSize() {
    return calcPreferredSize(true);
  }

  private Dimension calcPreferredSize(boolean useActualWidth) {
    int width = getWidthForPreferredSize(useActualWidth);
    int height = getSumPreferedHeight(width, myComponents, myVericalGap);
    return myArea.toWhole(width, height);
  }
  
  private int getWidthForPreferredSize(boolean useActualWidth) {
    if (!useActualWidth) return getMaxPrefWidth(myComponents);
    int viewportWidth = getViewportWidth();
    if (viewportWidth <= 0) return getMaxPrefWidth(myComponents);
    viewportWidth -= AwtUtil.getInsetWidth(this);
    if (viewportWidth <= 0) return getMaxPrefWidth(myComponents);
    return viewportWidth;
  }
  
  public Dimension getPreferredScrollableViewportSize() {
    return calcPreferredSize(false);
  }

  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return ScrollablePanel.getStdUnitIncrement(orientation, visibleRect);
  }

  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return ScrollablePanel.getStdBlockIncrement(orientation, visibleRect);
  }

  public void layout() {
    int width = myArea.getWidth();
    int y = 0;
    List<WidthDrivenComponent> visibleComponents = Collections15.arrayList(getComponentCount());
    for (WidthDrivenComponent component : myComponents) {
      if (component.isVisibleComponent())
        visibleComponents.add(component);
    }
    if (visibleComponents.size() == 0)
      return;
    for (int i = 0; i < visibleComponents.size() - 1; i++) {
      WidthDrivenComponent component = visibleComponents.get(i);
      int height = component.getPreferredHeight(width);
      myArea.subRectangle(0, y, width, height).fillWhole(component.getComponent());
      y += height + myVericalGap;
    }
    WidthDrivenComponent last = visibleComponents.get(visibleComponents.size() - 1);
    ContainerArea lastArea;
    if (myLastFillsAll)
      lastArea = myArea.subRectangle(0, y, width, myArea.getHeight() - y);
    else
      lastArea = myArea.subRectangle(0, y, width, last.getPreferredHeight(width));
    lastArea.fillWhole(last.getComponent());
  }

  public static int getMaxPrefWidth(Collection<? extends WidthDrivenComponent> components) {
    int maxPrefWidth = 0;
    for (WidthDrivenComponent component : components) {
      if (!component.isVisibleComponent())
        continue;
      maxPrefWidth = Math.max(maxPrefWidth, component.getPreferredWidth());
    }
    return maxPrefWidth;
  }

  public static int getVisiblePrefferredWidth(WidthDrivenComponent component) {
    return component.isVisibleComponent() ? component.getPreferredWidth() : 0;
  }

  public static int getVisibleHeight(WidthDrivenComponent component, int width) {
    return component.isVisibleComponent() ? component.getPreferredHeight(width) : 0;
  }

  public static int getSumPreferedHeight(int width, Collection<? extends WidthDrivenComponent> components, int vericalGap) {
    int height = 0;
    int visibleCounter = 0;
    for (WidthDrivenComponent component : components) {
      if (!component.isVisibleComponent())
        continue;
      height += component.getPreferredHeight(width);
      visibleCounter++;
    }
    if (visibleCounter > 1)
      height += (visibleCounter - 1) * vericalGap;
    return height;
  }

  private int getViewportWidth() {
    Container parent = getParent();
    return parent instanceof JViewport ? parent.getWidth() - AwtUtil.getInsetWidth((JComponent) parent) : -1;
  }
}
