package com.almworks.util.components;

import com.almworks.util.components.layout.ClientArea;
import com.almworks.util.components.layout.ComponentLine;
import com.almworks.util.components.layout.WidthDrivenComponent;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * @author dyoma
 */
public class FlowLayoutPanel extends JComponent implements WidthDrivenComponent {
  private final ClientArea myArea = new ClientArea(this);
  private final Map<Component, Integer> myOverridenGaps = Collections15.hashMap();
  private Dimension myGap = new Dimension();

  public void layout() {
    int y = 0;
    List<ComponentLine> lines = composeLines(getWidth());
    for (int i = 0; i < lines.size(); i++) {
      ComponentLine line = lines.get(i);
      int height = line.getAcross();
      int x = 0;
      for (int c = 0; c < line.getComponentCount(); c++) {
        Component comp = line.getComponent(c);
        int width = line.getAlong(c);
        myArea.placeChild(comp, x, y, width, height);
        x += width + getGapAfter(comp);
      }
      y += height + myGap.height;
    }
  }

  public int getPreferredHeight(int width) {
    List<ComponentLine> lines = composeLines(width);
    int sum = 0;
    int lineCount = lines.size();
    for (int i = 0; i < lineCount; i++)
      sum += lines.get(i).getAcross();
    if (lineCount > 1)
      sum += myGap.height * (lineCount - 1);
    return sum;
  }

  @NotNull
  public JComponent getComponent() {
    return this;
  }

  public boolean isVisibleComponent() {
    return getComponent().isVisible();
  }

  public int getPreferredWidth() {
    int maxWidth = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      Component component = getComponent(i);
      if (!component.isVisible())
        continue;
      maxWidth = Math.max(maxWidth, component.getPreferredSize().width);
    }
    return maxWidth;
  }

  private List<ComponentLine> composeLines(int maxWidth) {
    List<ComponentLine> result = Collections15.arrayList();
    ComponentLine currentLine = ComponentLine.horizontal();
    result.add(currentLine);
    int horizontalGaps = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      Component component = getComponent(i);
      if (!component.isVisible())
        continue;
      Dimension size = component.getPreferredSize();
      if (currentLine.getAlong() + size.width + horizontalGaps > maxWidth) {
        currentLine = ComponentLine.horizontal();
        result.add(currentLine);
        horizontalGaps = 0;
      }
      currentLine.addComponent(component, size);
      horizontalGaps += getGapAfter(component);
    }
    return result;
  }

  private int getGapAfter(Component component) {
    Integer overriden = myOverridenGaps.get(component);
    return overriden != null ? overriden.intValue() : myGap.width;
  }

  public void setGap(Dimension gap) {
    assert gap != null;
    myGap = gap;
  }

  public void overrideGapAfter(int pixels) {
    assert getComponentCount() > 0;
    Component component = getComponent(getComponentCount() - 1);
    myOverridenGaps.put(component, pixels);
  }
}
