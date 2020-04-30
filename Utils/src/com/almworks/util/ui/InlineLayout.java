package com.almworks.util.ui;

import com.almworks.util.LogHelper;
import com.almworks.util.collections.Convertor;
import org.almworks.util.Failure;

import java.awt.*;

/**
 * @author : Dyoma
 */
public class InlineLayout implements LayoutManager2 {
  private final Orientation myOrientation;
  private final int myGap;
  private final boolean myStretchAcross;
  private final int myMinAcross;
  private final int myMaxAcross;
  private boolean myLastTakesAllSpace;
  /**
   * If set to true - accounts invisible components when calculating pref size. Thus pref size does not depend on children visibility.
   */
  private boolean myCalcInvisible = false;

  public InlineLayout(Orientation orientation) {
    this(orientation, 0, true);
  }

  public InlineLayout(Orientation orientation, int gap) {
    this(orientation, gap, true);
  }

  /**
   * @param stretchAcross if true, vertical size of components will be always set to the size of the container (for horizontal layout)
   */
  public InlineLayout(Orientation orientation, int gap, boolean stretchAcross) {
    this(orientation, gap, stretchAcross, -1, -1);
  }

  /**
   * @param minAcross if positive specifies minimal across size
   * @param maxAcross if positive specifies maximum across size
   */
  public InlineLayout(Orientation orientation, int gap, boolean stretchAcross, int minAcross, int maxAcross) {
    assert orientation != null;
    assert gap >= 0 : gap;
    myOrientation = orientation;
    myGap = gap;
    myStretchAcross = stretchAcross;
    if (minAcross > maxAcross && maxAcross > 0) {
      LogHelper.error("Min across corrected", minAcross, maxAcross);
      minAcross = maxAcross;
    }
    myMinAcross = minAcross;
    myMaxAcross = maxAcross;
  }

  public InlineLayout setLastTakesAllSpace(boolean lastTakesAllSpace) {
    myLastTakesAllSpace = lastTakesAllSpace;
    return this;
  }

  public void setCalcInvisible(boolean calcInvisible) {
    myCalcInvisible = calcInvisible;
  }

  public static InlineLayout horizontal(int gap) {
    return new InlineLayout(HORISONTAL, gap);
  }

  public static InlineLayout vertical(int gap) {
    return new InlineLayout(VERTICAL, gap);
  }

  public Dimension maximumLayoutSize(Container target) {
    return calculateSizeType(MAX_SIZE, target);
  }

  public Dimension minimumLayoutSize(Container parent) {
    return calculateSizeType(MIN_SIZE, parent);
  }

  public Dimension preferredLayoutSize(Container parent) {
    return calculateSizeType(PREF_SIZE, parent);
  }

  public void layoutContainer(Container parent) {
    int prefAlong = myOrientation.getAlong(preferredLayoutSize(parent));
    int totalAlong = myOrientation.getAlong(parent.getSize());
    // layout type: 0 = ratio-based; 1 = maxSize-based; 2 = minSize-based
    final int layoutType;
    if (totalAlong < prefAlong) {
      int minAlong = myOrientation.getAlong(minimumLayoutSize(parent));
      layoutType = totalAlong < minAlong ? 1 : 0;
    } else
      layoutType = 0;
    float ratio = 1;
    if (layoutType == 0 && prefAlong > 0 && prefAlong > totalAlong)
      ratio = ((float) totalAlong) / prefAlong;
    Component[] components = parent.getComponents();
    Insets insets = parent.getInsets();
    Dimension insetOffset = new Dimension(insets.left, insets.top);
    Dimension insetDecrease = new Dimension(insets.left + insets.right, insets.top + insets.bottom);
    int alongPos = myOrientation.getAlong(insetOffset);
    int stretchAcrossPos = myOrientation.getAcross(insetOffset);
    int stretchAcrossSize = myOrientation.getAcross(parent.getSize()) - myOrientation.getAcross(insetDecrease);
    boolean firstLaid = false;
    for (int i = 0; i < components.length; i++) {
      Component component = components[i];
      if (!component.isVisible())
        continue;

      if (firstLaid)
        alongPos += myGap;
      else
        firstLaid = true;

      int alongSize;
      if (myLastTakesAllSpace && i == components.length - 1) {
        alongSize = totalAlong - myOrientation.getAlong(new Dimension(insets.right, insets.bottom)) - alongPos;
      } else {
        switch (layoutType) {
        case 1:
          alongSize = myOrientation.getAlong(MIN_SIZE.convert(component));
          break;
        case 0:
        default:
          alongSize = (int) (myOrientation.getAlong(component.getPreferredSize()) * ratio);
        }
      }

      int acrossSize;
      int acrossPos;
      if (myStretchAcross) {
        acrossPos = stretchAcrossPos;
        acrossSize = stretchAcrossSize;
      } else {
        acrossSize = Math.min(stretchAcrossSize, myOrientation.getAcross(PREF_SIZE.convert(component)));
        acrossPos = stretchAcrossPos + ((stretchAcrossSize - acrossSize) >> 1);
      }

      component.setBounds(myOrientation.createRectange(alongPos, acrossPos, alongSize, acrossSize));
      alongPos += alongSize;
    }
  }

  private Dimension calculateSizeType(Convertor<Component, Dimension> sizeType, Container target) {
    Component[] components = target.getComponents();
    int along = 0;
    int across = 0;
    for (int i = 0; i < components.length; i++) {
      Component component = components[i];
      if (!myCalcInvisible && !component.isVisible())
        continue;
      Dimension size = sizeType.convert(component);
      along = addInt(along, myOrientation.getAlong(size));
      if (i > 0)
        along = addInt(along, myGap);
      across = Math.max(across, myOrientation.getAcross(size));
    }
    if (myMinAcross > 0) across = Math.max(myMinAcross, across);
    if (myMaxAcross > 0) across = Math.min(myMaxAcross, across);
    Dimension dimension = myOrientation.createDimension(along, across);
    Insets insets = target.getInsets();
    return new Dimension(dimension.width + insets.left + insets.right, dimension.height + insets.top + insets.bottom);
  }

  private int addInt(int i1, int i2) {
    if (i2 > 0)
      return Integer.MAX_VALUE - i2 > i1 ? i1 + i2 : Integer.MAX_VALUE;
    else
      return Integer.MIN_VALUE - i2 < i1 ? i1 + i2 : Integer.MIN_VALUE;
  }

  public float getLayoutAlignmentX(Container target) {
    return 0.5f;
  }

  public float getLayoutAlignmentY(Container target) {
    return 0.5f;
  }

  public void addLayoutComponent(Component comp, Object constraints) {
  }

  public void removeLayoutComponent(Component comp) {
  }

  public void addLayoutComponent(String name, Component comp) {
  }

  public void invalidateLayout(Container target) {
  }

  public static abstract class Orientation {
    public abstract int getAlong(Dimension dimension);

    public abstract int getAcross(Dimension dimension);

    public abstract Dimension createDimension(int along, int across);

    public abstract Rectangle createRectange(int along, int across, int alongSize, int acrossSize);

    public abstract int getIndex();

    public abstract Orientation getOpposite();

    public abstract boolean isVertical();

    public abstract void setAlong(Dimension dimension, int value);

    public static Orientation forBorderLayout(String borderConstraint) {
      if (BorderLayout.NORTH.equals(borderConstraint) || BorderLayout.SOUTH.equals(borderConstraint))
        return HORISONTAL;
      else if (BorderLayout.EAST.equals(borderConstraint) || BorderLayout.WEST.equals(borderConstraint))
        return VERTICAL;
      else
        throw new Failure("Wrong constraint: " + borderConstraint);
    }

    public abstract int getAcrossSize(Rectangle rectangle);

    public abstract void setAcrossSize(Rectangle rectangle, int value);

    public abstract int getAcrossPosition(Rectangle rectangle);

    public abstract void setAcrossPosition(Rectangle rectangle, int value);
  }


  public static final Orientation HORISONTAL = new Orientation() {
    public int getAlong(Dimension dimension) {
      return dimension.width;
    }

    @Override
    public void setAlong(Dimension dimension, int value) {
      dimension.width = value;
    }

    public int getAcross(Dimension dimension) {
      return dimension.height;
    }

    @Override
    public int getAcrossSize(Rectangle rectangle) {
      return rectangle.height;
    }

    @Override
    public void setAcrossSize(Rectangle rectangle, int value) {
      rectangle.height = value;
    }

    @Override
    public int getAcrossPosition(Rectangle rectangle) {
      return rectangle.y;
    }

    @Override
    public void setAcrossPosition(Rectangle rectangle, int value) {
      rectangle.y = value;
    }

    public Dimension createDimension(int along, int across) {
      return new Dimension(along, across);
    }

    public Rectangle createRectange(int along, int across, int alongSize, int acrossSize) {
      return new Rectangle(along, across, alongSize, acrossSize);
    }

    public int getIndex() {
      return 0;
    }

    public Orientation getOpposite() {
      return VERTICAL;
    }

    @Override
    public boolean isVertical() {
      return false;
    }
  };

  public static final Orientation VERTICAL = new Orientation() {
    public int getAlong(Dimension dimension) {
      return dimension.height;
    }

    @Override
    public void setAlong(Dimension dimension, int value) {
      dimension.height = value;
    }

    public int getAcross(Dimension dimension) {
      return dimension.width;
    }

    @Override
    public int getAcrossSize(Rectangle rectangle) {
      return rectangle.width;
    }

    @Override
    public void setAcrossSize(Rectangle rectangle, int value) {
      rectangle.width = value;
    }

    @Override
    public int getAcrossPosition(Rectangle rectangle) {
      return rectangle.x;
    }

    @Override
    public void setAcrossPosition(Rectangle rectangle, int value) {
      rectangle.x = value;
    }

    public Dimension createDimension(int along, int across) {
      return new Dimension(across, along);
    }

    public Rectangle createRectange(int along, int across, int alongSize, int acrossSize) {
      return new Rectangle(across, along, acrossSize, alongSize);
    }

    public int getIndex() {
      return 1;
    }

    public Orientation getOpposite() {
      return HORISONTAL;
    }

    @Override
    public boolean isVertical() {
      return true;
    }
  };

  public static final Convertor<Component, Dimension> MAX_SIZE = new Convertor<Component, Dimension>() {
    public Dimension convert(Component value) {
      return value.getMaximumSize();
    }
  };

  public static final Convertor<Component, Dimension> PREF_SIZE = new Convertor<Component, Dimension>() {
    public Dimension convert(Component value) {
      return value.getPreferredSize();
    }
  };

  public static final Convertor<Component, Dimension> MIN_SIZE = new Convertor<Component, Dimension>() {
    public Dimension convert(Component value) {
      return value.getMinimumSize();
    }
  };
}
