package com.almworks.screenshot;

import com.almworks.api.gui.FrameBuilder;
import com.almworks.api.gui.WindowManager;
import com.almworks.container.TestContainer;
import com.almworks.gui.DefaultWindowManager;
import com.almworks.screenshot.editor.image.ImageEditor;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.ui.UIComponentWrapper2;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageEditorDemo {


  public static void show(WindowManager wm, UIComponentWrapper2 wrapper) {
    FrameBuilder myFrameBuilder;
    myFrameBuilder = wm.createFrame("imageEditor");
    myFrameBuilder.setPreferredSize(new Dimension(600, 600));
    myFrameBuilder.setTitle("Editor");

    myFrameBuilder.setContent(wrapper.getComponent());
    /*if (myShown)
      throw new IllegalStateException("cannot show twice");
    myShown = true;*/
    myFrameBuilder.showWindow(wrapper.getDetach());
  }

  public static void main(final String[] args) {
    LAFUtil.initializeLookAndFeel();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          TestContainer container = new TestContainer();
          Configuration config = ConfigurationUtil.copy(Configuration.EMPTY_CONFIGURATION);
          WindowManager wm = new DefaultWindowManager(container, config);

          if (args.length >= 1 && args[0].equals("shooter")) {
            showShooter(config, wm);
          } else {
            showEditor(config, wm);
          }

        } catch (AWTException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private static void showShooter(Configuration config, WindowManager wm) {
    final ScreenShooterImpl shooter = new ScreenShooterImpl(wm, config);
    shooter.shoot(null, Configuration.EMPTY_CONFIGURATION, null);
  }

  private static void showEditor(Configuration config, WindowManager wm) throws AWTException {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gd = ge.getScreenDevices()[0];
    

    GraphicsConfiguration gc = gd.getDefaultConfiguration();

    Rectangle bounds = gc.getBounds();



    bounds.setLocation(0, 0);

    Toolkit.getDefaultToolkit().sync();

    Robot robot = new Robot(gd);
    BufferedImage image = robot.createScreenCapture(bounds);

    final ImageEditor editor = new ImageEditor(image);
    editor.addDisposeNotification(new Runnable() {
      public void run() {
        System.exit(2);
      }
    });
    show(wm, editor);
  }

}
