package com.almworks.screenshot.shooter;

import com.almworks.api.gui.MainWindowManager;
import com.almworks.util.Env;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.FontSizeSettingPatch;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.Context;
import com.almworks.util.images.Icons;
import com.almworks.util.images.ImageUtil;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.WindowUtil;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.ArrayUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

/**
 * @author Stalex
 */
public class ShootDialog extends JFrame {
  private static final Integer[] VK_INT = {
    KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
    KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8 };

  private final JComponent myWholePanel;
  private final Procedure<BufferedImage> mySink;
  private final DetachComposite myLife = new DetachComposite();
  private final Frame myOtherFrame;

  private boolean myFramesMinimized;
  private boolean myMainFrameMinimized;
  private JButton myDefaultButton;
  public static final Comparator<Rectangle> BY_Y_STRICT = (r1, r2) -> {
    if (r1.getMinY() >= r2.getMaxY()) return 1;
    if (r2.getMinY() >= r1.getMaxY()) return -1;
    return 0;
  };
  public static final Comparator<Rectangle> BY_X = Comparator.comparing(Rectangle::getX);
  public static final Comparator<Rectangle> BY_Y = Comparator.comparing(Rectangle::getY);
  public static final Comparator<Rectangle> SCREEN_RECT = BY_Y_STRICT.thenComparing(BY_X).thenComparing(BY_Y);

  public static ShootDialog create(Component contextComponent, Configuration config, Procedure<BufferedImage> sink) {
    GraphicsDevice device = null;

    Frame otherFrame = null;
    if(contextComponent != null && contextComponent.isShowing()) {
      final GraphicsConfiguration gc = contextComponent.getGraphicsConfiguration();
      if(gc != null) {
        device = gc.getDevice();
      }

      Window window = SwingTreeUtil.getOwningWindow(contextComponent);
      while(window != null && !(window instanceof Frame)) {
        window = window.getOwner();
      }
      if(window instanceof Frame) {
        otherFrame = (Frame)window;
      }
    }

    if(device == null) {
      final Window w = UIUtil.getDefaultDialogOwner();
      if(w != null) {
        device = w.getGraphicsConfiguration().getDevice();
      } else {
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      }
    }

    return new ShootDialog(sink, device, otherFrame, config);
  }

  private ShootDialog(
    Procedure<BufferedImage> sink, GraphicsDevice defaultDevice, Frame otherFrame, Configuration config)
    throws HeadlessException
  {
    super("Screenshot", defaultDevice != null ? defaultDevice.getDefaultConfiguration() : null);

    mySink = sink;
    myOtherFrame = otherFrame;

    myWholePanel = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 0, true));

    final Clipboard clipboard = DndUtil.getClipboardSafe(true);
    if(clipboard != null) {
      final java.util.List<ClipboardCapturer> cc = Collections15.arrayList();
      cc.add(new DefaultClipboardCapturer());
      if(Env.isMac()) {
        cc.add(new ImageIOClipboardCapturer());
      }

      for(final ClipboardCapturer capturer : cc) {
        if(capturer.canExtractImage(clipboard)) {
          final JButton button = new JButton(capturer);
          myDefaultButton = button;
          myWholePanel.add(button);
          break;
        }
      }
    }

    GraphicsDevice[] devices = sortDevices(GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices());
    final boolean single = devices.length == 1;
    Icon[] icons;
    if (single) {
      icons = new Icon[devices.length];
      Arrays.fill(icons, null);
    } else icons = ScreenLocationIcon.createIcons(Math.max(12, FontSizeSettingPatch.getOverrideFontSize()) * 4 / 3, devices);
    for(int i = 0; i < devices.length; i++) {
      final GraphicsDevice device = devices[i];
      final JButton button = new JButton(new CaptureDevice(device, i, single));
      button.setIcon(icons[i]);
      button.setHorizontalAlignment(SwingConstants.LEADING);
      if(myDefaultButton == null && defaultDevice == device) {
        myDefaultButton = button;
      }
      myWholePanel.add(button);
    }

    myWholePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
    myWholePanel.getActionMap().put("esc", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

    myWholePanel.setOpaque(true);
    if(Aqua.isAqua()) {
      myWholePanel.setBorder(UIUtil.BORDER_5);
    }

    getContentPane().add(myWholePanel);
    setResizable(false);
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setAlwaysOnTop(true);
    setFocusableWindowState(true);
    setIconImage(Icons.ACTION_ATTACH_SCREENSHOT.getImage());

    if(myDefaultButton != null) {
      myDefaultButton.setDefaultCapable(true);
      getRootPane().setDefaultButton(myDefaultButton);
    }

    WindowUtil.setupWindow(
      myLife, this, config, true, null, true,
      defaultDevice == null ? null : defaultDevice.getDefaultConfiguration(),
      null);
  }

  private GraphicsDevice[] sortDevices(GraphicsDevice[] devices) {
    devices = ArrayUtil.arrayCopy(devices);
    Arrays.sort(devices, Comparator.comparing(d -> d.getDefaultConfiguration().getBounds(), SCREEN_RECT));
    return devices;
  }

  @Deprecated
  public void show() {
    minimizeFrames();
    pack();

    super.show();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        assignFocus();
      }
    });
  }

  private void assignFocus() {
    if(!myWholePanel.isShowing()) {
      return;
    }

    toFront();
    requestFocus();

    if(myDefaultButton != null) {
      myDefaultButton.requestFocusInWindow();
    }
  }

  private void callSink(BufferedImage capture) {
    restoreFrames();
    mySink.invoke(capture);
  }

  public void dispose() {
    super.dispose();
    myLife.detach();
    restoreFrames();
  }

  private void minimizeFrames() {
    if(!myFramesMinimized) {
      final MainWindowManager mwm = Context.get(MainWindowManager.class);
      final Frame mainFrame = mwm == null ? null : mwm.getMainFrame();

      if(mwm != null && !UIUtil.isMinimized(mainFrame)) {
        mwm.minimize();
        myFramesMinimized = true;
        myMainFrameMinimized = true;
      }

      if(myOtherFrame != null && myOtherFrame != mainFrame) {
        UIUtil.minimizeFrame(myOtherFrame);
        myFramesMinimized = true;
      }
    }
  }

  private void restoreFrames() {
    if(myFramesMinimized) {
      final MainWindowManager mwm = Context.get(MainWindowManager.class);
      final Frame mainFrame = mwm == null ? null : mwm.getMainFrame();

      if(mwm != null && myMainFrameMinimized) {
        if(mainFrame == myOtherFrame || myOtherFrame == null) {
          mwm.bringToFront();
        } else if(mainFrame != null) {
          UIUtil.restoreFrame(mainFrame);
        }
        myFramesMinimized = false;
        myMainFrameMinimized = false;
      }

      if(myOtherFrame != null && myOtherFrame != mainFrame) {
        UIUtil.restoreFrame(myOtherFrame);
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            myOtherFrame.toFront();
            final Component focusOwner = myOtherFrame.getFocusOwner();
            if(focusOwner != null) {
              focusOwner.requestFocus();
            } else {
              myOtherFrame.requestFocus();
            }
          }
        });
        myFramesMinimized = false;
      }
    }
  }

  public void addDetach(Detach detach) {
    myLife.add(detach);
  }

  private abstract class ClipboardCapturer extends AbstractAction {
    protected ClipboardCapturer() {
      putValue(MNEMONIC_KEY, KeyEvent.VK_C);
      putValue(NAME, "Paste from Clipboard");
      myWholePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("S"), this);
      myWholePanel.getActionMap().put(this, this);
    }

    public void actionPerformed(ActionEvent ev) {
      final Clipboard clipboard = DndUtil.getClipboardSafe(true);
      if(clipboard != null) {
        Image data = null;
        try {
          data = extractImage(clipboard);
        } catch(Exception e) {
          Log.warn(e);
        }
        if(data != null) {
          final BufferedImage bufferedImage;
          if(data instanceof BufferedImage) {
            bufferedImage = (BufferedImage) data;
          } else {
            bufferedImage = ImageUtil.createBufferedImage(data);
          }
          callSink(bufferedImage);
        }
      }
      dispose();
    }

    protected abstract boolean canExtractImage(@NotNull Clipboard clipboard);

    protected abstract Image extractImage(@NotNull Clipboard clipboard) throws UnsupportedFlavorException, IOException;
  }

  private class DefaultClipboardCapturer extends ClipboardCapturer {
    @Override
    protected boolean canExtractImage(@NotNull Clipboard clipboard) {
      return clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor);
    }

    @Override
    protected Image extractImage(@NotNull Clipboard clipboard) throws UnsupportedFlavorException, IOException {
      return (Image) clipboard.getData(DataFlavor.imageFlavor);
    }
  }

  private class ImageIOClipboardCapturer extends ClipboardCapturer {
    private DataFlavor getMatchingFlavor(@NotNull Clipboard clipboard) {
      for(final DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
        if("image".equals(flavor.getPrimaryType())
          && flavor.isRepresentationClassInputStream()
          && isReaderPresent(flavor))
        {
          return flavor;
        }
      }
      return null;
    }

    private boolean isReaderPresent(DataFlavor flavor) {
      final String mimeType = flavor.getPrimaryType() + "/" + flavor.getSubType();
      final Iterator<ImageReader> i = ImageIO.getImageReadersByMIMEType(mimeType);
      return i != null && i.hasNext();
    }

    @Override
    protected boolean canExtractImage(@NotNull Clipboard clipboard) {
      return getMatchingFlavor(clipboard) != null;
    }

    @Override
    protected Image extractImage(@NotNull Clipboard clipboard) throws UnsupportedFlavorException, IOException {
      final DataFlavor flavor = getMatchingFlavor(clipboard);
      if(flavor != null) {
        return ImageIO.read((InputStream) clipboard.getData(flavor));
      }
      return null;
    }
  }

  private class CaptureDevice extends AbstractAction {
    private final GraphicsDevice myDevice;

    public CaptureDevice(GraphicsDevice device, int index, boolean single) {
      myDevice = device;
      if (index >= 0 && index < VK_INT.length) {
        myWholePanel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
          .put(KeyStroke.getKeyStroke(String.valueOf(index + 1)), this);
        myWholePanel.getActionMap().put(this, this);
        putValue(MNEMONIC_KEY, VK_INT[index]);
      }
      putValue(NAME, single ? "Capture Screen" : "Capture Screen #" + (index + 1));
    }

    public void actionPerformed(ActionEvent e) {
      hide();
      Capturer capturer = new ScreenCapturer();
      BufferedImage capture = capturer.capture(myDevice);
      if (capture != null) {
        callSink(capture);
      }
      dispose();
    }
  }
}
