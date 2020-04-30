package com.almworks.util.components.layout;

import com.almworks.util.ui.InlineLayout;
import org.almworks.util.Collections15;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

/**
 * @author dyoma
 */
public class ComponentLine {
  private final InlineLayout.Orientation myOrientation;
  private final List<Component> myComponents = Collections15.arrayList();
  private final List<Dimension> mySizes = Collections15.arrayList();

  private ComponentLine(InlineLayout.Orientation orientation) {
    myOrientation = orientation;
  }

  public void addComponent(Component component, Dimension size) {
    myComponents.add(component);
    mySizes.add(size);
  }

  public final Dimension addWithPrefSize(Component component) {
    Dimension size = component.getPreferredSize();
    addComponent(component, size);
    return size;
  }

  public int getAlong() {
    int sum = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      Dimension size = mySizes.get(i);
      if (!isVisible(i))
        continue;
      sum += myOrientation.getAlong(size);
    }
    return sum;
  }

  public int getAcross() {
    int max = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      if (!isVisible(i))
        continue;
      Dimension size = mySizes.get(i);
      max = Math.max(max, myOrientation.getAcross(size));
    }
    return max;
  }

  public boolean isVisible(int componentIndex) {
    return myComponents.get(componentIndex).isVisible();
  }

  public int getMaxAlong() {
    int max = 0;
    for (int i = 0; i < getComponentCount(); i++) {
      if (isVisible(i))
        max = Math.max(max, myOrientation.getAlong(mySizes.get(i)));
    }
    return max;
  }

  public int getComponentCount() {
    assert mySizes.size() == myComponents.size();
    return mySizes.size();
  }

  public int getAlong(int index) {
    return isVisible(index) ? myOrientation.getAlong(mySizes.get(index)) : 0;
  }

  public Component getComponent(int index) {
    return myComponents.get(index);
  }

  public static ComponentLine horizontal() {
    return new ComponentLine(InlineLayout.HORISONTAL);
  }

  public static ComponentLine vertical() {
    return new ComponentLine(InlineLayout.VERTICAL);
  }

  public void addAllPrefSize(List<? extends Component> compoents) {
    for (Iterator<? extends Component> iterator = compoents.iterator(); iterator.hasNext();) {
      Component component = iterator.next();
      addWithPrefSize(component);
    }
  }

  public void removeAt(int row) {
    myComponents.remove(row);
    mySizes.remove(row);
  }
}
