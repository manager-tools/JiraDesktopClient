package com.almworks.util.ui.swing;

import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.threads.Threads;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Set;

public class AwtUtil {
  public static final Rectangle[] EMPTY_RECTANGLES = new Rectangle[0];
  private static Color ourPanelBackground;
  public static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
  public static final Border EMPTY_BORDER = new EmptyBorder(0, 0, 0, 0);
  private static final Rectangle TEMP_RECT = new Rectangle();
  private static int[] FOCUS_TRAVERSAL_KEY_IDS = {
    KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
    KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS};

  public static Dimension addInsets(Dimension dimension, Insets insets) {
    dimension.width += insets.left + insets.right;
    dimension.height += insets.top + insets.bottom;
    return dimension;
  }

  @NotNull
  public static Color getPanelBackground() {
    Color result = ourPanelBackground;
    if (result == null) {
      result = UIManager.getColor("Panel.background");
      if (result == null) {
        Log.warn("panel background is null");
        result = Color.GRAY;
      }
      ourPanelBackground = result;
    }
    return result;
  }

  public static Color getEditorPaneBackground() {
    return UIManager.getColor("EditorPane.background");
  }

  public static Color getTextComponentForeground() {
    Color result = UIManager.getColor("TextField.foreground");
    return result != null ? result : Color.BLACK;
  }

  public static boolean setBounds(Component child, int x, int y, int width, int height) {
    if (child.getX() != x || child.getY() != y || child.getWidth() != width || child.getHeight() != height) {
      child.setBounds(x, y, width, height);
      child.invalidate();
      if (child instanceof JComponent) ((JComponent) child).revalidate();
      child.repaint();
      return true;
    }
    return false;
  }

  public static boolean setBounds(Component component, Rectangle bounds) {
    return setBounds(component, bounds.x, bounds.y, bounds.width, bounds.height);
  }

  public static boolean setSize(Component component, int width, int height) {
    if (component.getWidth() != width || component.getHeight() != height) {
      component.setSize(width, height);
      component.invalidate();
      if (component instanceof JComponent) ((JComponent) component).revalidate();
      component.repaint();
      return true;
    }
    return false;
  }

  public static int getInsetWidth(JComponent component) {
    if (component == null)
      return 0;
    return getInsetWidth(component.getInsets());
  }

  public static int getInsetWidth(Insets insets) {
    return insets == null ? 0 : insets.left + insets.right;
  }

  public static int getInsetHeight(JComponent component) {
    if (component == null)
      return 0;
    return getInsetHeight(component.getInsets());
  }

  public static int getInsetHeight(Insets insets) {
    return insets == null ? 0 : insets.top + insets.bottom;
  }

  private static void priUniteInsets(Insets sum, Insets arg) {
    sum.top += arg.top;
    sum.left += arg.left;
    sum.right += arg.right;
    sum.bottom += arg.bottom;
  }

  public static Insets uniteInsetsFromTo(JComponent decendant, @Nullable Container ancestor) {
    Insets result = decendant.getInsets(new Insets(0, 0, 0, 0));
    Container c = decendant;
    while (true) {
      Container parent = c.getParent();
      if (parent == null) {
        assert ancestor == null : "Decendant: " + decendant + " acestor: " + ancestor;
        break;
      }
      if (parent instanceof JComponent)
        priUniteInsets(result, parent.getInsets());
      if (parent == ancestor)
        break;
      c = parent;
    }
    return result;
  }

  public static Insets uniteInsets(Insets ins1, Insets ins2) {
    Insets result = copyInsets(ins1);
    priUniteInsets(result, ins2);
    return result;
  }

  private static Insets copyInsets(Insets insets) {
    return new Insets(insets.top, insets.left, insets.bottom, insets.right);
  }

  /**
   * Sets Graphics clip to client area of component (inside insets).
   *
   * @return saved clip to pass it to {@link #restoreClip(java.awt.Graphics,javax.swing.JComponent,java.awt.Shape)} or
   *         null if clipped area is empty in this case no clipping is performed, drawing should be cancelled.
   */
  @Nullable
  @ThreadAWT
  public static Shape clipClientArea(Graphics g, JComponent component) {
    Threads.assertAWTThread();
    Shape savedClip = g.getClip();
    g.getClipBounds(TEMP_RECT);
    Insets insets = component.getInsets();
    int x = Math.max(insets.left, TEMP_RECT.x);
    int y = Math.max(insets.top, TEMP_RECT.y);
    int cornerX = Math.min(component.getWidth() - insets.right, TEMP_RECT.x + TEMP_RECT.width);
    int cornerY = Math.min(component.getHeight() - insets.bottom, TEMP_RECT.y + TEMP_RECT.height);
    if (cornerX <= x || cornerY <= y)
      return null;
    g.setClip(x, y, cornerX - x, cornerY - y);
    g.translate(insets.left, insets.top);
    return savedClip;
  }

  /**
   * Restores Graphics clip changed by {@link #clipClientArea(java.awt.Graphics,javax.swing.JComponent)}
   */
  public static void restoreClip(Graphics g, JComponent component, Shape savedClip) {
    Insets insets = component.getInsets();
    g.translate(-insets.left, -insets.top);
    g.setClip(savedClip);
  }

  public static void reshapeClientArea(JComponent component, int x, int y, int width, int height) {
    Insets insets = component.getInsets();
    x -= insets.left;
    y -= insets.top;
    width += getInsetWidth(insets);
    height += getInsetHeight(insets);
    component.setBounds(x, y, width, height);
  }

  public static void repaintClientRectangle(JComponent component, Rectangle rectangle) {
    Insets insets = component.getInsets();
    component.repaint(rectangle.x + insets.left, rectangle.y + insets.top, rectangle.width, rectangle.height);
  }

  public static void copyClientRectangle(JComponent component, Rectangle dest, Rectangle source) {
    Insets insets = component.getInsets();
    dest.x = source.x + insets.left;
    dest.y = source.y + insets.top;
    dest.width = source.width;
    dest.height = source.height;
  }

  public static String tableEventToString(TableModelEvent e) {
    StringBuilder result = new StringBuilder();
    switch (e.getType()) {
      case TableModelEvent.INSERT: result.append("INSERT"); break;
      case TableModelEvent.UPDATE: result.append("UPDATE"); break;
      case TableModelEvent.DELETE: result.append("DELETE"); break;
      default: result.append("UNKNWON (").append(e.getType()).append(")");
    }
    result.append("; ");
    int column = e.getColumn();
    result.append("Column:");
    if (column == TableModelEvent.ALL_COLUMNS)
      result.append("All");
    else
      result.append(column);
    result.append("; ");
    result.append("Rows:[").append(e.getFirstRow()).append(", ").append(e.getLastRow()).append("]");
    return result.toString();
  }

  public static boolean traverseFocus(Component component, KeyEvent e) {
    if (component == null || !component.getFocusTraversalKeysEnabled())
      return false;
    AWTKeyStroke stroke = KeyStroke.getAWTKeyStrokeForEvent(e);
    int[] ids = {
      KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
      KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS};
    for (int id : ids) {
      Set<AWTKeyStroke> strokes = component.getFocusTraversalKeys(id);

      if (strokes.contains(stroke)) {
        KeyboardFocusManager kbManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        switch (id) {
        case KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS: kbManager.focusNextComponent(component); return true;
        case KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS: kbManager.focusPreviousComponent(component); return true;
        case KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS: kbManager.upFocusCycle(component);
        }
        if (component instanceof Container) {
          Container container = (Container) component;
          if (id == KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS) kbManager.downFocusCycle(container);
          else assert false : id;
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Copied from {@link java.awt.Rectangle#intersection(java.awt.Rectangle)} <br>
   * Calculates intersection of two rectangles and stores result to dst. If dst is null creates new rectangle. <br>
   * dst may reuse any parameter (values are read before any result is stored).
   * @param dst optional rectangle to store result to
   * @return dst if was not null or newly created rectangle
   */
  public static Rectangle intersection(Rectangle rect1, Rectangle rect2, Rectangle dst) {
    int tx1 = rect1.x;
    int ty1 = rect1.y;
    int rx1 = rect2.x;
    int ry1 = rect2.y;
    long tx2 = tx1; tx2 += rect1.width;
    long ty2 = ty1; ty2 += rect1.height;
    long rx2 = rx1; rx2 += rect2.width;
    long ry2 = ry1; ry2 += rect2.height;
    if (tx1 < rx1) tx1 = rx1;
    if (ty1 < ry1) ty1 = ry1;
    if (tx2 > rx2) tx2 = rx2;
    if (ty2 > ry2) ty2 = ry2;
    tx2 -= tx1;
    ty2 -= ty1;
    // tx2,ty2 will never overflow (they will never be
    // larger than the smallest of the two source w,h)
    // they might underflow, though...
    if (tx2 < Integer.MIN_VALUE) tx2 = Integer.MIN_VALUE;
    if (ty2 < Integer.MIN_VALUE) ty2 = Integer.MIN_VALUE;
    if (dst == null)
      dst = new Rectangle();
    dst.setBounds(tx1, ty1, (int) tx2, (int) ty2);
    return dst;
  }

  public static int getFocusAction(KeyEvent e, Component c) {
    if (e.getID() != KeyEvent.KEY_PRESSED)
      return -1;
    AWTKeyStroke stroke = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
    for (int id : FOCUS_TRAVERSAL_KEY_IDS) {
      Set<AWTKeyStroke> set = c.getFocusTraversalKeys(id);
      if (set.contains(stroke))
        return id;
    }
    return -1;
  }

  public static boolean equals(Rectangle r, int x, int y, int width, int height) {
    return r.x == x && r.y == y && r.width == width && r.height == height;
  }

  public static boolean isSameSize(Rectangle r, int width, int height) {
    return r.width == width && r.height == height;
  }

  public static void applyRenderingHints(Graphics g) {
    if (!(g instanceof Graphics2D)) {
      assert false;
      return;
    }
    Toolkit tk = Toolkit.getDefaultToolkit();
    Map hints = ((Map)tk.getDesktopProperty("awt.font.desktophints"));
    if (hints != null) {
      ((Graphics2D)g).addRenderingHints(hints);
    }
  }

  /**
   * Extends target dimensions to cover given width and height.
   * @param target dimensions to be extended if required
   * @param width
   * @param height
   */
  public static void maxDimensions(Dimension target, int width, int height) {
    target.width = Math.max(target.width, width);
    target.height = Math.max(target.height, height);
  }
}
