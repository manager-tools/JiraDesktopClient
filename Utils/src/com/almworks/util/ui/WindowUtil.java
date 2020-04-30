package com.almworks.util.ui;

import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.macosx.FullScreenEvent;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class WindowUtil {
  public static interface WindowPositioner {
    /**
     * @param screen Screen bounds.
     * @param position The original window position (from config or default); nullable.
     * @param window Window size.
     * @return Adjusted window position.
     */
    Point getPosition(Rectangle screen, @Nullable Point position, Dimension window);

    WindowPositioner CENTER = new WindowPositioner() {
      @Override
      public Point getPosition(Rectangle screen, @Nullable Point position, Dimension window) {
        return UIUtil.getCenterRectangle(screen, window);
      }
    };
  }

  /**
   * A bridge from {@link com.almworks.util.ui.UIUtil.Positioner}.
   */
  public static class OwnedPositioner implements WindowPositioner {
    private final Component myOwner;
    private final UIUtil.Positioner myPositioner;

    public OwnedPositioner(Component owner, UIUtil.Positioner positioner) {
      myOwner = owner;
      myPositioner = positioner;
    }

    @Override
    public Point getPosition(Rectangle screen, @Nullable Point position, Dimension window) {
      if(myOwner.isShowing()) {
        final Rectangle owner = new Rectangle(myOwner.getLocationOnScreen(), myOwner.getSize());
        return myPositioner.getPosition(screen, owner, window);
      } else {
        return position;
      }
    }
  }

  /**
   * Sets up window location and size, based on values stored in the configuration. Makes sure the window is
   * visible (only when setPosition = true) on one of the displays. Sets up window state, position and size monitoring
   * and saves all changes to the configuration.
   *
   * @param life lifespan for the window monitoring.
   * @param window window to be set up.
   * @param config configuration that stores window state parameters
   * @param setPosition if false, window position will not be touched or monitored.
   * @param preferredSize preferredWindow size in case the size is not stored in config. if null, the window will be packed.
   * @param requiredDisplay if not null, the window must appear on this display
   */
  public static void setupWindow(@NotNull Lifespan life, @NotNull final Window window, @NotNull Configuration config,
    boolean setPosition, @Nullable Dimension preferredSize, boolean overrideStoredSize,
    @Nullable GraphicsConfiguration requiredDisplay, @Nullable WindowPositioner adjuster)
  {
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    if (genv.isHeadlessInstance())
      return;
    GraphicsDevice[] devices = genv.getScreenDevices();
    boolean multiDisplay = devices.length > 1;

    final ConfigAccessors dimensionAcc = ConfigAccessors.dimension(config);
    boolean maximized = dimensionAcc.isMaximized();
    boolean fullScreen = dimensionAcc.isFullScreen();
    boolean fullScreenable = MacIntegration.isWindowFullScreenable(window);
    boolean useOldValues = maximized || (fullScreen && fullScreenable);

    if (fullScreen && fullScreenable) {
      window.addWindowListener(new WindowAdapter() {
        public void windowOpened(WindowEvent e) {
          MacIntegration.toggleFullScreen(window);
          window.removeWindowListener(this);
        }
      });
      maximized = false;
    }

    final ConfigAccessors positionAcc;
    GraphicsConfiguration display = window.getGraphicsConfiguration();
    Point position = null;

    if (setPosition) {
      positionAcc = ConfigAccessors.position(config, multiDisplay);
      position = useOldValues ? positionAcc.getOldPoint() : positionAcc.getPointOrNull();
      if (position != null) {
        GraphicsConfiguration gc = UIUtil.getGraphicsConfigurationForPoint(position, devices);
        if (gc == null) {
          // position is off the screen, try to find closest and adjust position
          Dimension lastSize = dimensionAcc.getDimensionOrNull();
          if (lastSize != null) {
            gc = findDominatedConfigurationAndAdjustPosition(position, lastSize, devices);
          }
        }
        if (gc == null) {
          // completely off the screen
          position = null;
        } else {
          // use this configuration when limiting window size
          display = gc;
        }
      }
      if (requiredDisplay != null && !Util.equals(requiredDisplay, display)) {
        // change display to required, invalidate position
        display = requiredDisplay;
        position = null;
      }
      if (position != null) {
        window.setLocation(position);
      } else {
        // use setLocationRelativeTo(), but only after window size is set
      }
    } else {
      positionAcc = null;
    }

    Rectangle screenBounds = UIUtil.getScreenUserSize(display);
    Dimension dimension = useOldValues ? dimensionAcc.getOldDimension() : dimensionAcc.getDimensionOrNull();
    if (dimension != null && !overrideStoredSize) {
      dimension = UIUtil.constrainDimension(dimension, screenBounds.width, screenBounds.height);
      window.setSize(dimension);
    } else {
      setPreferredSize(window, UIUtil.constrainDimension(preferredSize, screenBounds.width, screenBounds.height), screenBounds.width, screenBounds.height);
      dimensionAcc.setDimension(window.getSize());
    }

    if (setPosition && adjuster != null) {
      position = adjuster.getPosition(screenBounds, position, window.getSize());
      window.setLocation(position);
    }

    if (setPosition && position == null) {
      UIUtil.centerWindow(window);
      // save position/dimension here?
    }

    if (maximized) {
      if (window instanceof Frame) {
        ((Frame) window).setExtendedState(Frame.MAXIMIZED_BOTH);
      }
    }

    WindowConfigurationWatcher watcher = new WindowConfigurationWatcher(window, dimensionAcc, positionAcc);
    UIUtil.addComponentListener(life, window, watcher);
    UIUtil.addWindowStateListener(life, window, watcher);
    if (fullScreenable) {
      MacIntegration.addFullScreenListener(window, watcher);
    }
  }

  private static GraphicsConfiguration findDominatedConfigurationAndAdjustPosition(Point position, Dimension size,
    GraphicsDevice[] devices)
  {
    int maxSpace = 0;
    GraphicsConfiguration maxConfiguration = null;
    int maxAdjustment = 0;
    Rectangle window = new Rectangle(position, size);
    for (GraphicsDevice device : devices) {
      GraphicsConfiguration gc = device.getDefaultConfiguration();
      Rectangle bounds = gc.getBounds();
      Rectangle intr = bounds.intersection(window);
      if (intr.isEmpty())
        continue;
      int space = intr.width * intr.height;
      if (space <= 0)
        continue;
      if (space >= maxSpace) {
        maxSpace = space;
        maxConfiguration = gc;
        maxAdjustment = Math.max(0, bounds.y - position.y);
      }
    }
    if (maxConfiguration != null) {
      position.y += maxAdjustment;
    }
    return maxConfiguration;
  }


  private static void setPreferredSize(final Window window, Dimension preferedSize, final int maxWidth, final int maxHeight) {
    if (preferedSize != null) {
      window.setSize(preferedSize);
    } else {
      // double-pack for some controls that re-layouted by size decorators
      window.pack();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          window.pack();
          Dimension size = window.getSize();
          if (size.width > maxWidth || size.height > maxHeight) {
            size.width = Math.min(size.width, maxWidth);
            size.height = Math.min(size.height, maxHeight);
            window.setSize(size);
          }
        }
      });
    }
  }
  
  private static class WindowConfigurationWatcher extends ComponentAdapter implements WindowStateListener, FullScreenEvent.Listener {
    private final Window myWindow;
    private final ConfigAccessors myDimensionAcc;
    private final ConfigAccessors myPositionAcc;
    private final boolean myFullScreenable;

    public WindowConfigurationWatcher(Window window, ConfigAccessors dimensionAcc, ConfigAccessors positionAcc) {
      myWindow = window;
      myDimensionAcc = dimensionAcc;
      myPositionAcc = positionAcc;
      myFullScreenable = MacIntegration.isWindowFullScreenable(myWindow);
    }

    public void componentResized(ComponentEvent e) {
      if (myFullScreenable && MacIntegration.isWindowInFullScreen(myWindow)) {
        return;
      }
      Dimension oldSize = myDimensionAcc.getDimension();
      Dimension newSize = myWindow.getSize();
      if (!oldSize.equals(newSize)) {
        myDimensionAcc.setOldDimension(oldSize);
        myDimensionAcc.setDimension(newSize);
      }
    }

    public void componentMoved(ComponentEvent e) {
      if (myPositionAcc != null) {
        Point oldPoint = myPositionAcc.getPoint();
        Point newPoint = myWindow.getLocation();
        if (!oldPoint.equals(newPoint)) {
          myPositionAcc.setOldPoint(oldPoint);
          myPositionAcc.setPoint(newPoint);
        }
      }
    }

    public void windowStateChanged(WindowEvent e) {
      myDimensionAcc.setMaximized(((e.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH));
    }

    public void onFullScreenEvent(FullScreenEvent e) {
      if (e.getWindow() == myWindow) {
        myDimensionAcc.setFullScreen(e.getType() == FullScreenEvent.Type.ENTERED);
      }
    }
  }
}
