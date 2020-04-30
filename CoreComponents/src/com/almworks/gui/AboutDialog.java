package com.almworks.gui;

import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.install.Setup;
import com.almworks.platform.about.ApplicationAbout;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.OpenBrowserAnAction;
import com.almworks.util.components.URLLink;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.MacIntegration;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageObserver;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : Dyoma
 */
public class AboutDialog implements Startable {
  public static final Role<AboutDialog> ROLE = Role.role("aboutDialog", AboutDialog.class);

  private static final Color FOREGROUND_COLOR = Setup.isJiraClient() ? new Color(0x434343) : Color.WHITE;

  private static final String MARKER = "####";
  private static final String OPEN_BRACE = "{{";
  private static final String CLOSE_BRACE = "}}";
  private static final int CLOSE_BRACE_LENGTH = CLOSE_BRACE.length();
  private static final int OPEN_BRACE_LENGTH = OPEN_BRACE.length();
  private static final int MARKER_LENGTH = MARKER.length();

  private static final String TEXT_BLOCK = "textBlock";

  private final ActionRegistry myRegistry;
  private final MainWindowManager myMainWindowManager;
  private final ApplicationAbout myAbout;

  private final Runnable SHOW_DIALOG = new Runnable() {
    public void run() {
      About dialog = new About(myMainWindowManager.getMainFrame(), createAboutProperties());
      dialog.initialize();
      dialog.updateBounds();
      dialog.setVisible(true);
      dialog.requestFocus();
    }
  };

  public AboutDialog(ActionRegistry registry, MainWindowManager mainWindowManager, ApplicationAbout about) {
    myRegistry = registry;
    myMainWindowManager = mainWindowManager;
    myAbout = about;
  }

  public void start() {
    registerActions();
  }

  private void registerActions() {
    if (Env.isMac()) {
      MacIntegration.setAboutHandler(this::showAbout);
    } else {
      myRegistry.registerAction(MainMenu.Help.ABOUT, new SimpleAction("&About") {
        @Override
        protected void customUpdate(UpdateContext context) {
          String productName = Setup.getProductName();
          context.putPresentationProperty(PresentationKey.NAME, "&About " + productName);
        }

        @Override
        protected void doPerform(ActionContext context) throws CantPerformException {
          context.getSourceObject(ROLE).showAbout();
        }
      });
    }

    myRegistry.registerAction(MainMenu.Help.USER_MANUAL,
      new OpenBrowserAnAction(Setup.getUrlUserManual(), true, "User &Manual"));
  }

  public void showAbout() {
    SwingUtilities.invokeLater(SHOW_DIALOG);
  }

  public void stop() {
  }

  private Properties createAboutProperties() {
    Properties myProperties = new Properties(System.getProperties());
    myProperties.putAll(myAbout.getAboutProperties());
    myProperties.put("currentYear", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
    return myProperties;
  }

  private static String replaceVariables(String template, Properties properties) {
    // 1. replace with values and markers
    Pattern pattern = Pattern.compile("\\$([\\w\\._-]+)\\$");
    Matcher matcher = pattern.matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = properties.getProperty(key, "").trim();
      if (!value.isEmpty()) {
        value = value.replace("\\", "\\\\");
        value = value.replace("$", "\\$");
        value = value + MARKER;
      }
      matcher.appendReplacement(result, value);
    }
    matcher.appendTail(result);

    // 2. remove  all "{{ ... }}" sections that don't contain a marker, else remove braces
    int index = 0;
    while (true) {
      int start = result.indexOf(OPEN_BRACE, index);
      if (start < 0)
        break;
      int end = result.indexOf(CLOSE_BRACE, start + 1);
      if (end < 0)
        break;
      int marker = result.indexOf(MARKER, start + 1);
      if (marker < 0 || marker > end) {
        // marker not found in "{{ .. }}" - remove all block
        result.delete(start, end + CLOSE_BRACE_LENGTH);
        index = start;
      } else {
        // marker is found - remove only braces
        result.delete(start, start + OPEN_BRACE_LENGTH);
        index = end - OPEN_BRACE_LENGTH;
        result.delete(index, index + CLOSE_BRACE_LENGTH);
      }
    }

    // 3. Remove all markers
    index = 0;
    while (true) {
      int start = result.indexOf(MARKER, index);
      if (start < 0)
        break;
      result.delete(start, start + MARKER_LENGTH);
      index = start;
    }

    return result.toString();
  }


  private static final class About extends JDialog {
    private static final Rectangle TEXT_BLOCK_BOUNDS = new Rectangle(22, 113, 342, 157);

    private final Properties myProperties;
    private Image myImage;
    private final static int IMAGE_HEIGHT = 290;
    private final static int IMAGE_WIDTH = 386;
    private final List<Component> myAdjustableFontComponents = Collections15.arrayList();

    public About(JFrame owner, Properties properties) {
      super(owner);
      myProperties = properties;
    }

    public void initialize() {
      // first, access the image, then initialize everything else
      myImage = Icons.ABOUT_BOX_BACKGROUND.getImage();
      assert myImage.getHeight(null) == -1 || myImage.getHeight(null) == IMAGE_HEIGHT;
      assert myImage.getWidth(null) == -1 || myImage.getWidth(null) == IMAGE_WIDTH;
      try {
        setup();
        JPanel textBlock = makeTextBlock();
        adjustFonts(textBlock, new Dimension(TEXT_BLOCK_BOUNDS.width, TEXT_BLOCK_BOUNDS.height));
        JPanel contentPanel = makeContentPanel(textBlock);
        JComponent picture = makeBackground(contentPanel);
        JPanel mainPanel = makeMainPanel(contentPanel, picture);
        getContentPane().add(mainPanel);
        addClosingListeners(mainPanel);
//        runAfterInitialization.run();
      } catch (Exception e) {
        Log.error(e);
      }
    }

    private void adjustFonts(JPanel measurable, Dimension target) {
      Dimension lastSize = null;
      while (true) {
        Dimension size = measurable.getPreferredSize();
        if (size.width <= target.width && size.height <= target.height)
          return;
        if (lastSize != null && (size.width >= lastSize.width || size.height >= lastSize.height))
          return;
        lastSize = size;

        for (Component component : myAdjustableFontComponents) UIUtil.adjustFont(component, 0.9F, -1, true);
      }
    }

    private void addClosingListeners(JPanel mainPanel) {
      addWindowFocusListener(new WindowFocusListener() {
        public void windowGainedFocus(WindowEvent e) {
        }

        public void windowLostFocus(WindowEvent e) {
          setVisible(false);
        }
      });
      mainPanel.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
          setVisible(false);
        }
      });
      addKeyListener(new KeyAdapter() {
        public void keyReleased(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiersEx() == 0)
            setVisible(false);
        }
      });
    }

    private void setup() {
      setTitle(L.dialog(replaceVariables("About $productName$", myProperties)));
      setResizable(false);
      setUndecorated(true);
      updateBounds();
    }

    private void updateBounds() {
      Rectangle owner = getOwner().getBounds();
      int x = owner.x + owner.width / 2 - IMAGE_WIDTH / 2;
      int y = owner.y + owner.height / 2 - IMAGE_HEIGHT / 2;
      setBounds(x, y, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private JPanel makeMainPanel(final JPanel contentPanel, JComponent picture) {
      JPanel mainPanel = new JPanel();
      OverlayLayout overlay = new OverlayLayout(mainPanel);
      mainPanel.setLayout(overlay);
      mainPanel.add(contentPanel);
      mainPanel.add(picture);
      return mainPanel;
    }

    private JComponent makeBackground(JPanel contentPanel) {
      final ImageObserver observer = makeImageObserver(contentPanel);
      return new JComponent() {
        {
          setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
          Graphics g = graphics.create();
          AwtUtil.applyRenderingHints(g);
          try {
            g.drawImage(myImage, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, observer);
          } finally {
            g.dispose();
          }
        }
      };
    }

    private ImageObserver makeImageObserver(final JPanel contentPanel) {
      return (img, infoflags, x, y, width, height) -> {
        if ((infoflags & ImageObserver.ALLBITS) == ImageObserver.ALLBITS) {
          contentPanel.repaint();
          return false;
        } else {
          return true;
        }
      };
    }

    private JPanel makeContentPanel(JPanel textBlock) {
      final JPanel contentPanel = new JPanel();
      contentPanel.setBorder(new LineBorder(Color.BLACK, 1));
      contentPanel.setLayout(new AboutContentLayout());
      contentPanel.setOpaque(false);
      contentPanel.add(textBlock);
      return contentPanel;
    }

    private JPanel makeTextBlock() {
      JPanel textBlock = new JPanel(new BorderLayout(4, 4));
      textBlock.setOpaque(false);
      textBlock.setName(TEXT_BLOCK);
      textBlock.add(makeAboutLabel(), BorderLayout.CENTER);
      textBlock.add(makeAlmworksLink(), BorderLayout.SOUTH);
      textBlock.add(makeBuildBox(), BorderLayout.EAST);
      textBlock.setBorder(new EmptyBorder(3, 3, 3, 3));
      return textBlock;
    }

    private JPanel makeBuildBox() {
      JPanel buildInfo = new JPanel(new BorderLayout(2, 2));
      buildInfo.add(makeBuildBoxLabel("<html><body>Build " + myProperties.getProperty("buildNumber") + "<br>" +
        myProperties.getProperty("buildDate")), BorderLayout.NORTH);
      buildInfo.setOpaque(false);
      return buildInfo;
    }

    private URLLink makeAlmworksLink() {
      URLLink link = new URLLink(Setup.URL_ALMWORKS, true).leftAligned();
      UIUtil.adjustFont(link, -1, Font.BOLD, false);
      if (Setup.isJiraClient()) {
        link.setForeground(new Color(0x444ea2));
        link.setHoverColor(new Color(0x5967d5));
        link.setPressedColor(new Color(0x7e87d6));
      } else {
        link.setForeground(new Color(0xF9FF99));
        link.setHoverColor(new Color(0xF9FDB8));
        link.setPressedColor(new Color(0xFFFFFF));
      }
      myAdjustableFontComponents.add(link);
      return link;
    }

    private JLabel makeAboutLabel() {
      final ALabel aboutText = new ALabel();
      String template = FileUtil.loadTextResource("com/almworks/gui/About.html", AboutDialog.class.getClassLoader());
      aboutText.setText(replaceVariables(template, myProperties));
      aboutText.setForeground(FOREGROUND_COLOR);
      aboutText.setAlignmentY(0.0F);
      aboutText.setAlignmentX(0.0F);
      aboutText.setVerticalAlignment(SwingConstants.TOP);
      myAdjustableFontComponents.add(aboutText);
      return aboutText;
    }

    private JLabel makeBuildBoxLabel(String text) {
      ALabel label = new ALabel(text);
      label.setForeground(FOREGROUND_COLOR);
      label.setHorizontalAlignment(SwingConstants.RIGHT);
      UIUtil.adjustFont(label, -1, Font.BOLD, false);
      myAdjustableFontComponents.add(label);
      return label;
    }

    private static class AboutContentLayout implements LayoutManager2 {
      private final Dimension myContainerDimensions = new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT);

      public float getLayoutAlignmentX(java.awt.Container target) {
        return 0;
      }

      public float getLayoutAlignmentY(java.awt.Container target) {
        return 0;
      }

      public void invalidateLayout(java.awt.Container target) {
      }

      public Dimension maximumLayoutSize(java.awt.Container target) {
        return myContainerDimensions;
      }

      public void addLayoutComponent(Component comp, Object constraints) {
      }

      public void removeLayoutComponent(Component comp) {
      }

      public void layoutContainer(java.awt.Container parent) {
        Component[] components = parent.getComponents();
        for (Component c : components) {
          JComponent component = (JComponent) c;
          String name = component.getName();
          if (TEXT_BLOCK.equals(name)) {
            component.setBounds(TEXT_BLOCK_BOUNDS);
          }
        }
      }

      public void addLayoutComponent(String name, Component comp) {
      }

      public Dimension minimumLayoutSize(java.awt.Container parent) {
        return myContainerDimensions;
      }

      public Dimension preferredLayoutSize(java.awt.Container parent) {
        return myContainerDimensions;
      }
    }
  }
}
