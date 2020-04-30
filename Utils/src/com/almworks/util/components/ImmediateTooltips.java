package com.almworks.util.components;

import com.almworks.util.Env;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;

public class ImmediateTooltips {
  private static final ComponentProperty<Popup> POPUP = ComponentProperty.createProperty("immediateTooltipPopup");
  private static final ComponentProperty<JToolTip> TOOLTIP =
    ComponentProperty.createProperty("immediateTooltipTooltip");
  private static final ComponentProperty<Point> LAST_POINT =
    ComponentProperty.createProperty("immediateTooltipLastPoint");
  // From component coordinates to screen coordinates
  private static final ComponentProperty<TooltipLocationProvider> LOCATION_PROVIDER =
    ComponentProperty.createProperty("popupLocationConvertor");

  private static WeakReference<JComponent> ourLastShownTooltipOwner = null;
  private static AWTEventListener ourFocusListener;

  public static void installImmediateTooltipManager(Lifespan lifespan, final JComponent component) {
    installImmediateTooltipManager(lifespan, component, null);
  }

  public static void installImmediateTooltipManager(Lifespan lifespan, final JComponent component,
    TooltipLocationProvider locationProvider)
  {
    ToolTipManager.sharedInstance().unregisterComponent(component);
    if (locationProvider != null) {
      LOCATION_PROVIDER.putClientValue(component, locationProvider);
    }
    component.addMouseListener(MouseHandler.INSTANCE);
    component.addMouseMotionListener(MouseHandler.INSTANCE);
    ensureFocusListenerInstalled();
    lifespan.add(new Detach() {
      protected void doDetach() {
        uninstallImmediateTooltipManager(component);
      }
    });
  }

  private static synchronized void ensureFocusListenerInstalled() {
    if (ourFocusListener != null)
      return;
    ourFocusListener = new AWTEventListener() {
      public void eventDispatched(AWTEvent event) {
        if (ourLastShownTooltipOwner != null && (event instanceof FocusEvent)) {
          FocusEvent fe = (FocusEvent) event;
          Component one = fe.getComponent();
          Component another = fe.getOppositeComponent();
          if (one == null || another == null ||
            SwingUtilities.getWindowAncestor(one) != SwingUtilities.getWindowAncestor(another))
          {
            // window has changed
            hideTooltip();
          }
        }
      }
    };
    Toolkit.getDefaultToolkit().addAWTEventListener(ourFocusListener, FocusEvent.FOCUS_EVENT_MASK);
  }

  public static void uninstallImmediateTooltipManager(JComponent component) {
    component.removeMouseListener(MouseHandler.INSTANCE);
    component.removeMouseMotionListener(MouseHandler.INSTANCE);
    hideTooltip(component);
    TOOLTIP.putClientValue(component, null);
    LOCATION_PROVIDER.putClientValue(component, null);
  }

  public static void tooltipChanged(JComponent component) {
    if (POPUP.getClientValue(component) != null) {
      showTooltip(component, null);
    }
  }

  private static void showTooltip(JComponent c, MouseEvent event) {
    if (c == null)
      return;
    Window window = SwingUtilities.getWindowAncestor(c);
    if (window == null)
      return;
    if (!window.isFocused() || !window.isActive())
      return;
    Point lastPoint = LAST_POINT.getClientValue(c);
    if (event == null && lastPoint != null) {
      long now = System.currentTimeMillis();
      event = new MouseEvent(c, MouseEvent.MOUSE_ENTERED, now, 0, lastPoint.x, lastPoint.y, 0, false);
    }
    String text = event == null ? c.getToolTipText() : c.getToolTipText(event);
    Popup popup = POPUP.getClientValue(c);
    if (popup != null) {
      JToolTip toolTip = TOOLTIP.getClientValue(c);
      if (toolTip != null && Util.equals(toolTip.getTipText(), text)) {
        return;
      }
      popup.hide();
      POPUP.putClientValue(c, null);
      ourLastShownTooltipOwner = null;
    }
    if (text != null && text.length() > 0 && lastPoint != null) {
      hideTooltip();
      popup = createPopup(c, text, lastPoint);
      POPUP.putClientValue(c, popup);
      popup.show();
      ourLastShownTooltipOwner = new WeakReference<JComponent>(c);
    }
  }

  private static void hideTooltip() {
    WeakReference<JComponent> lastOwner = ourLastShownTooltipOwner;
    if (lastOwner != null) {
      JComponent c = lastOwner.get();
      if (c != null) {
        hideTooltip(c);
      }
      lastOwner.clear();
      ourLastShownTooltipOwner = null;
    }
  }

  private static void hideTooltip(JComponent c) {
    if (c == null)
      return;
    Popup popup = POPUP.getClientValue(c);
    if (popup != null) {
      popup.hide();
      POPUP.putClientValue(c, null);
    }
    LAST_POINT.putClientValue(c, null);
    ourLastShownTooltipOwner = null;
  }

  private static JComponent getComponent(MouseEvent e) {
    Component component = e.getComponent();
    if (!(component instanceof JComponent))
      return null;
    return (JComponent) component;
  }

  private static Popup createPopup(JComponent component, String text, Point point) {
    JToolTip contents = getTooltip(component);

    contents.setTipText(text);
    contents.setBorder(null);
    contents.updateUI();
    if (text.startsWith("<html>")) {
      contents.setBorder(UIUtil.getCompoundBorder(contents.getBorder(), new EmptyBorder(1, 4, 1, 4)));
    }

    Point location = getPopupLocation(component, point);
    Dimension size = contents.getPreferredSize();
    GraphicsConfiguration gc = UIUtil.getGraphicsConfigurationForPoint(location);
    Rectangle bounds = gc.getBounds();
    if (size.width > bounds.width / 2) {
      contents.setSize(bounds.width / 2, Short.MAX_VALUE);
      size = contents.getPreferredSize();
    }
    int boundsRight = bounds.x + bounds.width;
    if (location.x + size.width > boundsRight) {
      location.x = boundsRight - size.width - 1;
    }
    int boundsBottom = bounds.y + bounds.height;
    if (location.y + size.height > boundsBottom) {
      location.y = boundsBottom - size.height - 1;
    }
    return UIUtil.getPopup(component, contents, location.x, location.y);
  }

  private static Point getPopupLocation(JComponent component, Point point) {
    TooltipLocationProvider provider = LOCATION_PROVIDER.getClientValue(component);
    if (provider == null) {
      provider = TooltipLocationProvider.UNDER_COMPONENT;
    }
    Point location = provider.getTooltipLocation(component, point);
    if (location == null) {
      assert false : point + " " + provider;
      location = point;
    }
    return location;
  }

  private static JToolTip getTooltip(JComponent component) {
    JToolTip toolTip = TOOLTIP.getClientValue(component);
    if (toolTip == null) {
      toolTip = new JToolTip();
      toolTip.setComponent(component);
      TOOLTIP.putClientValue(component, toolTip);
    }
    return toolTip;
  }


  private static final class MouseHandler implements MouseListener, MouseMotionListener {
    private static final boolean IS_MAC = Env.isMac();
    
    public static final MouseHandler INSTANCE = new MouseHandler();

    public void mouseClicked(MouseEvent e) {
      mouseButton(e);
    }

    public void mousePressed(MouseEvent e) {
      mouseButton(e);
    }

    public void mouseReleased(MouseEvent e) {
      mouseButton(e);
    }

    private void mouseButton(MouseEvent e) {
      if(IS_MAC && e.isPopupTrigger()) {
        hideTooltip(getComponent(e));
      }
    }

    public void mouseEntered(MouseEvent e) {
      showNow(e);
    }

    public void mouseExited(MouseEvent e) {
      hideTooltip(getComponent(e));
    }

    public void mouseDragged(MouseEvent e) {
      showNow(e);
    }

    public void mouseMoved(MouseEvent e) {
      showNow(e);
    }

    private void showNow(MouseEvent e) {
      JComponent c = getComponent(e);
      if(c != null) {
        LAST_POINT.putClientValue(c, e.getPoint());
        showTooltip(c, e);
      }
    }
  }
}
