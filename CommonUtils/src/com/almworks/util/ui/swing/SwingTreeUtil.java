package com.almworks.util.ui.swing;

import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.exec.Context;
import com.almworks.util.threads.ThreadAWT;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.applet.Applet;
import java.awt.*;

/**
 * @author dyoma
 */
public class SwingTreeUtil {
  public static boolean isAncestor(@Nullable Component ancestor, @Nullable Component child) {
    if (ancestor == null)
      return false;
    while (child != null) {
      if (child == ancestor)
        return true;
      child = child.getParent();
    }
    return false;
  }

  public static boolean isAncestorOfFocusOwner(Container ancestor) {
    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return owner != null && (owner == ancestor || isAncestor(ancestor, owner));
  }

  @Nullable
  public static Container findFocusPolicyProvider(@Nullable Component start, @Nullable Container stop) {
    while (start != null && start != stop) {
      if (start instanceof JComponent && ((JComponent) start).isFocusTraversalPolicyProvider() && ((JComponent) start).getFocusTraversalPolicy() != null)
        return (Container) start;
      start = start.getParent();
    }
    return null;
  }

  public static int getComponentIndex(Container parent, Component child) {
    int count = parent.getComponentCount();
    for (int i = 0; i < count; i++)
      if (child == parent.getComponent(i))
        return i;
    return -1;
  }

  @Nullable
  public static <T> T findAncestorOfType(@Nullable Component component, @NotNull Class<? extends T> aClass) {
    Component parent = component;
    while (parent != null) {
      T cast = Util.castNullable(aClass, parent);
      if (cast != null) return cast;
      parent = parent.getParent();
    }
    return null;
  }

  /**
   * Returns the owner window for the given component.
   * For JPopupMenus follows the getInvoker() link, for
   * other component types the standard getParent() method
   * is used.
   * @param component The component.
   * @return Component's owner window.
   */
  public static Window getOwningWindow(Component component) {
    while (component != null) {
      if (component instanceof Window) {
        return (Window) component;
      }

      if (component instanceof JPopupMenu) {
        component = ((JPopupMenu) component).getInvoker();
      } else {
        component = component.getParent();
      }
    }
    
    return null;
  }

  public static Rectangle getVisibleScreenBounds(Component component) {
    Rectangle result = component.getBounds();
    Rectangle parentRect = null;
    for (Container parent = component.getParent(); parent != null; parent = parent.getParent()) {
      boolean window = false;
      if (parent instanceof Window || parent instanceof Applet) {
        if (parentRect == null) parentRect = new Rectangle();
        parentRect.setLocation(parent.getLocationOnScreen());
        parentRect.width = parent.getWidth();
        parentRect.height = parent.getHeight();
        window = true;
      } else parentRect = parent.getBounds(parentRect);
      result.x += parentRect.x;
      result.y += parentRect.y;
      result = result.intersection(parentRect);
      if (window) break;
    }
    Point p = new Point(0, 0);
    SwingUtilities.convertPointToScreen(p, component);
    return result;
  }

  /**
   * @return list of all descendant components iterable via {@link #iterateDescendants(java.awt.Container, TreeElementVisitor)}<br>
   * The result is mofiable (replace element) iff not empty and appendable iff size is greater than 1.
   */
  public static ThreadLocal<DescendantsCollector> COMPONENTS_COLLECTOR = new ThreadLocal<DescendantsCollector>();
  @ThreadAWT
  public static java.util.List<Component> descendants(Container container) {
    assert Context.isAWT();
    DescendantsCollector collector = COMPONENTS_COLLECTOR.get();
    if (collector == null) collector = new DescendantsCollector();
    else COMPONENTS_COLLECTOR.set(null);
    collector.getIterator().clear();
    iterateDescendants(container, collector);
    java.util.List<Component> result = collector.getIterator().notEmptyCopyAndClear();
    COMPONENTS_COLLECTOR.set(collector);
    return result;
  }

  /**
   * Performs depth-first iteration through components tree. Do not iterate into {@link javax.swing.CellRendererPane}s.
   * @param container start from. The container isnt passed to iterator
   * @param iterator
   * @return true if iteration isnt terminated (probably some substrees are skipped by iterator)
   */
  public static boolean iterateDescendants(Container container, TreeElementVisitor<Component> iterator) {
    for (int i = 0; i < container.getComponentCount(); i++) {
      Component component = container.getComponent(i);
      if (component instanceof CellRendererPane)
        continue;
      TreeElementVisitor.Result result = iterator.visit(component);
      if (result == TreeElementVisitor.Result.STOP) return false;
      if (result == TreeElementVisitor.Result.GO_ON && component instanceof Container)
        if (!iterateDescendants((Container) component, iterator))
          return false;
    }
    return true;
  }

  @Nullable
  public static Window getOwningFrameDialog(@Nullable Window window) {
    while (window != null && !(window instanceof Frame ||
      window instanceof Dialog)) {
      window = (Window)window.getParent();
    }
    return window;
  }

  /**
   * Should be used when rare changes affects component layout on complex form with {@link javax.swing.plaf.metal.MetalBorders.ScrollPaneBorder scroll pane borders}
   * @param component a component inside the widow to be revalidated.
   */
  public static void revalidateWindow(JComponent component) {
    JRootPane pane = SwingTreeUtil.findAncestorOfType(component, JRootPane.class);
    if (pane != null) pane.revalidate();
    component.repaint();
  }

  private static class DescendantsCollector extends TreeElementVisitor.WholeTree<Component> {
    public DescendantsCollector() {
      super(new ElementVisitor.Collector<Component>());
    }

    @Override
    public ElementVisitor.Collector<Component> getIterator() {
      return (ElementVisitor.Collector<Component>)super.getIterator();
    }
  }
}
