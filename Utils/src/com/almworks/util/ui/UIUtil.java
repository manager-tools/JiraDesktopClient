package com.almworks.util.ui;

import com.almworks.util.Env;
import com.almworks.util.ErrorHunt;
import com.almworks.util.L;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AComboboxModel;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.*;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.*;
import com.almworks.util.components.plaf.LinkUI;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.macosx.AquaPopupMenuAdapter;
import com.almworks.util.components.recent.AddRecentFromComboBox;
import com.almworks.util.components.recent.RecentController;
import com.almworks.util.components.recent.UnwrapCombo;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.config.ConfigAccessors;
import com.almworks.util.config.Configuration;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.Icons;
import com.almworks.util.io.IOUtils;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.text.TextUtil;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.actions.dnd.DndUtil;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.almworks.util.Collections15;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.swing.FocusManager;
import javax.swing.Timer;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.*;
import javax.swing.plaf.TextUI;
import javax.swing.table.TableModel;
import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.*;

/**
 * @author : Dyoma
 */
public class UIUtil {
  public static final Convertor<Component, String> COMPONENT_NAME = new Convertor<Component, String>() {
    public String convert(Component component) {
      return component.getName();
    }
  };
  /**
   * Typical gap for layout managers.
   */
  public static final int GAP = 5;
  private static Color ourDefaultBackgroundColor;
  private static final int DEFAULT_FONT_HEIGHT = 9; // todo ?
  private static final int DEFAULT_FONT_WIDTH = 7; // todo ?
  public static final Rectangle RECT_0011 = new Rectangle(0, 0, 1, 1);
  public static final Rectangle RECT_0000 = new Rectangle(0, 0, 0, 0);
  public static final Border BORDER_1 = new EmptyBorder(1, 1, 1, 1);
  public static final Border BORDER_9 = new EmptyBorder(9, 9, 9, 9);
  public static final Border BORDER_5 = new EmptyBorder(5, 5, 5, 5);

  /**
   * The key of the client property used to alter the behavior
   * of {@link #setDefaultLabelAlignment(javax.swing.JComponent)} on JLabels.
   */
  public static final String SET_DEFAULT_LABEL_ALIGNMENT = "UIUtil.setDefaultLabelAlignment";

  public static final int EDITOR_PANEL_BORDER_WIDTH = Aqua.isAqua() ? 9 : 5;
  /**
   * The border used around white web-like panels in the issue editor
   * and WorkflowActionArtifactEditor-based dialogs.
   */
  public static final Border EDITOR_PANEL_BORDER = new EmptyBorder(EDITOR_PANEL_BORDER_WIDTH, EDITOR_PANEL_BORDER_WIDTH, EDITOR_PANEL_BORDER_WIDTH, EDITOR_PANEL_BORDER_WIDTH);

  public static final int SPLITPANE_DIVIDER_GRANULARITY = 1000;

  private static final int MAX_NOTICE_POPUP_WIDTH = 400;

  private static Dimension ICON_BUTTON_PREF_SIZE;

  private static AListModel<TimeZone> ourTimezonesModel;
  public static final CanvasRenderer<TimeZone> TIMEZONE_RENDERER = new CanvasRenderer<TimeZone>() {
    public void renderStateOn(CellState state, Canvas canvas, TimeZone item) {
      int offset = item.getRawOffset();
      StringBuilder s = new StringBuilder();
      if (offset != 0) {
        s.append('(');
        if (offset < 0) {
          s.append('-');
          offset = -offset;
        } else {
          s.append('+');
        }
        int hours = (int) (offset / Const.HOUR);
        if (hours < 10)
          s.append('0');
        s.append(hours);
        s.append(':');
        int minutes = (int) ((offset % Const.HOUR) / Const.MINUTE);
        if (minutes < 10)
          s.append('0');
        s.append(minutes);
        s.append(") ");
      }
      String displayName = item.getDisplayName(false, TimeZone.SHORT, Locale.US);
      s.append(displayName);
      String id = item.getID();
      if (!id.equals(displayName)) {
        s.append(" - ");
        s.append(id);
      }
      canvas.appendText(s.toString());
    }
  };

  @Deprecated
  public static Detach addComponentListener(final Component component, final ComponentListener listener) {
    component.addComponentListener(listener);
    return new Detach() {
      protected void doDetach() {
        component.removeComponentListener(listener);
      }
    };
  }

  public static void addComponentListener(Lifespan life, final Component component, final ComponentListener listener) {
    if (life.isEnded())
      return;
    life.add(addComponentListener(component, listener));
  }

  public static Detach addWindowListener(final Window window, final WindowListener listener) {
    window.addWindowListener(listener);
    return new Detach() {
      protected void doDetach() {
        window.removeWindowListener(listener);
      }
    };
  }

  public static void addWindowStateListener(Lifespan life, final Window window, final WindowStateListener listener) {
    if (life.isEnded())
      return;
    window.addWindowStateListener(listener);
    life.add(new Detach() {
      protected void doDetach() {
        window.removeWindowStateListener(listener);
      }
    });
  }

  public static void addWindowFocusListener(Lifespan life, final Window window, final WindowFocusListener listener) {
    if(life.isEnded()) {
      return;
    }
    window.addWindowFocusListener(listener);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        window.removeWindowFocusListener(listener);
      }
    });
  }

  public static void addListDataListener(Lifespan life, final ListModel model, final ListDataListener listener) {
    if (life.isEnded())
      return;
    life.add(addListDataListener(model, listener));
  }

  public static Detach addListDataListener(final ListModel model, final ListDataListener listener) {
    model.addListDataListener(listener);
    return new Detach() {
      protected void doDetach() {
        model.removeListDataListener(listener);
      }
    };
  }

  /**
   * Creates default BorderLayout
   */
  public static BorderLayout createBorderLayout() {
    return new BorderLayout(GAP, GAP);
  }


  public static Border createEastBevel(Color backgroundColor) {
    return new CompoundBorder(new BrokenLineBorder(backgroundColor.brighter(), 1, BrokenLineBorder.EAST),
      new BrokenLineBorder(backgroundColor.darker(), 1, BrokenLineBorder.EAST));
  }

  public static Border createWestBevel(Color backgroundColor) {
    return new CompoundBorder(new BrokenLineBorder(backgroundColor.darker(), 1, BrokenLineBorder.WEST),
      new BrokenLineBorder(backgroundColor.brighter(), 1, BrokenLineBorder.WEST));
  }

  public static JLabel createMessage(String message) {
    JLabel label = new JLabel(message);
    label.setHorizontalTextPosition(SwingConstants.CENTER);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setVerticalTextPosition(SwingConstants.CENTER);
    return label;
  }

  public static Border createNorthBevel(Color backgroundColor) {
    return new CompoundBorder(new BrokenLineBorder(backgroundColor.darker(), 1, BrokenLineBorder.NORTH),
      new BrokenLineBorder(backgroundColor.brighter(), 1, BrokenLineBorder.NORTH));
  }

  public static Border createSouthBevel(Color backgroundColor) {
    if (backgroundColor == null)
      backgroundColor = getDefaultBackgroundColor();
    return new CompoundBorder(new BrokenLineBorder(backgroundColor.brighter(), 1, BrokenLineBorder.SOUTH),
      new BrokenLineBorder(backgroundColor.darker(), 1, BrokenLineBorder.SOUTH));
  }

  public static Color getEditorBackground() {
    return UIManager.getColor("EditorPane.background");
  }

  public static Color getEditorForeground() {
    return UIManager.getColor("EditorPane.foreground");
  }

  public static Color getEditorInactiveBackground() {
    return UIManager.getColor("TextField.inactiveBackground");
  }


  private static Color getDefaultBackgroundColor() {
/*
    if (ourDefaultBackgroundColor != null)
      return ourDefaultBackgroundColor;
    synchronized (UIUtil.class) {
      if (ourDefaultBackgroundColor != null)
        return ourDefaultBackgroundColor;
      ourDefaultBackgroundColor = UIManager.getColor("Panel.background");
      if (ourDefaultBackgroundColor == null) {
        Log.warn("no color for panel");
        ourDefaultBackgroundColor = Color.LIGHT_GRAY;
      }
    }
    return ourDefaultBackgroundColor;
*/
    ourDefaultBackgroundColor = UIManager.getColor("Panel.background");
    if (ourDefaultBackgroundColor == null) {
      Log.warn("no color for panel");
      ourDefaultBackgroundColor = Color.LIGHT_GRAY;
    }
    return ourDefaultBackgroundColor;
  }

  public static AdjustedSplitPane createSplitPane(
    JComponent leftTop, JComponent rightBottom, boolean verticalDividerOrientation,
    Configuration config, String settingName, int defaultDividerPos)
  {
    // todo add/remove listener on addNotify/removeNotify
    int orientation = verticalDividerOrientation ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
    final AdjustedSplitPane splitPane = new AdjustedSplitPane(orientation, leftTop, rightBottom);
    splitPane.setOneTouchExpandable(false);
    splitPane.setResizeWeight(0.5);
    final ConfigAccessors.Int position = ConfigAccessors.integer(config, defaultDividerPos, settingName);
    splitPane.setDividerLocation(position.getInt());
    splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        position.setInt(splitPane.getDividerLocation());
      }
    });
    return splitPane;
  }

  /**
   * Creates and returns an {@link AdjustedSplitPane}.
   * @param leftTop The left or top component.
   * @param rightBottom The right or bottom component.
   * @param verticalDividerOrientation {@code true} for vertical divider (horizontal split), {@code false} for
   * horizontal divider (vertical split).
   * @param configuration The configuration to load and save divider location.
   * @param settingName The setting name to load didiver and save divider location.
   * @param defaultDividerProportion Default divider location as a proportion, must be in [0.0d; 1.0d].
   * @param maxDefaultDividerLocation If > 0, this value is used as the maximum default divider location.
   * @return The split pane.
   */
  public static AdjustedSplitPane createSplitPane(JComponent leftTop, JComponent rightBottom,
    final boolean verticalDividerOrientation, Configuration configuration, String settingName,
    final double defaultDividerProportion, final int maxDefaultDividerLocation)
  {
    final int orientation = verticalDividerOrientation ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;

    final ConfigAccessors.Int locationAccessor = ConfigAccessors.integer(configuration, Integer.MIN_VALUE, settingName);
    final int location = locationAccessor.getInt();

    final AdjustedSplitPane splitPane;
    if(location != Integer.MIN_VALUE) {
      splitPane = new AdjustedSplitPane(orientation, leftTop, rightBottom);
      splitPane.setDividerLocation(location);
    } else {
      splitPane = new AdjustedSplitPane(orientation, leftTop, rightBottom) {
        boolean locationSet = false;
        @Override
        public void doLayout() {
          if(!locationSet) {
            if(maxDefaultDividerLocation > 0) {
              final int size = (verticalDividerOrientation ? getWidth() : getHeight()) - getDividerSize();
              setDividerLocation(Math.min((int)(size * defaultDividerProportion), maxDefaultDividerLocation));
            } else {
              setDividerLocation(defaultDividerProportion);
            }
            locationSet = true;
          }
          super.doLayout();
        }
      };
    }

    splitPane.setOneTouchExpandable(false);
    splitPane.setResizeWeight(0.5);

    // todo add/remove listener on addNotify/removeNotify
    splitPane.addPropertyChangeListener(
      JSplitPane.DIVIDER_LOCATION_PROPERTY,
      new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          locationAccessor.setInt(splitPane.getDividerLocation());
        }
      });

    return splitPane;
  }


  public static JTabbedPane createTabbedPane(String[] names, Component[] components, Configuration config,
    String settingName, int defaultTab)
  {
    // todo add/remove listener on addNotify/removeNotify
    assert names.length == components.length;
    assert names.length > 0;
    final JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
    for (int i = 0; i < names.length; i++) {
      NameMnemonic nameMnemonic = NameMnemonic.parseString(names[i]);
      pane.addTab(nameMnemonic.getText(), components[i]);
      int tabIndex = pane.getTabCount() - 1;
      pane.setMnemonicAt(tabIndex, nameMnemonic.getMnemonicIndex());
    }
    final ConfigAccessors.Int selectedTab = ConfigAccessors.integer(config, defaultTab, settingName);
    int tab = Math.min(Math.max(selectedTab.getInt(), 0), names.length);
    pane.setSelectedIndex(tab);
    pane.getModel().addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        selectedTab.setInt(pane.getSelectedIndex());
      }
    });
    return pane;
  }


  public static Icon getIcon(String resourceUrl) {
    try {
      return new ImageIcon(loadImage(UIUtil.class.getClassLoader().getResourceAsStream(resourceUrl)));
    } catch (IOException e) {
      return EmptyIcon.ZERO_SIZE;
    }
  }

  @Nullable
  public static <T> T getImplementor(Object element, Class<? extends T> aClass) {
    T cast = Util.castNullable(aClass, element);
    if (cast != null) return cast;
    else
      return element instanceof ObjectWrapper<?> ?
        getImplementor(((ObjectWrapper<?>) element).getUserObject(), aClass) : null;
  }

  public static int getLineHeight(JComponent component) {
    if (component != null) {
      Font font = component.getFont();
      if (font != null) {
        FontMetrics fontMetrics;
        try {
          fontMetrics = component.getFontMetrics(font);
          return fontMetrics.getHeight();
        } catch (NullPointerException e) {
          // happens when component is not displayable
        }
      }
    }
    return DEFAULT_FONT_HEIGHT;
  }

  public static int getColumnWidth(JComponent component) {
    if (component != null) {
      Font font = component.getFont();
      if (font != null) {
        try {
          return component.getFontMetrics(font).charWidth('m');
        } catch (NullPointerException e) {
          // happens when component is not displayable
        }
      }
    }
    return DEFAULT_FONT_WIDTH;
  }

  public static Dimension getRelativeDimension(JComponent component, int columns, int rows) {
    return new Dimension(getColumnWidth(component) * columns, getLineHeight(component) * rows);
  }

  public static Dimension getRelativeDimensionWithBorder(JComponent component, int columns, int rows) {
    final Dimension size = getRelativeDimension(component, columns, rows);
    final Border border = component.getBorder();
    if(border != null) {
      final Insets insets = border.getBorderInsets(component);
      if(insets != null) {
        AwtUtil.addInsets(size, insets);
      }
    }
    return size;
  }

  public static TreePath getPathToRoot(TreeNode node) {
    return new TreePath(getPathToRoot(node, 0));
  }

  /**
   * @return sorted IntArray of selected indexes
   */
  public static IntArray getSelectedIndices(ListSelectionModel selectionModel) {
    if (selectionModel != null) {
      int iMin = selectionModel.getMinSelectionIndex();
      int iMax = selectionModel.getMaxSelectionIndex();
      if ((iMin == -1) || (iMax == -1)) return new IntArray();
      IntArray result = new IntArray();
      for (int i = iMin; i <= iMax; i++) {
        if (selectionModel.isSelectedIndex(i)) result.add(i);
      }
      return result;
    }
    return new IntArray();
  }

  public static int getSelectionCount(ListSelectionModel selectionModel) {
    if (selectionModel != null) {
      int iMin = selectionModel.getMinSelectionIndex();
      int iMax = selectionModel.getMaxSelectionIndex();
      int count = 0;

      for (int i = iMin; i <= iMax; i++) {
        if (selectionModel.isSelectedIndex(i)) {
          count++;
        }
      }
      return count;
    }
    return 0;
  }

  public static String getStringPresentation(KeyStroke keyStroke) {
    String modifiersText = KeyEvent.getModifiersExText(keyStroke.getModifiers());
    return modifiersText + (!modifiersText.isEmpty() ? " - " : "") + KeyEvent.getKeyText(keyStroke.getKeyCode());
  }

  public static JComponent labelComponentHorizontal(String textWithMnemonic, JComponent component) {
    NameMnemonic mnemonic = NameMnemonic.parseString(textWithMnemonic);
    JPanel panel = new JPanel(new InlineLayout(InlineLayout.HORISONTAL, 4, false).setLastTakesAllSpace(true));
    JLabel label = new JLabel();
    mnemonic.setToLabel(label);
    panel.add(label);
    panel.add(component);
    label.setLabelFor(component);
    return panel;
  }

  public static Image loadImage(InputStream stream) throws IOException {
    return Toolkit.getDefaultToolkit().createImage(FileUtil.readBytes(stream));
  }

  public static JComponent placeOnTop(JComponent component) {
    JPanel panel = new JPanel(UIUtil.createBorderLayout());
    panel.add(component, BorderLayout.NORTH);
    return panel;
  }

  public static void scrollSelectionToView(JTextComponent component) {
    TextUI ui = component.getUI();
    try {
      Rectangle start = ui.modelToView(component, component.getSelectionStart(), Position.Bias.Forward);
      Rectangle end = ui.modelToView(component, component.getSelectionEnd(), Position.Bias.Backward);
      ensureRectVisiblePartially(component, start.union(end));
    } catch (BadLocationException e) {
      // ignore
    }
  }

  public static void ensureRectVisiblePartially(JComponent component, Rectangle rect) {
    if(component == null || component instanceof JViewport) {
      assert false;
      return;
    }

    if(!component.isShowing()) {
      return;
    }

    final JViewport viewport = SwingTreeUtil.findAncestorOfType(component, JViewport.class);
    if (viewport == null) {
      return;
    }

    final Component view = viewport.getView();
    final Rectangle viewRect = viewport.getViewRect();
    if(view == null || viewRect == null) {
      return;
    }

    final int v0 = viewRect.y;
    final int v1 = v0 + viewRect.height;

    int y0 = rect.y;
    if(view != component) {
      y0 = SwingUtilities.convertPoint(component, 0, y0, view).y;
    }
    final int y1 = y0 + rect.height;

    if(y0 >= v0 && y1 <= v1) {
      // completely visible
      return;
    }

    if(y1 > v0 && y0 < v1) {
      // at least partially displayed
      if(rect.height > viewRect.height) {
        // too large to fit
        return;
      }
    }

    final int newY;
    if(y0 <= v0) {
      // scroll backward -- will appear at the top
      newY = y0;
    } else {
      // scroll forward -- will appear at the bottom
      assert y1 >= v1 : y0 + " " + viewRect;
      newY = Math.max(0, y1 - viewRect.height);
    }
    
    // todo smooth scrolling
    viewport.setViewPosition(new Point(viewRect.x, newY));
  }

  public static void setDocument(Lifespan life, final JTextComponent textField, Document document) {
    if (life.isEnded())
      return;
    textField.setDocument(document);
    if (life != Lifespan.FOREVER)
      life.add(new Detach() {
        protected void doDetach() {
          textField.setDocument(new PlainDocument());
        }
      });
  }

  public static void setStyledDocument(Lifespan life, final JTextPane pane, StyledDocument document) {
    if (life.isEnded())
      return;
    pane.setDocument(document);
    if (life != Lifespan.FOREVER)
      life.add(new Detach() {
        protected void doDetach() {
          pane.setDocument(new DefaultStyledDocument());
        }
      });
  }

  /**
   * This method doesn't sets component text iff component text is equal to newText.<br>
   * This method is to avoid problems like <a href="http://bugzilla/main/show_bug.cgi?id=853">853</a>
   */
  public static void setFieldText(JTextComponent component, String newText) {
    String oldText = component.getText();
    if (oldText.equals(newText))
      return;
    component.setText(newText);
  }

  public static Detach setupButtonGroup(final ButtonGroup buttonGroup, final Configuration configuration,
    final String setting)
  {
    int count = buttonGroup.getButtonCount();
    int option = configuration.getIntegerSetting(setting, 0);
    if (option < 0)
      option = 0;
    if (option >= count)
      option = count - 1;

    // first set selection, then add listeners
    Enumeration<AbstractButton> buttons = buttonGroup.getElements();
    int i = 0;
    while (buttons.hasMoreElements()) {
      AbstractButton button = buttons.nextElement();
      if (i == option) {
        button.setSelected(true);
        break;
      }
      i++;
    }

    final ItemListener listener = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        Enumeration<AbstractButton> buttons = buttonGroup.getElements();
        int i = 0;
        while (buttons.hasMoreElements()) {
          AbstractButton button = buttons.nextElement();
          if (button.isSelected()) {
            configuration.setSetting(setting, i);
            break;
          }
          i++;
        }
      }
    };

    DetachComposite detach = new DetachComposite();
    buttons = buttonGroup.getElements();
    while (buttons.hasMoreElements()) {
      final AbstractButton button = buttons.nextElement();
      button.addItemListener(listener);
      detach.add(new Detach() {
        protected void doDetach() {
          button.removeItemListener(listener);
        }
      });
    }

    return detach;
  }

  public static Rectangle getScreenUserSize(GraphicsConfiguration gc) {
    Rectangle screenBounds = gc.getBounds();
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
    if (insets != null) {
      screenBounds.x += insets.left;
      screenBounds.y += insets.top;
      screenBounds.width -= insets.left + insets.right;
      screenBounds.height -= insets.top + insets.bottom;
    }
    return screenBounds;
  }

  public static void transferFocus(JToggleButton from, final JComponent to) {
    from.getAccessibleContext().addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (!AccessibleContext.ACCESSIBLE_STATE_PROPERTY.equals(evt.getPropertyName()))
          return;
        if (!AccessibleState.SELECTED.equals(evt.getNewValue()))
          return;
        if (to.isDisplayable() && to.isEnabled())
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              to.requestFocusInWindow();
            }
          });
      }
    });
  }

  public static Color transformColor(Color color, float redMultiplier, float greenMultiplier, float blueMultiplier) {
    int red = Math.max(Math.min((int) (color.getRed() * redMultiplier), 255), 0);
    int green = Math.max(Math.min((int) (color.getGreen() * greenMultiplier), 255), 0);
    int blue = Math.max(Math.min((int) (color.getBlue() * blueMultiplier), 255), 0);
    return new Color(red, green, blue);
  }

  public static String unsafeGetDocumentNewText(DocumentEvent event) {
    try {
      return event.getDocument().getText(event.getOffset(), event.getLength());
    } catch (BadLocationException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Nullable
  private static TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
    TreeNode[] retNodes;
    if (aNode == null) {
      if (depth == 0)
        return null;
      else
        retNodes = new TreeNode[depth];
    } else {
      depth++;
      retNodes = getPathToRoot(aNode.getParent(), depth);
      assert retNodes != null : depth;
      retNodes[retNodes.length - depth] = aNode;
    }
    return retNodes;
  }

  public static Detach addActionListener(final AbstractButton button, final ActionListener listener) {
    button.addActionListener(listener);
    return new Detach() {
      protected void doDetach() {
        button.removeActionListener(listener);
      }
    };
  }

  public static Detach addActionListener(final ComboBoxEditor editor, final ActionListener listener) {
    editor.addActionListener(listener);
    return new Detach() {
      protected void doDetach() {
        editor.removeActionListener(listener);
      }
    };
  }

  public static void addActionListener(Lifespan lifespan, final JComboBox combobox, final ActionListener listener) {
    if (!lifespan.isEnded()) {
      combobox.addActionListener(listener);
      lifespan.add(new Detach() {
        protected void doDetach() {
          combobox.removeActionListener(listener);
        }
      });
    }
  }

  public static void addActionListener(Lifespan lifespan, final AbstractButton button, final ActionListener listener) {
    if (!lifespan.isEnded()) {
      button.addActionListener(listener);
      lifespan.add(new Detach() {
        protected void doDetach() {
          button.removeActionListener(listener);
        }
      });
    }
  }

  public static int getDefaultCloseOperation(Window window) {
    int defaultCloseOperation;
    if (window instanceof JFrame) {
      defaultCloseOperation = ((JFrame) window).getDefaultCloseOperation();
    } else if (window instanceof JDialog) {
      defaultCloseOperation = ((JDialog) window).getDefaultCloseOperation();
    } else
      defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE;
    return defaultCloseOperation;
  }

  public static <C extends Component> C adjustFont(C component, float factor, int style) {
    return adjustFont(component, factor, style, false);
  }


  /**
   * @param antialias deprecated: for proper antialising, use {@link AwtUtil#applyRenderingHints}
   */
  public static <C extends Component> C adjustFont(C component, float factor, int style, @Deprecated boolean antialias) {
    return adjustFont(component, factor, style, antialias, 1);
  }

  /**
   * @param antialias deprecated: for proper antialising, use {@link AwtUtil#applyRenderingHints}
   */
  public static <C extends Component> C adjustFont(C component, float factor, int style, @Deprecated boolean antialias,
    int minSize)
  {
    Font font = component.getFont();
    if (font == null) {
      assert false : component + " has null font, maybe it is not displayable?";
      return component;
    }
    if (factor > 0 && Math.abs(factor - 1) > 0.001F) {
      int newSize = Math.round(font.getSize2D() * factor);
      if (newSize == font.getSize())
        newSize = factor > 1 ? newSize + 1 : newSize - 1;
      newSize = Math.max(minSize, newSize);
      font = font.deriveFont((float) newSize);
    }
    if (style >= 0)
      font = font.deriveFont(style);
    component.setFont(font);
    if (antialias)
      if (component instanceof Antialiasable)
        ((Antialiasable) component).setAntialiased(true);
      else
        Log.warn("antialiasing is not supported by " + component, new UnsupportedOperationException());
    return component;
  }

  public static Window getDefaultDialogOwner() {
    Window focusedWindow = FocusManager.getCurrentManager().getFocusedWindow();
    if (focusedWindow != null && focusedWindow.isDisplayable() && focusedWindow.isVisible() && (focusedWindow instanceof RootPaneContainer))
      return focusedWindow;
    return JOptionPane.getRootFrame();
  }

  public static Detach bindCheckBox(final JCheckBox checkbox, final ConfigAccessors.Bool accessor) {
    checkbox.setSelected(accessor.getBool());
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        accessor.setBool(checkbox.isSelected());
      }
    };
    checkbox.addActionListener(listener);
    return new Detach() {
      protected void doDetach() {
        checkbox.removeActionListener(listener);
      }
    };
  }

  public static void setupTransparentPanel(JPanel panel) {
    panel.setOpaque(false);
    panel.setBorder(AwtUtil.EMPTY_BORDER);
  }

  public static void copyToClipboard(String content) {
    DndUtil.copyToClipboard(new StringSelection(content));
  }

  public static void copyToClipboard(Image image) {
    DndUtil.copyToClipboard(new TransferableImage(image));
  }

  public static Box createCenteringBox(int orientation, JComponent component) {
    Box box = new Box(orientation);
    box.add(createGlue(orientation));
    box.add(component);
    box.add(createGlue(orientation));
    return box;
  }

  private static Component createGlue(int orientation) {
    return orientation == BoxLayout.X_AXIS ? Box.createHorizontalGlue() : Box.createVerticalGlue();
  }

  public static Box createCenteringBox(int orientation, JComponent[] components) {
    Box box = new Box(orientation);
    box.add(createGlue(orientation));
    for (JComponent component : components)
      box.add(component);
    box.add(createGlue(orientation));
    return box;
  }

  public static <T extends Component> boolean visitComponents(Component parent, Class<T> wantedClass,
    ElementVisitor<T> visitor)
  {
    boolean proceed = true;
    if (wantedClass.isInstance(parent))
      proceed = visitor.visit((T) parent);
    if (proceed && (parent instanceof Container)) {
      Container container = ((Container) parent);
      for (int i = 0; i < container.getComponentCount(); i++) {
        proceed = visitComponents(container.getComponent(i), wantedClass, visitor);
        if (!proceed)
          break;
      }
    }
    return proceed;
  }

  @Nullable
  public static Component findComponentByName(Component parent, String name) {
    if (Util.equals(parent.getName(), name))
      return parent;
    if (parent instanceof Container) {
      Container container = ((Container) parent);
      for (int i = 0; i < container.getComponentCount(); i++) {
        Component result = findComponentByName(container.getComponent(i), name);
        if (result != null)
          return result;
      }
    }
    return null;
  }

  public static <T extends Component> T firstDescendantOfType(Component parent, Class<T> type) {
    final Object[] result = { null };
    visitComponents(parent, type, new ElementVisitor<T>() {
      @Override
      public boolean visit(T element) {
        result[0] = element;
        return false;
      }
    });
    return (T)result[0];
  }

  public static <T1 extends Component, T2 extends Component> boolean visitComponents2(Component parent,
    final Class<T1> wanted1, final Class<T2> wanted2, final PairVisitor<T1, T2> visitor)
  {

    return visitComponents(parent, Component.class, new ElementVisitor<Component>() {
      public boolean visit(Component component) {
        boolean proceed = false;
        if (wanted1.isInstance(component))
          //noinspection ConstantConditions
          proceed = proceed && visitor.visitFirst((T1) component);
        if (wanted2.isInstance(component))
          proceed = proceed && visitor.visitSecond((T2) component);
        return proceed;
      }
    });
  }

  public static Detach setupConditionalVisibility(final AbstractButton condition, final Component dependant,
    final boolean inverse)
  {
    javax.swing.event.ChangeListener listener = new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        dependant.setVisible(condition.isSelected() ^ inverse);
        Container parent = dependant.getParent();
        if (parent instanceof JComponent)
          ((JComponent) parent).revalidate();
      }
    };
    Detach detach = addChangeListener(condition.getModel(), listener);
    listener.stateChanged(null);
    return detach;
  }

  public static Detach setupConditionalEnabled(final AbstractButton condition, final boolean inverse,
    final Component... dependants)
  {
    javax.swing.event.ChangeListener listener = new javax.swing.event.ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        final boolean enabled = (condition.isEnabled() && condition.isSelected()) ^ inverse;
        for (Component dependant : dependants) {
          visitComponents(dependant, Component.class, new ElementVisitor<Component>() {
            public boolean visit(Component component) {
              component.setEnabled(enabled);
              return true;
            }
          });
          dependant.repaint();
        }
      }
    };
    Detach detach = addChangeListener(condition.getModel(), listener);
    listener.stateChanged(null);
    return detach;
  }

  public static Detach addItemListener(final ItemSelectable selectable, final ItemListener listener) {
    selectable.addItemListener(listener);
    return new Detach() {
      protected void doDetach() {
        selectable.removeItemListener(listener);
      }
    };
  }

  public static void addChangeListener(Lifespan life, final ChangeListener listener, ButtonModel model) {
    life.add(addChangeListener(model, new javax.swing.event.ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        listener.onChange();
      }
    }));
  }

  @Deprecated
  /**
   * @deprecated replaced with {@link #addChangeListener(org.almworks.util.detach.Lifespan, com.almworks.util.collections.ChangeListener, javax.swing.ButtonModel)}
   */
  public static Detach addChangeListener(final ButtonModel model, final javax.swing.event.ChangeListener listener) {
    model.addChangeListener(listener);
    return new Detach() {
      protected void doDetach() {
        model.removeChangeListener(listener);
      }
    };
  }

  public static Detach addFocusListener(final Component component, final FocusListener listener) {
    component.addFocusListener(listener);
    return new Detach() {
      protected void doDetach() {
        component.removeFocusListener(listener);
      }
    };
  }

  public static void addFocusListener(Lifespan life, Component component, FocusListener listener) {
    if (life.isEnded()) return;
    life.add(addFocusListener(component, listener));
  }

  public static Detach keepSelectionOnRemove(final FlatCollectionComponent<?> component) {
    return component.getCollectionModel().addRemovedElementListener(new AListModel.RemovedElementsListener() {
      public void onBeforeElementsRemoved(AListModel.RemoveNotice elements) {
        SelectionAccessor<?> selection = component.getSelectionAccessor();
        AListModel<?> model = component.getCollectionModel();
        Collection<?> selectedToRemove = Containers.intersect(selection.getSelectedItems(), elements.getList());
        if (selectedToRemove.isEmpty())
          return;
        int[] indecies = model.indeciesOf(selectedToRemove);
        Arrays.sort(indecies);
        int notFirstIndex = 0;
        while (notFirstIndex < indecies.length && notFirstIndex == indecies[notFirstIndex])
          notFirstIndex++;
        if (notFirstIndex > 0)
          selection.addSelectionIndex(notFirstIndex);
        for (int i = notFirstIndex; i < indecies.length; i++) {
          int index = indecies[i];
//          selection.removeSelectionAt(index);
          if (index != notFirstIndex) {
            assert index > i;
            int newIndex = index - i - 1;
            if (!selection.isSelectedAt(newIndex))
              selection.addSelectionIndex(newIndex);
          }
        }
      }
    });
  }

  public static JScrollPane getScrollPaned(JComponent component) {
    JScrollPane scrollPane = new JScrollPane(ScrollablePanel.adapt(component), JScrollPane.VERTICAL_SCROLLBAR_NEVER,
      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setOpaque(false);
    scrollPane.getViewport().setOpaque(false);
    return scrollPane;
  }

  @Nullable
  public static Color getParentBackground(Component component) {
    if (component == null)
      return null;
    component = component.getParent();
    while (component != null) {
      if (component.isOpaque())
        return component.getBackground();
      component = component.getParent();
    }
    return null;
  }

  public static void addSwingPropertyChangeListener(Lifespan life, final Component component, final String name,
    final PropertyChangeListener listener)
  {
    if (life == Lifespan.NEVER)
      return;
    if (life == Lifespan.FOREVER) {
      component.addPropertyChangeListener(name, listener);
      return;
    }
    component.addPropertyChangeListener(name, listener);
    life.add(new Detach() {
      protected void doDetach() {
        component.removePropertyChangeListener(name, listener);
      }
    });
  }

  /**
   * Listens changes of text. If document of the component is changed the listener attaches to new document.
   */
  public static void addTextListener(Lifespan life, final JTextComponent component, final ChangeListener listener) {
    final Lifecycle documentCycle = new Lifecycle();
    final PropertyChangeListener propertyListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        documentCycle.cycle();
        DocumentUtil.addChangeListener(documentCycle.lifespan(), component.getDocument(), listener);
        listener.onChange();
      }
    };
    component.addPropertyChangeListener("document", propertyListener);
    propertyListener.propertyChange(null);
    life.add(new Detach() {
      protected void doDetach() {
        component.removePropertyChangeListener("document", propertyListener);
        documentCycle.dispose();
      }
    });
  }

  public static Detach listenURLLinkSource(final Link link, final JTextComponent textComponent) {
    return listenLinkSource(link, textComponent, new AbstractAction(link.getText()) {
      public void actionPerformed(ActionEvent e) {
        ExternalBrowser browser = new ExternalBrowser();
        browser.setUrl(textComponent.getText(), false);
        browser.setDialogHandler(L.dialog("Open Browser"), L.content("Failed to open browser"));
        browser.openBrowser();
      }
    }, StringChecker.IS_URL);
  }

  public static Detach listenLinkSource(final Link link, final JTextComponent textComponent, final Action action,
    final StringChecker stringChecker)
  {
    if (link == null || textComponent == null) {
      assert link != null;
      //noinspection ConstantConditions
      assert textComponent != null;
      return Detach.NOTHING;
    }
    link.setAction(action);
    link.setForeground(UIManager.getColor("Label.foreground"));
    link.setDisabledLook(new LinkUI.NormalPaint("Label.foreground"));
    DetachComposite result = new DetachComposite();

    addTextListener(result, textComponent, new ChangeListener() {
      public void onChange() {
        action.setEnabled(stringChecker.isTextValid(textComponent.getText()));
      }
    });
    result.add(new Detach() {
      protected void doDetach() {
        link.setAction(null);
      }
    });
    return result;
  }

  public static JLabel createLabelFor(JComponent component, String text) {
    NameMnemonic mnemonic = NameMnemonic.parseString(text);
    JLabel label = new JLabel();
    mnemonic.setToLabel(label);
    label.setLabelFor(component);
    label.setHorizontalAlignment(SwingConstants.LEADING);
    return label;
  }

  public static void setupLabelFor(Container parent) {
    final Map<String, JComponent> components = Collections15.hashMap();
    visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent c) {
        String name = c.getName();
        if (name != null && name.length() > 0)
          components.put(name, c);
        return true;
      }
    });
    visitComponents(parent, JLabel.class, new ElementVisitor<JLabel>() {
      public boolean visit(JLabel label) {
        String name = label.getName();
        if (name != null && name.startsWith("for:")) {
          name = name.substring(4);
          JComponent c = components.get(name);
          if (c != null)
            label.setLabelFor(c);
        }
        return true;
      }
    });
  }

  public static void setLabelMnemonics(Container parent) {
    visitComponents(parent, JLabel.class, new ElementVisitor<JLabel>() {
      public boolean visit(JLabel label) {
        String text = label.getText();
        NameMnemonic mnemonic = NameMnemonic.parseString(text);
        if (mnemonic.getMnemonicIndex() >= 0)
          mnemonic.setToLabel(label);
        return true;
      }
    });
  }

  public static TexturePaint createChequeredPaint() {
    Color c1 = getEditorBackground();
    Color c2 = ColorUtil.between(c1, getEditorForeground(), 0.07F);
    int size = 8;
    BufferedImage image = new BufferedImage(size * 2, size * 2, BufferedImage.TYPE_INT_BGR);
    Graphics g = image.getGraphics();
    g.setColor(c1);
    g.fillRect(0, 0, size, size);
    g.fillRect(size, size, size, size);
    g.setColor(c2);
    g.fillRect(0, size, size, size);
    g.fillRect(size, 0, size, size);

    return new TexturePaint(image, new Rectangle.Float(0, 0, size * 2, size * 2));
  }

  public static void addPopupTriggerListener(final FlatCollectionComponent<?> component) {
    component.getSwingComponent().addMouseListener(new MousePopupTrigger(component));
  }

  public static void adjustSelection(int row, SelectionAccessor<?> selectionAccessor) {
    if (row != -1 && !selectionAccessor.isSelectedAt(row))
      selectionAccessor.setSelectedIndex(row);
  }

  public static Dimension getPreferredSizePropped(Dimension preferredSize, int minWidth, int minHeight) {
    if (preferredSize == null)
      return new Dimension(minWidth, minHeight);
    if (preferredSize.width >= minWidth && preferredSize.height >= minHeight)
      return preferredSize;
    return new Dimension(Math.max(minWidth, preferredSize.width), Math.max(minHeight, preferredSize.height));
  }

  public static void addMouseListener(Lifespan life, final JComponent component, final MouseListener listener) {
    component.addMouseListener(listener);
    life.add(new Detach() {
      protected void doDetach() {
        component.removeMouseListener(listener);
      }
    });
  }

  public static void addKeyListener(Lifespan life, final JComponent component, final KeyListener listener) {
    component.addKeyListener(listener);
    life.add(new Detach() {
      protected void doDetach() {
        component.removeKeyListener(listener);
      }
    });
  }

  public static void addChangeListener(Lifespan life, final BoundedRangeModel model, final javax.swing.event.ChangeListener listener) {
    if (life == null || life.isEnded() || listener == null || model == null) return;
    model.addChangeListener(listener);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        model.removeChangeListener(listener);
      }
    });
  }

  /**
   * Returns JRootPane that is either contained in the component, or a parent of this component.
   * This is a fix-up for SwingUtilities.getRootPane(component) that allows to get pop-up root pane
   * from a focused pop-up panel.
   */
  public static JRootPane getRootPaneFixed(Component component) {
    if (component == null)
      return null;
    if (component instanceof Container) {
      Container container = ((Container) component);
      if (container.getComponentCount() == 1) {
        Component child = container.getComponent(0);
        if (child instanceof JRootPane) {
          return (JRootPane) child;
        }
      }
    }
    return SwingUtilities.getRootPane(component);
  }

  public static void scrollToTopDelayed(final JComponent component) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        component.scrollRectToVisible(UIUtil.RECT_0011);
      }
    });
  }

  public static void scrollToTop(JTextComponent component) {
    if (component == null)
      return;
    component.setCaretPosition(0);
    Container parent = component.getParent();
    if (parent instanceof JViewport) {
      ((JViewport) parent).setViewPosition(new Point(0, 0));
    }
  }

  public static void scrollToBottom(JTextComponent component) {
    if (component == null)
      return;
    int len = getDocumentText(component).length();
    component.setCaretPosition(len);
    Container parent = component.getParent();
    if (parent instanceof JViewport) {
      Point p = new Point(0, 0);
      if (len > 0) {
        try {
          Rectangle r = component.modelToView(len - 1);
          p.y = Math.max(0, r.y + r.height - parent.getHeight());
        } catch (BadLocationException e) {
          // ignore
        }
      }
      ((JViewport) parent).setViewPosition(p);
    }
  }


  public static CompoundBorder getToolbarEditorBorder() {
    LineBorder line = new LineBorder(AwtUtil.getPanelBackground().darker(), 1);
    return new CompoundBorder(line, new EmptyBorder(0, 2, 0, 2));
  }

  public static void setTextKeepView(JTextComponent component, String text) {
    Container parent = component.getParent();
    Point position = null;
    if (parent instanceof JViewport) {
      position = ((JViewport) parent).getViewPosition();
    }
    int caret = component.getCaretPosition();

    component.setText(text);

    if (caret >= 0 && caret <= component.getDocument().getLength()) {
      component.setCaretPosition(caret);
    } else {
      component.setCaretPosition(0);
    }
    if (position != null) {
      ((JViewport) parent).setViewPosition(position);
    }
  }

  public static void updateComponents(Component component, ComponentVisitor visitor) {
    assert visitor != null;
    Threads.assertAWTThread();
    visitor.processComponent(component);
    if (component instanceof Container) {
      Container container = ((Container) component);
      int count = container.getComponentCount();
      for (int i = 0; i < count; i++) {
        updateComponents(container.getComponent(i), visitor);
      }
    }
  }

  public static Dimension constrainDimension(Dimension dim, int width, int height) {
    Dimension result = null;
    if (dim != null) {
      if (dim.width > width || dim.height > height) {
        result = new Dimension(Math.min(dim.width, width), Math.min(dim.height, height));
      }
    }
    return result == null ? dim : result;
  }

  public static Dimension getDefaultScreenUserSize() {
    GraphicsEnvironment genv = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice device = genv.getDefaultScreenDevice();
    GraphicsConfiguration gconv = device.getDefaultConfiguration();
    Rectangle size = getScreenUserSize(gconv);
    return new Dimension(size.width, size.height);
  }

  @Nullable
  public static GraphicsConfiguration getGraphicsConfigurationForPoint(Point point, GraphicsDevice[] devices) {
    for (GraphicsDevice gd : devices) {
      try {
        GraphicsConfiguration gconf = gd.getDefaultConfiguration();
        if (gconf.getBounds().contains(point)) {
          return gconf;
        }
      } catch (Exception e) {
        // may catch IOOBE because displayes have changed
        Log.debug(e);
      }
    }
    return null;
  }

  @NotNull
  public static GraphicsConfiguration getGraphicsConfigurationForPoint(Point point) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice[] devices = ge.getScreenDevices();
    GraphicsConfiguration gc = getGraphicsConfigurationForPoint(point, devices);
    if (gc == null)
      gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
    return gc;
  }


  public static Point getCenterWindowLocation(Window window) {
    GraphicsConfiguration gconf = window.getGraphicsConfiguration();
    return getCenterRectangle(getScreenUserSize(gconf), window.getSize());
  }

  public static Point getCenterRectangle(Rectangle outter, Dimension rectangleSize) {
    int x = outter.x + Math.max(0, (outter.width - rectangleSize.width) / 2);
    int y = outter.y + Math.max(0, (outter.height - rectangleSize.height) / 2);
    return new Point(x, y);
  }

  public static void centerWindow(Window window) {
    window.setLocation(getCenterWindowLocation(window));
  }

  /**
   * Updates rect parameter to make it fully fit into screen rectangle.
   * This method keeps rectange size (tries to move but not resize) until the rectangle is too large to fit the screen
   */
  public static void fitScreen(Rectangle screen, Rectangle rect) {
    // Ensure rect is not large than screen (shink otherwise)
    rect.width = Math.min(screen.width, rect.width);
    rect.height = Math.min(screen.height, rect.height);
    // Ensure rect origin is inside the screen (move otherwise)
    rect.x = Math.max(screen.x, rect.x);
    rect.y = Math.max(screen.y, rect.y);
    // Ensure rect corner is inside the screen (move otherwise)
    rect.x = Math.min(rect.x, screen.x + screen.width - rect.width);
    rect.y = Math.min(rect.y, screen.y + screen.height - rect.height);
  }

  @NotNull
  public static Dimension getProgressBarPreferredSize(JProgressBar progressBar) {
    if (progressBar == null)
      throw new NullPointerException();
    try {
      Dimension r = progressBar.getPreferredSize();
      if (r == null) {
        Log.warn("progress bar preferred size is null: " + progressBar);
        return new Dimension(100, 24);
      }
      return r;
    } catch (NullPointerException e) {
      // #1243
      // this is a Sun's bug!
      LookAndFeel laf = UIManager.getLookAndFeel();
      String lafName = laf == null ? "null laf" : laf.getClass().getName();
      Log.warn("progress bar preferred size NPE: " + progressBar + " (" + lafName + ")");
      return new Dimension(100, 24);
    }
  }


  public static void invokeLater(int timeout, ActionListener runnable) {
    javax.swing.Timer timer = new Timer(timeout, runnable);
    timer.setRepeats(false);
    timer.start();
  }

  public static void copyFocusTraversalKeys(Component from, Component to) {
    to.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
      from.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
    to.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
      from.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
//    to.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, from.getFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS));
//    to.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, from.getFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS));
  }

  public static JScrollPane encloseWithScrollPaneForPopup(JComponent popup, JComponent owner) {
    final JScrollPane scrollPane = new JScrollPane(popup);
    final Dimension d1 = owner.getSize();
    final Dimension d2 = UIUtil.getRelativeDimension(owner, 10, 10);
    scrollPane.setPreferredSize(new Dimension(d1.width, d2.height));
    scrollPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true), DropDownListener.HIDE_DROP_DOWN);
    Aqua.cleanScrollPaneBorder(scrollPane);
    return scrollPane;
  }

  public static void selectAll(JTextComponent c) {
    c.setSelectionStart(0);
    int len = getDocumentText(c).length();
    c.setSelectionEnd(len);
  }

  public static int getFormLayoutRowOfDescendant(JComponent panel, String descendantName) {
    FormLayout layout = getFormLayout(panel);
    Component c = findComponentByName(panel, descendantName);
    if (layout == null || descendantName == null || descendantName.length() == 0 || c == null) {
      assert false;
      return -1;
    }
    CellConstraints cc = layout.getConstraints(c);
    if (cc == null) {
      assert false;
      return -1;
    }
    return cc.gridY;
  }

  public static FormLayout getFormLayout(JComponent panel) {
    LayoutManager lm = panel.getLayout();
    if (lm instanceof FormLayout)
      return (FormLayout) lm;
    assert false : lm;
    return null;
  }

  /**
   * Extracts actual text from document.<br>
   * Use this method when calculating text offsets for painting.<br>
   * {@link javax.swing.text.JTextComponent#getText()} may return new lines different from document content
   */
  @NotNull
  public static String getDocumentText(JTextComponent component) {
    Document document = component.getDocument();
    try {
      return document.getText(0, document.getLength());
    } catch (BadLocationException e) {
      LogHelper.error(e);
      return "";
    }
  }

  private static abstract class ButtonPresser extends KeyAdapter {
    private final AbstractButton myButton;

    public ButtonPresser(AbstractButton button) {
      myButton = button;
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if(shouldHandle(e)) {
        myButton.getModel().setArmed(true);
        myButton.getModel().setPressed(true);
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      if(shouldHandle(e)) {
        myButton.getModel().setPressed(false);
        myButton.getModel().setArmed(false);
      }
    }

    protected abstract boolean shouldHandle(KeyEvent e);
  }

  public static KeyListener pressButtonWithKey(AbstractButton button, Integer... keyCode) {
    final Set<Integer> keyCodes = Collections15.hashSet(keyCode);
    return new ButtonPresser(button) {
      @Override
      protected boolean shouldHandle(KeyEvent e) {
        return keyCodes.contains(e.getKeyCode());
      }
    };
  }

  public static KeyListener pressButtonWithKeyStroke(AbstractButton button, KeyStroke... strokes) {
    final Set<KeyStroke> strokeSet = Collections15.hashSet(strokes);
    return new ButtonPresser(button) {
      @Override
      protected boolean shouldHandle(KeyEvent e) {
        return strokeSet.contains(getKeyStroke(e));
      }

      private KeyStroke getKeyStroke(KeyEvent e) {
        return KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
      }
    };
  }

  public static Detach addSelectedListener(final JToggleButton button, final ChangeListener listener) {
    final javax.swing.event.ChangeListener l = new javax.swing.event.ChangeListener() {
      private boolean mySelected = button.isSelected();

      public void stateChanged(ChangeEvent e) {
        if (mySelected == button.isSelected())
          return;
        mySelected = button.isSelected();
        listener.onChange();
      }
    };
    button.addChangeListener(l);
    return new Detach() {
      protected void doDetach() {
        button.removeChangeListener(l);
      }
    };
  }

  public static int getTextComponentPreferredHeight(JTextComponent component, int width) {
    Dimension backup = component.getSize();
    Insets insets = component.getInsets();
    try {
      component.setSize(width, Short.MAX_VALUE);
      return component.getPreferredSize().height;
    } finally {
      component.setSize(backup);
    }
  }

  public static JRadioButton createRadioButton(String text) {
    JRadioButton button = new JRadioButton();
    NameMnemonic.parseString(text).setToButton(button);
    return button;
  }

  @NotNull
  public static Border getCompoundBorder(@Nullable Border outer, @Nullable Border inner) {
    if (outer == null) {
      if (inner == null) {
        return AwtUtil.EMPTY_BORDER;
      } else {
        return inner;
      }
    } else {
      if (inner == null) {
        return outer;
      } else {
        return new CompoundBorder(outer, inner);
      }
    }
  }

  public static void addOuterBorder(@NotNull JComponent component, @Nullable Border border) {
    component.setBorder(getCompoundBorder(border, component.getBorder()));
  }

  public static ButtonGroup createButtonGroup(AbstractButton... buttons) {
    ButtonGroup group = new ButtonGroup();
    for (AbstractButton button : buttons) {
      group.add(button);
    }
    return group;
  }

  public static void addChangeListeners(Lifespan lifespan, javax.swing.event.ChangeListener listener,
    AbstractButton... buttons)
  {
    if (lifespan.isEnded())
      return;
    for (AbstractButton button : buttons) {
      lifespan.add(addChangeListener(button.getModel(), listener));
    }
  }

  public static void setEnabledDeep(Component component, final boolean enabled) {
    visitComponents(component, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent component) {
        if (component instanceof AbstractButton || component instanceof JTextComponent || component instanceof JList ||
          component instanceof JComboBox)
        {
          component.setEnabled(enabled);
        }
        return true;
      }
    });
  }

  public static void addMouseMotionListener(Lifespan lifespan, final JComponent component,
    final MouseMotionListener listener)
  {
    if (lifespan.isEnded())
      return;
    component.addMouseMotionListener(listener);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        component.removeMouseMotionListener(listener);
      }
    });
  }

  public static void addTreeModelListener(Lifespan lifespan, final TreeModel model, final TreeModelListener listener) {
    if (!lifespan.isEnded()) {
      model.addTreeModelListener(listener);
      lifespan.add(new Detach() {
        protected void doDetach() throws Exception {
          model.removeTreeModelListener(listener);
        }
      });
    }
  }

  public static void addTableModelListener(Lifespan lifespan, final TableModel model,
    final TableModelListener listener)
  {
    if (!lifespan.isEnded()) {
      model.addTableModelListener(listener);
      lifespan.add(new Detach() {
        protected void doDetach() throws Exception {
          model.removeTableModelListener(listener);
        }
      });
    }
  }

  public static void addOpenLinkInBrowserListener(JEditorPane editorPane) {
    editorPane.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        HyperlinkEvent.EventType type = e.getEventType();
        if (type == HyperlinkEvent.EventType.ACTIVATED) {
          URL url = findUrl(e);
          if (url != null) ExternalBrowser.openURL(url.toExternalForm(), true);
        }
      }

      private URL findUrl(HyperlinkEvent e) {
        URL url = e.getURL();
        if (url != null) return url;
        Object a = e.getSourceElement().getAttributes().getAttribute(HTML.Tag.A);
        if (a instanceof SimpleAttributeSet) {
          String href = Util.castNullable(String.class, ((SimpleAttributeSet) a).getAttribute(HTML.Attribute.HREF));
          if (href != null) {
            try {
              return new URL(href);
            } catch (MalformedURLException e1) {
              // ignore
            }
          }
        }
        return null;
      }
    });
  }

  public static boolean isEmptyInsets(Insets insets) {
    return insets.left == 0 && insets.right == 0 && insets.top == 0 && insets.bottom == 0;
  }

  public static int getClientAreaWidth(JComponent component) {
    return component.getWidth() - AwtUtil.getInsetWidth(component);
  }

  public static int getClientAreaHeight(JComponent component) {
    return component.getHeight() - AwtUtil.getInsetHeight(component);
  }

  public static void requestFocusInWindowLater(final Component c) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        c.requestFocusInWindow();
      }
    });
  }

  public static void requestFocusLater(final Component c) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        c.requestFocus();
      }
    });
  }

  private static class Notice extends AbstractAction implements FocusListener, KeyListener {
    private JPanel myPanel;
    private JEditorPane myEditor;
    private JButton myButton;
    private Popup myPopup;
    private Detach myCloser;
    private Dimension myDimension;

    public Notice(String htmlText, int maxPopupWidth) {
      putValue(Action.NAME, "Close");
      putValue(Action.ACCELERATOR_KEY, Shortcuts.ESCAPE);

      myPanel = new JPanel(new BorderLayout(0, 9));
      final Color bg = getNoticeBackground();
      myPanel.setBackground(bg);
      myPanel.setOpaque(true);
      myPanel.setBorder(
        new CompoundBorder(new LineBorder(bg.darker().darker().darker(), 1), BORDER_9));

      if(htmlText.indexOf("<html>") < 0) {
        htmlText = "<html><body>" + htmlText;
      }

      myEditor = new JEditorPane();
      myEditor.setEditorKit(new HTMLEditorKit());
      UIUtil.addOpenLinkInBrowserListener(myEditor);
      ReadOnlyTextFields.basicReadonlySetup(myEditor);
      myEditor.setBorder(AwtUtil.EMPTY_BORDER);
      ErrorHunt.setEditorPaneText(myEditor, TextUtil.preprocessHtml(htmlText));
      myEditor.setCaretPosition(0);
      myPanel.add(myEditor, BorderLayout.CENTER);

      myDimension = myEditor.getPreferredSize();
      if(myDimension.width > maxPopupWidth) {
        final int height = Math.max(SwingHTMLHack.getPreferredHeight(myEditor, htmlText, maxPopupWidth), 30);
        myDimension = new Dimension(maxPopupWidth, height);
        myEditor.setPreferredSize(myDimension);
      }

      myButton = new JButton("Close");
      if(Aqua.isAqua()) {
        myButton.setOpaque(false);
      }
      myPanel.add(SingleChildLayout.envelop(myButton), BorderLayout.SOUTH);

      myDimension = new Dimension(myPanel.getPreferredSize());
    }

    public Detach showPopup(Component owner, Point location, final Detach onClose) {
      myPopup = getPopup(owner, myPanel, true, location);

      myCloser = new Detach() {
        protected void doDetach() throws Exception {
          myPopup.hide();
          onClose.detach();
        }
      };


      myButton.setAction(this);
      myButton.addFocusListener(this);
      myEditor.addFocusListener(this);
      myButton.addKeyListener(this);
      myEditor.addKeyListener(this);

      myPopup.show();
      myButton.requestFocus();

      return myCloser;
    }

    public Dimension getDimension() {
      return myDimension;
    }

    public void actionPerformed(ActionEvent e) {
      myCloser.detach();
    }

    public void focusLost(FocusEvent e) {
      Component c = e.getOppositeComponent();
      if(c == null || SwingTreeUtil.findAncestorOfType(c, JPanel.class) != myPanel) {
        myCloser.detach();
      }
    }

    public void keyReleased(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
        myCloser.detach();
      }
    }

    public void focusGained(FocusEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {}
  }

  /**
   * A {@code Positioner} is used to calculate the position of a notice
   * pop-up relative to its owner component within screen bounds.
   */
  public static interface Positioner {
    /**
     * Calculate the position of the pop-up.
     * @param screen Screen bounds.
     * @param owner Owner bounds (in screen coordinate system).
     * @param child Pop-up size.
     * @return Pop-up position.
     */
    Point getPosition(Rectangle screen, Rectangle owner, Dimension child);
  }

  /**
   * One-dimensional variant of {@link Positioner}.
   * Used to independently calculate x and y coordiantes.
   */
  public static interface Positioner1D {
    /**
     * Calculate the pop-up position.
     * @param ss Start of the screen.
     * @param sl Length of the screen.
     * @param os Start of the owner.
     * @param ol Length of the owner.
     * @param cl Length of the pop-up.
     * @return Start of the pop-up.
     */
    int getPosition(int ss, int sl, int os, int ol, int cl);
  }

  /**
   * Aligns the beginning of the pop-up with the beginning of the owner.
   */
  public static final Positioner1D ALIGN_START = new Positioner1D() {
    public int getPosition(int ss, int sl, int os, int ol, int cl) {
      return os;
    }
  };

  /**
   * Aligns the end of the pop-up with the end of the owner.
   */
  public static final Positioner1D ALIGN_END = new Positioner1D() {
    public int getPosition(int ss, int sl, int os, int ol, int cl) {
      return os + ol - cl;
    }
  };

  /**
   * Aligns the end of the pop-up with the start of the owner.
   */
  public static final Positioner1D BEFORE = new Positioner1D() {
    public int getPosition(int ss, int sl, int os, int ol, int cl) {
      return os - cl - 1;
    }
  };

  /**
   * Try it after the owner, if not enough space, then before the owner.
   */
  public static class AfterOrBefore implements Positioner1D {
    private final int myGap;

    public AfterOrBefore(int gap) {
      myGap = gap;
    }

    public int getPosition(int ss, int sl, int os, int ol, int cl) {
      if(os + ol + cl + myGap <= ss + sl) {
        return os + ol + myGap;
      }
      return os - cl - myGap;
    }
  }

  /**
   * Default Y-axis positioner for notice pop-ups.
   */
  public static final Positioner1D NOTICE_POPUP_Y = new AfterOrBefore(1);

  /**
   * A {@code Positioner} implementation that makes life easier in two ways:
   * - allows calculating x and y coordinates independenly in two
   *   overridden methods or {@link Positioner1D} instances;
   * - auto-adjusts calculated coordinates so that the pop-up tries not to go
   * off-screen (no redundant checks for implementers).
   */
  public static class IndependentPositioner implements Positioner {
    private final Positioner1D myXPositioner;
    private final Positioner1D myYPositioner;

    public IndependentPositioner() {
      this(null, null);
    }

    public IndependentPositioner(Positioner1D xPositioner, Positioner1D yPositioner) {
      myXPositioner = xPositioner;
      myYPositioner = yPositioner;
    }

    public Point getPosition(Rectangle screen, Rectangle owner, Dimension child) {
      int x = getX(screen.x, screen.width, owner.x, owner.width, child.width);
      if(x + child.width > screen.x + screen.width) {
        x = screen.x + screen.width - child.width;
      }
      if(x < screen.x) {
        x = screen.x;
      }

      int y = getY(screen.y, screen.height, owner.y, owner.height, child.height);
      if(y + child.height > screen.y + screen.height) {
        y = screen.y + screen.height - child.height;
      }
      if(y < screen.y) {
        y = screen.y;
      }

      return new Point(x, y);
    }

    public int getX(int screenX, int screenW, int ownerX, int ownerW, int childW) {
      return myXPositioner.getPosition(screenX, screenW, ownerX, ownerW, childW);
    }

    public int getY(int screenY, int screenH, int ownerY, int ownerH, int childH) {
      return myYPositioner.getPosition(screenY, screenH, ownerY, ownerH, childH);
    }
  }

  /**
   * Default positioner for component-owned notice popups. Tries to show the pop-up
   * below the owner, if there's enough screen space; otherwise, above the owner.
   * The left edge of the pop-up is aligned with the left edge of the owner. 
   */
  public static final Positioner NOTICE_POPUP = new IndependentPositioner(ALIGN_START, NOTICE_POPUP_Y);

  public static Detach showNoticePopup(String htmlText, Component owner) {
    return showNoticePopup(htmlText, owner, Detach.NOTHING, NOTICE_POPUP);
  }

  public static Detach showNoticePopup(String htmlText, Component owner, @Nullable Detach onClose, Positioner positioner) {
    if(owner == null) {
      owner = getDefaultDialogOwner();
    }

    Point p;
    Notice notice = new Notice(htmlText, MAX_NOTICE_POPUP_WIDTH);
    if(owner != null) {
      p = calcPopupPosition(owner, positioner, notice.getDimension());
    } else {
      final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
      p = new Point(size.width / 2 - 50, size.height / 2 - 50);
    }
    return notice.showPopup(owner, p, Util.NN(onClose, Detach.NOTHING));
  }

  public static Color getNoticeBackground() {
    return ColorUtil.between(getEditorBackground(), Color.YELLOW, 0.15F);
  }

  public static Point calcPopupPosition(Component owner, Positioner positioner, Dimension popupSize) {
    Rectangle ob = SwingTreeUtil.getVisibleScreenBounds(owner);
    return positioner.getPosition(owner.getGraphicsConfiguration().getBounds(), ob, popupSize);
  }

  public static void showNoticePopup(String htmlText, Window window) {
    final Notice notice = new Notice(htmlText, window.getWidth());

    final Component owner;
    if(window instanceof RootPaneContainer) {
      owner = ((RootPaneContainer)window).getContentPane();
    } else {
      owner = getDefaultDialogOwner();
    }

    final Dimension sz = notice.getDimension();
    final Rectangle db = window.getGraphicsConfiguration().getBounds();
    final Rectangle pb = window.getBounds();

    final Point location = new Point();

    if(pb.x - sz.width >= db.x) {
      location.x = pb.x - sz.width;
    } else {
      location.x = pb.x + pb.width;
    }

    if(pb.y + sz.height <= db.y + db.height) {
      location.y = pb.y;
    } else {
      location.y = db.y + db.height - sz.height;
    }

    notice.showPopup(owner, location, Detach.NOTHING);
  }

  public static void addGlobalFocusOwnerListener(Lifespan lifespan, final PropertyChangeListener listener) {
    addKeyboardFocusManagerListener(lifespan, "focusOwner", listener);
  }

  public static void addKeyboardFocusManagerListener(Lifespan lifespan, final String propertyName,
    final PropertyChangeListener listener)
  {
    if (lifespan.isEnded())
      return;
    final KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    mgr.addPropertyChangeListener(propertyName, listener);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        mgr.removePropertyChangeListener(propertyName, listener);
      }
    });
  }

  public static JComponent createMessagePanel(String message, boolean scrollpaned, boolean white, Border border) {
    JPanel panel = SingleChildLayout.envelopCenter(new JLabel(message));
    if (white) {
      panel.setOpaque(true);
      panel.setBackground(UIUtil.getEditorBackground());
    } else {
      panel.setOpaque(false);
    }
    if (border != null) {
      panel.setBorder(border);
    }
    if (scrollpaned) {
      return getScrollPaned(panel);
    } else {
      return panel;
    }
  }

  @Nullable
  public static JDialog createDialog(Component owner) {
    if (owner == null)
      return null;
    Window window = SwingUtilities.getWindowAncestor(owner);
    if (window == null)
      return null;
    if (window instanceof JFrame)
      return new JDialog((JFrame) window);
    else if (window instanceof JDialog)
      return new JDialog((JDialog) window);
    else
      return null;
  }

  public static void safeFillRect(Graphics g, int x, int y, int width, int height) {
    Graphics gg = Env.isMac() ? g.create() : g;
    try {
      gg.fillRect(x, y, width, height);
    } finally {
      if (gg != g)
        gg.dispose();
    }
  }

  public static void addToolkitListener(Lifespan lifespan, long eventMask, final AWTEventListener listener) {
    if (lifespan.isEnded())
      return;
    Toolkit.getDefaultToolkit().addAWTEventListener(listener, eventMask);
    lifespan.add(new Detach() {
      protected void doDetach() throws Exception {
        Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
      }
    });
  }


  public static synchronized AListModel<TimeZone> createAvailableTimezonesModel() {
    if (ourTimezonesModel == null) {
      String[] ids = TimeZone.getAvailableIDs();
      Set<TimeZone> zones = Collections15.linkedHashSet();
      TimeZone gmt = TimeZone.getTimeZone("GMT");
      if (gmt == null) {
        assert false;
        Log.warn("no gmt");
        return AListModel.EMPTY;
      }
      zones.add(gmt);
      for (String id : ids) {
        TimeZone zone = TimeZone.getTimeZone(id);
        if (zone != gmt && zone != null) {
          zones.add(zone);
        }
      }
      TimeZone[] zoneArray = zones.toArray(new TimeZone[zones.size()]);
      Arrays.sort(zoneArray, new Comparator<TimeZone>() {
        public int compare(TimeZone o1, TimeZone o2) {
          return Containers.compareInts(o1.getRawOffset(), o2.getRawOffset());
        }
      });
      ourTimezonesModel = FixedListModel.create(zoneArray);
    }
    return ourTimezonesModel;
  }

  public static void addChangeListeners(Lifespan life, final javax.swing.event.ChangeListener listener,
    final JViewport viewport)
  {
    if (life.isEnded())
      return;
    viewport.addChangeListener(listener);
    if (life != Lifespan.FOREVER)
      life.add(new Detach() {
        protected void doDetach() throws Exception {
          viewport.removeChangeListener(listener);
        }
      });
  }

  public static void configureScrollpaneVerticalOnly(JScrollPane scrollPane) {
    ScrollBarPolicy.setDefaultWithHorizontal(scrollPane, ScrollBarPolicy.NEVER);
  }

  public static void configureBasicScrollPane(JScrollPane pane, int colWidth, int rowHeight) {
    pane.setPreferredSize(UIUtil.getRelativeDimension(pane, colWidth, rowHeight));
    configureScrollpaneVerticalOnly(pane);
  }

  public static void setUneditableField(JTextField field, boolean useOpacity) {
    field.setEditable(false);
    if (Env.isWindows()) {
      if (useOpacity)
        field.setOpaque(false);
      else
        field.setBackground(getDefaultBackgroundColor());
    }
  }

  public static void fillPaint(Graphics g, JComponent c, Paint paint, boolean useInsets) {
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      Dimension size = c.getSize();
      Insets insets = useInsets ? c.getInsets() : AwtUtil.EMPTY_INSETS;
      g2.setPaint(paint);
      g2.fillRect(insets.left, insets.top, size.width - insets.left - insets.right,
        size.height - insets.top - insets.bottom);
    } finally {
      g2.dispose();
    }
  }

  public static void setCaretAlwaysVisible(Lifespan life, final JTextComponent c) {
    if (life.isEnded())
      return;
    life.add(addFocusListener(c, new FocusListener() {
      public void focusGained(FocusEvent e) {
        if (!c.isEditable()) {
          Caret caret = c.getCaret();
          if (caret != null) {
            caret.setVisible(true);
          }
        }
      }

      public void focusLost(FocusEvent e) {
        if (!c.isEditable()) {
          Caret caret = c.getCaret();
          if (caret != null) {
            caret.setVisible(false);
          }
        }
      }
    }));
  }

  public static void setInitialFocus(final Component c) {
    if (c.isShowing()) {
      c.requestFocusInWindow();
    }
    final HierarchyListener listener = new HierarchyListener() {
      public void hierarchyChanged(HierarchyEvent e) {
        if (c.isShowing()) {
          c.removeHierarchyListener(this);
          requestFocusInWindowLater(c);
        }
      }
    };
    c.addHierarchyListener(listener);
  }

  public static JTabbedPane newJTabbedPane() {
    if (!Env.isMac()) {
      return new JTabbedPane();
    } else {
      // see #934
      return new JTabbedPane() {
        protected void fireStateChanged() {
          try {
            super.fireStateChanged();
          } catch (IndexOutOfBoundsException e) {
            Log.warn("http://bugzilla.almworks.com/show_bug.cgi?id=934", e);
          }
        }
      };
    }
  }

  public static void forEachComponentAndTheirLabels(Collection<? extends Component> components,
    Procedure<Component> procedure)
  {
    if (components == null || components.isEmpty())
      return;
    Set<Container> parents = Collections15.hashSet();
    for (Component c : components) {
      if (c == null)
        continue;
      procedure.invoke(c);
      Container p = c.getParent();
      if (p != null && parents.add(p)) {
        for (int i = 0; i < p.getComponentCount(); i++) {
          Component peer = p.getComponent(i);
          if (peer instanceof JLabel) {
            Component labelFor = ((JLabel) peer).getLabelFor();
            if (labelFor != null && components.contains(labelFor)) {
              procedure.invoke(peer);
            }
          }
        }
      }
    }
  }

  /**
   * Creates a <code>Popup</code> for the Component <code>owner</code>
   * containing the Component <code>contents</code>.
   * On Mac OS a <code>JUMP</code> to <code>parent</code> is set on
   * <code>contents</code> to work around the parent-child inconsistency.
   * @param parent Component mouse coordinates are relative to, may be null
   * @param contents Contents of the Popup
   * @param x Initial x screen coordinate
   * @param y Initial y screen coordinate
   * @return Popup containing Contents
   * @throws IllegalArgumentException if contents is null
   * @see javax.swing.PopupFactory#getPopup(java.awt.Component, java.awt.Component, int, int) 
   */
  public static Popup getPopup(Component parent, Component contents, int x, int y) {
    return getPopup(parent, contents, false, new Point(x, y));
  }

  public static Popup getPopup(Component parent, Component content, boolean focusable, Point location) {
    final Popup popup = PopupFactory.getSharedInstance().getPopup(parent, content, location.x, location.y);

    if(Env.isMac()) {
      if(parent instanceof JComponent && content instanceof JComponent) {
        ComponentProperty.JUMP.putClientValue(
            (JComponent) content, (JComponent)parent);
      } else {
        Log.warn("UIUtil.getPopup(): parent=[" + parent + "]; contents = [" + content + "]");
      }
    }
    if (focusable) {
      Window containerWindow = SwingTreeUtil.findAncestorOfType(content, Window.class);
      if (containerWindow != null) containerWindow.setFocusableWindowState(true);
    }
    return popup;
  }

  /**
   * Creates and returns a new {@link JPopupMenu} instance.
   * For Mac OS X instantiates an {@link AquaPopupMenuAdapter}.
   * @return A new {@code JPopupMenu} instance.
   */
  public static JPopupMenu createJPopupMenu() {
    return Aqua.isAqua() ? new AquaPopupMenuAdapter() : new JPopupMenu();
  }

  /**
   * Adjusts the HorizontalAlignment property of every {@code JLabel} and
   * {@link Link} contained by the given component to the preferred
   * alignment: trailing on the Mac, leading otherwise.
   * To prevent this asjustment for a particular label, put a client
   * property with a key of UIUtil.SET_DEFAULT_LABEL_ALIGNMENT and a value
   * of Boolean.FALSE.
   * @param parent The parent component.
   */
  public static void setDefaultLabelAlignment(JComponent parent) {
    final int horizontalAlignment = Env.isMac() ? SwingConstants.TRAILING : SwingConstants.LEADING;

    visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent element) {
        if(element instanceof JLabel || element instanceof Link) {
          if(!Boolean.FALSE.equals(element.getClientProperty(SET_DEFAULT_LABEL_ALIGNMENT))) {
            if(element instanceof JLabel) {
              ((JLabel)element).setHorizontalAlignment(horizontalAlignment);
            } else if(element instanceof Link) {
              ((Link)element).setHorizontalAlignment(horizontalAlignment);
            }
          }
        }
        return true;
      }
    });
  }

  /**
   * Determines whether {@code parent} or any of its child
   * components owns the focus.
   * @param parent The parent component.
   * @return {@code true} if the focus owner is within {@code parent}'s subtree.
   */
  public static boolean containsFocusOwner(JComponent parent) {
    final boolean[] result = { false };

    visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent element) {
        if(element.isFocusOwner()) {
          result[0] = true;
          return false;
        }
        return true;
      }
    });

    return result[0];
  }

  /**
   * Retrieve the focus owner from {@code parent}'s subtree, if there is one.
   * @param parent The parent component.
   * @return Focus owner or {@code null}.
   */
  public static JComponent getFocusOwnerFrom(JComponent parent) {
    final JComponent[] result = { null };

    visitComponents(parent, JComponent.class, new ElementVisitor<JComponent>() {
      public boolean visit(JComponent element) {
        if(element.isFocusOwner()) {
          result[0] = element;
          return false;
        }
        return true;
      }
    });

    return result[0];
  }

  /**
   * Removes all rows from layout, updating groups as well
   *
   * @param layout the layout
   * @param lastRemainingRow all rows that are *after* this one are removed
   */
  public static void removeExcessRowsFromFormLayout(FormLayout layout, int lastRemainingRow) {
    int[][] rowGroups = layout.getRowGroups();
    if (rowGroups.length > 0) {
      List<int[]> cc = Collections15.arrayList();
      for (int[] rowGroup : rowGroups) {
        List<Integer> g = Collections15.arrayList();
        for (int row : rowGroup) {
          if (row <= lastRemainingRow) {
            g.add(row);
          }
        }
        if(!g.isEmpty()) {
          int[] newg = new int[g.size()];
          for (int i = 0; i < g.size(); i++) {
            newg[i] = g.get(i);
          }
          cc.add(newg);
        }
      }
      int[][] updatedGroups = cc.toArray(new int[cc.size()][]);
      layout.setRowGroups(updatedGroups);
    }

    while (layout.getRowCount() > lastRemainingRow) {
      layout.removeRow(layout.getRowCount());
    }
  }

  public static void minimizeFrame(final Frame frame) {
    final int state = frame.getState();
    if(state == Frame.NORMAL) {
      frame.setState(Frame.ICONIFIED);
    }
  }

  public static void restoreFrame(final Frame frame) {
    if(!frame.isVisible()) {
      frame.setVisible(true);
    }
    final int state = frame.getState();
    if(state == Frame.ICONIFIED) {
      frame.setState(Frame.NORMAL);
    }
  }

  public static boolean isMinimized(Window window) {
    if(window instanceof Frame) {
      return ((Frame)window).getState() == Frame.ICONIFIED;
    }
    return false;
  }

  public static Dimension getIconButtonPrefSize() {
    if(ICON_BUTTON_PREF_SIZE == null) {
      final AToolbarButton dummy = new AToolbarButton();
      dummy.setText("Dummy");
      dummy.setIcon(Icons.ACTION_GENERIC_CANCEL_OR_REMOVE);
      ICON_BUTTON_PREF_SIZE = dummy.getPreferredSize();
      ICON_BUTTON_PREF_SIZE.width = ICON_BUTTON_PREF_SIZE.height;
    }
    return new Dimension(ICON_BUTTON_PREF_SIZE);
  }

  public static JPanel createInlinePanel(
    InlineLayout.Orientation orientation, int gap, boolean stretch, JComponent... content) 
  {
    final JPanel panel = new JPanel(new InlineLayout(orientation, gap, stretch));
    for(final JComponent c : content) {
      panel.add(c);
    }
    return panel;
  }

  private static class MousePopupTrigger extends MouseAdapter {
    private final FlatCollectionComponent<?> myComponent;

    public MousePopupTrigger(FlatCollectionComponent<?> component) {
      myComponent = component;
    }

    public void mouseClicked(MouseEvent e) {
      processMouseEvent(e);
    }

    public void mousePressed(MouseEvent e) {
      processMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
      processMouseEvent(e);
    }

    private void processMouseEvent(MouseEvent e) {
      if (!e.isPopupTrigger())
        return;
      Point point = e.getPoint();
      adjustSelection(myComponent.getElementIndexAt(point.x, point.y), myComponent.getSelectionAccessor());
    }
  }

  public static boolean isPrimaryDoubleClick(MouseEvent e) {
    return e.getID() == MouseEvent.MOUSE_CLICKED
      && e.getButton() == MouseEvent.BUTTON1
      && e.getClickCount() == 2;
  }

  public static void trackVisibility(Lifespan life, JComponent master, final JComponent ... slaves) {
    if (slaves.length == 0 || life.isEnded())
      return;
    Procedure<Boolean> proc = new Procedure<Boolean>() {
      @Override
      public void invoke(Boolean visible) {
        for (JComponent slave : slaves)
          slave.setVisible(visible);
      }
    };
    trackVisibility(life, master, proc);
  }

  private static final String CHARSET_SETTING = "charset";
  private static final CanvasRenderer<Charset> ENCODING_COMBO_RENDERER = new CanvasRenderer<Charset>() {
    @Override
    public void renderStateOn(CellState state, com.almworks.util.components.Canvas canvas, Charset item) {
      if (item == null) return;
      canvas.appendText(item.displayName(Locale.getDefault()));
    }
  };
  public static void configureEncodingCombo(Lifespan life, AComboBox<Charset> combo, final Configuration initialConfig, @Nullable final Procedure<Charset> proc, Configuration recentsConfig, Charset defaultCharset) {
    Map<String, Charset> charsets = Charset.availableCharsets();
    SelectionInListModel<Charset> model = SelectionInListModel.create(charsets.values(), null);
    boolean preFill = recentsConfig.isEmpty();
    RecentController<Charset> recents = new RecentController<Charset>();
    recents.setup(model, recentsConfig);
    recents.setWrapRecents(true);
    recents.setRenderer(ENCODING_COMBO_RENDERER);
    recents.setIdentityConvertor(new Convertor<Charset, String>() {
      @Override
      public String convert(Charset value) {
        return value != null ? value.name() : "";
      }
    });
    if (preFill) {
      List<Charset> initials = new ArrayList<Charset>();
      for (String cs : IOUtils.COMMON_CHARSET_NAMES)
        if (charsets.containsKey(cs))
          initials.add(charsets.get(cs));
      recents.addToRecent(initials);
    }

    recents.setupAComboBox(combo, life);
    AddRecentFromComboBox.install(life, recents, combo);

    final AComboboxModel<Charset> decoratedModel = combo.getModel();
    decoratedModel.addSelectionChangeListener(life, new ChangeListener() {
      @Override
      public void onChange() {
        Charset charset = UnwrapCombo.getUnwrapSelected(decoratedModel);
        initialConfig.setSetting(CHARSET_SETTING, charset != null ? charset.name() : "");
        if (proc != null)
          proc.invoke(charset);
      }
    });

    Charset initial = charsets.get(initialConfig.getSetting(CHARSET_SETTING, ""));
    if (initial == null)
      initial = defaultCharset;
    recents.addToRecent(initial);
    UnwrapCombo.selectRecent(decoratedModel, initial);
  }

  public static void trackVisibility(Lifespan life, final JComponent master, final Procedure<Boolean> proc) {
    class MyListener implements HierarchyListener {
      boolean wasVisible = master.isVisible();

      @Override
      public void hierarchyChanged(HierarchyEvent e) {
        boolean visible = master.isVisible();
        if (wasVisible == visible)
          return;
        wasVisible = visible;
        proc.invoke(visible);
      }
    }
    final MyListener listener = new MyListener();
    master.addHierarchyListener(listener);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        master.removeHierarchyListener(listener);
      }
    });
    proc.invoke(master.isVisible());
  }
  
  public static JTextField createCopyableLabel() {
    JTextField field = new JTextField() {
      @Override
      public void setBorder(Border border) {
        // No border
      }
    };
    field.setOpaque(true);
    field.setEditable(false);
    return field;
  }

  public static class FormLayoutRowHider implements Procedure<Boolean> {
    private static final RowSpec ZERO_ROW_SPEC = new RowSpec("0dlu");

    private final JPanel myPanel;
    private final FormLayout myLayout;
    private final String myCmpName;

    private RowSpec myUpperSpec, mySpec;
    private boolean myWasVisible;


    public FormLayoutRowHider(JPanel panel, String cmpName) {
      myPanel = panel;
      myLayout = getFormLayout(myPanel);
      myCmpName = cmpName;
    }

    @Override
    public void invoke(Boolean visible) {
      if (myWasVisible == visible)
        return;
      myWasVisible = visible;
      int index = getFormLayoutRowOfDescendant(myPanel, myCmpName);
      if (index <= 0)
        return;
      if (!visible) {
        myUpperSpec = myLayout.getRowSpec(index - 1);
        mySpec = myLayout.getRowSpec(index);
        assert (myUpperSpec != null && mySpec != null);
      }
      if (mySpec == null)
        return;
      myLayout.setRowSpec(index-1, visible ? myUpperSpec : ZERO_ROW_SPEC);
      myLayout.setRowSpec(index, visible ? mySpec : ZERO_ROW_SPEC);
    }
  }
}