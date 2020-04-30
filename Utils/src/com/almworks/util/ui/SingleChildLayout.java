package com.almworks.util.ui;

import com.almworks.util.Enumerable;
import com.almworks.util.collections.Convertor;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Util;

import javax.swing.*;
import java.awt.*;

/**
 * This layout supports only ONE component in container, which is
 * sized to its preferred size and placed according to alignment x
 * and alignment y.
 */
public class SingleChildLayout implements LayoutManager2 {
  public static final Policy CONTAINER = new Policy("CONTAINER");
  public static final Policy PREFERRED = new Policy("PREFERRED");

  public static final SingleChildLayout FILL_CONTAINER = new SingleChildLayout(CONTAINER);

  private static final Dimension ZERO_SIZE = new Dimension(0, 0);
  private static final JComponent EMPTY = new JPanel();
  static {
    EMPTY.setSize(0, 0);
  }

  private final Policy myHorizontalExcessPolicy;
  private final Policy myHorizontalLackPolicy;
  private final Policy myVerticalExcessPolicy;
  private final Policy myVerticalLackPolicy;

  public SingleChildLayout(Policy horizontalExcessPolicy, Policy verticalExcessPolicy, Policy horizontalLackPolicy,
    Policy verticalLackPolicy) {

    assert horizontalExcessPolicy != null;
    assert horizontalLackPolicy != null;
    assert verticalExcessPolicy != null;
    assert verticalLackPolicy != null;

    myHorizontalExcessPolicy = horizontalExcessPolicy;
    myHorizontalLackPolicy = horizontalLackPolicy;
    myVerticalExcessPolicy = verticalExcessPolicy;
    myVerticalLackPolicy = verticalLackPolicy;
  }

  public SingleChildLayout(Policy horizontalPolicy, Policy verticalPolicy) {
    this(horizontalPolicy, verticalPolicy, horizontalPolicy, verticalPolicy);
  }

  public SingleChildLayout(Policy policy) {
    this(policy, policy, policy, policy);
  }

  public SingleChildLayout() {
    this(PREFERRED, PREFERRED, PREFERRED, PREFERRED);
  }

  public static JPanel envelopNorth(JComponent component) {
    return envelop(component, CONTAINER, PREFERRED, CONTAINER, CONTAINER, 0.5F, 0F);
  }

  public static JPanel envelopCenter(JComponent component) {
    return envelop(component, PREFERRED, PREFERRED, CONTAINER, CONTAINER, 0.5F, 0.5F);
  }

  public static JPanel envelop(JComponent component, Policy horizontalExcessPolicy, Policy verticalExcessPolicy,
    Policy horizontalLackPolicy, Policy verticalLackPolicy, float alignmentX, float alignmentY) {

    assert component != null;
    if (alignmentX >= 0F)
      component.setAlignmentX(alignmentX);
    if (alignmentY >= 0F)
      component.setAlignmentY(alignmentY);
    JPanel result = new JPanel(new SingleChildLayout(horizontalExcessPolicy, verticalExcessPolicy,
      horizontalLackPolicy, verticalLackPolicy));
    result.setOpaque(false);
    result.add(component);
    return result;
  }

  public static JPanel envelop(JComponent component, float alignmentX, float alignmentY) {
    return envelop(component, PREFERRED, PREFERRED, PREFERRED, PREFERRED, alignmentX, alignmentY);
  }


  public void removeLayoutComponent(Component comp) {
  }

  public void layoutContainer(Container parent) {
    if (parent.getComponentCount() == 0)
      return;
    JComponent component = getComponent(parent);
    Dimension size = parent.getSize();
    Insets insets = parent.getInsets();
    int ah = insets.top + insets.bottom;
    int aw = insets.left + insets.right;
    Rectangle rect = new Rectangle(insets.left, insets.top, size.width - aw, size.height - ah);
    Dimension preferred = Util.NN(component.getPreferredSize(), ZERO_SIZE);
    int x, width, y, height;
    Policy horPolicy = rect.width >= preferred.width ? myHorizontalExcessPolicy : myHorizontalLackPolicy;
    if (horPolicy == CONTAINER) {
      x = rect.x;
      width = rect.width;
    } else if (horPolicy == PREFERRED) {
      x = rect.x + Math.round(component.getAlignmentX() * (rect.width - preferred.width));
      width = preferred.width;
    } else {
      assert false : horPolicy;
      x = rect.x;
      width = rect.width;
    }
    Policy verPolicy = rect.height >= preferred.height ? myVerticalExcessPolicy : myVerticalLackPolicy;
    if (verPolicy == CONTAINER) {
      y = rect.y;
      height = rect.height;
    } else if (verPolicy == PREFERRED) {
      y = rect.y + Math.round(component.getAlignmentY() * (rect.height - preferred.height));
      height = preferred.height;
    } else {
      assert false : verPolicy;
      y = rect.y;
      height = rect.height;
    }
    component.setBounds(x, y, width, height);
  }

  public void addLayoutComponent(String name, Component comp) {
  }

  private Dimension calcLayoutSize(Container parent, Convertor<Component, Dimension> sizeType) {
    assert parent.getComponentCount() > 0 : parent;
    Insets insets = parent.getInsets();
    Dimension size = sizeType.convert(getComponent(parent));
    return AwtUtil.addInsets(size, insets);
  }

  public Dimension minimumLayoutSize(Container parent) {
    if (parent.getComponentCount() == 0)
      return AwtUtil.addInsets(new Dimension(), parent.getInsets());
    return calcLayoutSize(parent, InlineLayout.MIN_SIZE);
  }

  public Dimension preferredLayoutSize(Container parent) {
    if (parent.getComponentCount() == 0)
      return AwtUtil.addInsets(new Dimension(), parent.getInsets());
    return calcLayoutSize(parent, InlineLayout.PREF_SIZE);
//    if (parent.getComponentCount() == 0)
//      return UIUtil.addInsets(new Dimension(), parent.getInsets());
//    Dimension size = getComponent(parent).getPreferredSize();
//    Insets insets = parent.getInsets();
//    int aw = insets.left + insets.right;
//    int ah = insets.top + insets.bottom;
//    if (aw > 0 || ah > 0)
//      return new Dimension(size.width + aw, size.height + ah);
//    else
//      return size;
  }

  public float getLayoutAlignmentX(Container target) {
    return 0.5F;
  }

  public float getLayoutAlignmentY(Container target) {
    return 0.5F;
  }

  public void invalidateLayout(Container target) {
  }

  public Dimension maximumLayoutSize(Container target) {
    if (target.getComponentCount() == 0)
      return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    return calcLayoutSize(target, InlineLayout.MAX_SIZE);
  }

  public void addLayoutComponent(Component comp, Object constraints) {
  }

  private JComponent getComponent(Container target) {
    int count = target.getComponentCount();
    if (count > 1)
      throw new IllegalStateException(this + " supports only one component in container");
    Component component = target.getComponent(0);
    if (!(component instanceof JComponent))
      throw new IllegalStateException(this + " supports only JComponents");
    return count == 0 ? EMPTY : (JComponent) component;
  }

  public static JPanel envelop(JComponent component) {
    return envelop(component, 0.5F, 0.5F);
  }

  public static JPanel envelop(JComponent component, Policy policy) {
    return envelop(component, policy, policy, policy, policy, 0.5F, 0.5F);
  }

  public static final class Policy extends Enumerable {
    private Policy(String name) {
      super(name);
    }
  }
}
