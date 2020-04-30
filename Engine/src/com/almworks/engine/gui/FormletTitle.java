package com.almworks.engine.gui;

import com.almworks.util.Env;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.ElementVisitor;
import com.almworks.util.components.*;
import com.almworks.util.components.layout.WidthDrivenColumn;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public class FormletTitle extends JPanel {
  public static final TypedKey<String> COLLAPSED_TOOLTIP = TypedKey.create("collapsed");
  public static final TypedKey<String> EXPANDED_TOOLTIP = TypedKey.create("expanded");
  public static final TypedKey<String> FOCUSED_TOOLTIP = TypedKey.create("focused");
  private static final Object EXPAND_OR_FOCUS = "expandOrFocus";
  private static final EmptyBorder OFFSET_BORDER = new EmptyBorder(0, 5, 0, 0);
  private static final AnAction EXPAND_SECTION_ACTION = new ExpandSectionAction();
  private static final DataRole<FormletTitle> FORMLET_HEADER = DataRole.createRole(FormletTitle.class);
  private static final ComponentProperty<java.util.List<FormletTitle>> LINEUP =
    ComponentProperty.createProperty("LINEUP");

  /**
   * @param index The formlet index.
   * @return The string representation of the quick navigation
   * shortcut for this formlet (OS-dependent).
   */
  private static String getShortcutString(int index) {
    if(index < 0 || index > 9) {
      return "";
    }

    return Shortcuts.getKeyStrokeText(getShortcutKeystrokes(index)[0]);
  }

  /**
   * Key codes for formlet quick navigation shortcuts.
   */
  private static final int[][] SHORTCUT_KEYCODES = {
    {KeyEvent.VK_0, KeyEvent.VK_NUMPAD0 },
    {KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 },
    {KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 },
    {KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 },
    {KeyEvent.VK_4, KeyEvent.VK_NUMPAD4 },
    {KeyEvent.VK_5, KeyEvent.VK_NUMPAD5 },
    {KeyEvent.VK_6, KeyEvent.VK_NUMPAD6 },
    {KeyEvent.VK_7, KeyEvent.VK_NUMPAD7 },
    {KeyEvent.VK_8, KeyEvent.VK_NUMPAD8 },
    {KeyEvent.VK_9, KeyEvent.VK_NUMPAD9 },
  };

  /**
   * OS-dependent KeyStrokes for formlet quick
   * navigation shortcuts.
   */
  private static final KeyStroke[][] SHORTCUT_KEYSTROKES;
  static {
    final int modifiers = Env.isMac()
        ? (KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)
        : KeyEvent.ALT_DOWN_MASK;

    SHORTCUT_KEYSTROKES = new KeyStroke[SHORTCUT_KEYCODES.length][];
    for(int i = 0; i < SHORTCUT_KEYCODES.length; i++) {
      SHORTCUT_KEYSTROKES[i] = new KeyStroke[SHORTCUT_KEYCODES[i].length];
      for(int j = 0; j < SHORTCUT_KEYCODES[i].length; j++) {
        SHORTCUT_KEYSTROKES[i][j] = KeyStroke.getKeyStroke(SHORTCUT_KEYCODES[i][j], modifiers);
      }
    }
  }

  /** Empty KeyStrokes array, returned for inappropriate indices. */
  private static final KeyStroke[] EMPTY_KEYSTROKES = {};

  /**
   * @param index The formlet index.
   * @return The array of KeyStrokes used as this formlet's quick
   * navigation shortcuts (OS-dependent).
   */
  private static KeyStroke[] getShortcutKeystrokes(int index) {
    if(index < 0 || index > 9) {
      return EMPTY_KEYSTROKES;
    }
    return SHORTCUT_KEYSTROKES[index];
  }

  private final JLabel myTitleLabel = new JLabel();
  private final ALabel myRightMnemonicLabel = new ALabel();
  private final JLabel myCaption = new OneLineLabel();
  private final AToolbar myToolbar = new AToolbar();

  private final Formlet myFormlet;
  private final String myTitle;
  private final Map<TypedKey<String>, String> myTooltips = Collections15.hashMap();
  private final MyListener myListener = new MyListener();
  private final Lifecycle myLife = new Lifecycle(false);
  private List<ToolbarEntry> myLastActions;
  private final int myIndex;
  private static final char NO_CHAR = '\uFFFF';
  private static final JLabel SAMPLE_LABEL = new JLabel("9.");
  public final AToolbarButton myExpandButton = new AToolbarButton(EXPAND_SECTION_ACTION);

  public FormletTitle(Formlet formlet, String title, int index) {
    super(new BorderLayout());
    myIndex = index;
    myTitle = title;
    myFormlet = formlet;
    myTitleLabel.getActionMap().put(EXPAND_OR_FOCUS, new ExpandOrFocusAction());
    init();
  }

  public void setTooltip(TypedKey<String> key, String tooltip) {
    key.putTo(myTooltips, tooltip);
  }

  public void setCaption(@Nullable String caption) {
    myCaption.setText(caption);
    updateTitle();
  }

  private void updateTitle() {
    String caption = myCaption.getText();
    String title = caption == null || caption.trim().length() == 0 ? myTitle : myTitle + ":";
    myTitleLabel.setText(title);
    @NotNull TypedKey<String> tooltip;
    if (myFormlet.isCollapsed())
      tooltip = COLLAPSED_TOOLTIP;
    else if (SwingTreeUtil.isAncestorOfFocusOwner(myFormlet.getContent().getComponent()))
      tooltip = FOCUSED_TOOLTIP;
    else
      tooltip = EXPANDED_TOOLTIP;
    String ttip = tooltip.getFrom(myTooltips);
//    myTitleLabel.setToolTipText(ttip);
    myRightMnemonicLabel.setToolTipText(ttip);
  }

  private void init() {
    myRightMnemonicLabel.setAlignmentY(0.5F);
    myExpandButton.setAlignmentY(0.5F);
    Box right = Box.createHorizontalBox();
    right.add(myRightMnemonicLabel);
    right.add(myExpandButton);
    add(right, BorderLayout.EAST);

    myTitleLabel.setBorder(Aqua.isAqua() ? new EmptyBorder(0, 8, 0, 0) : new EmptyBorder(0, 5, 0, 0));
    myTitleLabel.setText(myTitle);
    UIUtil.adjustFont(myTitleLabel, -1, Font.BOLD, false);

    setTooltip(FormletTitle.FOCUSED_TOOLTIP, null);
    if (myIndex >= 0) {
      final String tooltip = "Press " + FormletTitle.getShortcutString(myIndex) + " to quickly navigate here";
      setTooltip(FormletTitle.EXPANDED_TOOLTIP, tooltip);
      setTooltip(FormletTitle.COLLAPSED_TOOLTIP, tooltip);

      for(final KeyStroke ks : FormletTitle.getShortcutKeystrokes(myIndex)) {
        myTitleLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, EXPAND_OR_FOCUS);
      }
    }

    myRightMnemonicLabel.setBorder(new EmptyBorder(0, 0, 0, 1));
    myRightMnemonicLabel.setText(getShortcutString(myIndex));
    final Color fg = ColorUtil.between(
        myRightMnemonicLabel.getForeground(),
        myRightMnemonicLabel.getBackground(),
        Aqua.isAqua() ? 0.3F : 0.6F);
    myRightMnemonicLabel.setForeground(fg);
    UIUtil.adjustFont(myRightMnemonicLabel, 0.75F, 0, true, 9);
//    Font font = myRightMnemonicLabel.getFont();
//    if (font != null) {
//      int oldStyle = font.getStyle();
//      int newStyle = oldStyle & (~Font.BOLD);
//      if (oldStyle != newStyle) {
//        myRightMnemonicLabel.setFont(font.deriveFont(newStyle));
//      }
//    }

    myCaption.putClientProperty("html.disable", Boolean.TRUE);
    myCaption.setPreferredSize(new Dimension(0, 0));

    myToolbar.setBorder(AwtUtil.EMPTY_BORDER);

    JPanel center = new JPanel(new BorderLayout());
    center.add(myToolbar, BorderLayout.WEST);
    center.add(myCaption, BorderLayout.CENTER);
    center.setBorder(OFFSET_BORDER);

    add(myTitleLabel, BorderLayout.WEST);
    add(center, BorderLayout.CENTER);

    setOpaque(true);
    DocumentFormAugmentor.DO_NOT_AUGMENT.putClientValue(this, Boolean.TRUE);

    ConstProvider.addRoleValue(this, FORMLET_HEADER, this);

    watchForExpand(this);
    watchForExpand(myTitleLabel);
    watchForExpand(myRightMnemonicLabel);
    watchForExpand(myCaption);
  }

  private void watchForExpand(JComponent component) {
    MegaMouseAdapter listener = new MegaMouseAdapter() {
      public void mouseReleased(MouseEvent e) {
        Object c = e.getSource();
        if (c instanceof JComponent) {
          ActionUtil.performAction(EXPAND_SECTION_ACTION, ((JComponent) c));
        }
      }

      public void mouseEntered(MouseEvent e) {
        myExpandButton.getModel().setRollover(true);
      }

      public void mouseExited(MouseEvent e) {
        myExpandButton.getModel().setRollover(false);
      }
    };
    component.addMouseListener(listener);
    component.addMouseMotionListener(listener);
  }

  public void addNotify() {
    super.addNotify();
    myLife.cycleStart();
    addToHeaderLineup();
    attach();
  }

  private void addToHeaderLineup() {
    WidthDrivenColumn column = SwingTreeUtil.findAncestorOfType(this, WidthDrivenColumn.class);
    if (column != null) {
      java.util.List<FormletTitle> list = LINEUP.getClientValue(column);
      if (list == null) {
        list = Collections15.arrayList();
        LINEUP.putClientValue(column, list);
      }
      list.add(this);
      recalculateLines(list);
      final java.util.List<FormletTitle> finalList = list;
      myLife.lifespan().add(new Detach() {
        protected void doDetach() throws Exception {
          finalList.remove(FormletTitle.this);
        }
      });
    }
  }

  private void recalculateLines(List<FormletTitle> list) {
    int max = 0;
    for (FormletTitle title : list) {
      JLabel label = title.myTitleLabel;
      String oldText = label.getText();
      label.setPreferredSize(null);
      label.setText(title.myTitle + ":");
      max = Math.max(label.getPreferredSize().width, max);
      label.setText(oldText);
    }
    // give extra 5
    max += 5;

    for (FormletTitle title : list) {
      JLabel label = title.myTitleLabel;
      label.setPreferredSize(new Dimension(max, label.getPreferredSize().height));
    }
  }

  private void attach() {
    myFormlet.getModifiable().addAWTChangeListener(myLife.lifespan(), myListener);
    updateFromFormlet();
  }

  private void updateFromFormlet() {
    setCaption(myFormlet.getCaption());
    setActions(myFormlet.getActions());
  }

  private void setActions(@Nullable List<? extends ToolbarEntry> actions) {
    if (actions == null || actions.size() == 0) {
      if (myLastActions != null) {
        myLastActions = null;
        myToolbar.setVisible(false);
        revalidate();
      }
    } else {
      if (!actions.equals(myLastActions)) {
        myLastActions = Collections15.arrayList(actions);
        myToolbar.removeAll();
        for (ToolbarEntry action : myLastActions) {
          action.addToToolbar(myToolbar);
        }
        myToolbar.setVisible(true);
        revalidate();
      }
    }
  }

  public void removeNotify() {
    myLife.cycleEnd();
    super.removeNotify();
  }

  public Formlet getFormlet() {
    return myFormlet;
  }


  private static class ExpandSectionAction extends SimpleAction {
    public ExpandSectionAction() {
      super((String)null, Icons.EXPAND_DOWN);
      setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      FormletTitle title = context.getSourceObject(FORMLET_HEADER);
      Formlet formlet = title.getFormlet();
      context.updateOnChange(formlet.getModifiable());
      if (!formlet.isCollapsible()) {
        context.setEnabled(EnableState.INVISIBLE);
        return;
      }
      if (formlet.isCollapsed()) {
        context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.EXPAND_DOWN);
        context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Expand section");
      } else {
        context.putPresentationProperty(PresentationKey.SMALL_ICON, Icons.COLLAPSE_UP);
        context.putPresentationProperty(PresentationKey.SHORT_DESCRIPTION, "Collapse section");
      }
      context.setEnabled(true);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      FormletTitle title = context.getSourceObject(FORMLET_HEADER);
      Formlet formlet = title.getFormlet();
      if (formlet.isCollapsible()) {
        formlet.toggleExpand();
      }
    }
  }


  private class MyListener implements ChangeListener {
    public void onChange() {
      updateFromFormlet();
    }
  }


  private class ExpandOrFocusAction extends AbstractAction {
    public void actionPerformed(ActionEvent e) {
      if (!myFormlet.isCollapsed()) {
        JComponent container = myFormlet.getContent().getComponent();
        Container ancestor = container.getFocusCycleRootAncestor();
        boolean focused = false;
        if (ancestor != null) {
          FocusTraversalPolicy policy = ancestor.getFocusTraversalPolicy();
          if (policy != null) {
            Component component = policy.getDefaultComponent(container);
            if (component != null) {
              component.requestFocusInWindow();
              focused = true;
            }
          }
        }
        if (!focused && container.isFocusable()) {
          container.requestFocusInWindow();
        }
        UIUtil.visitComponents(container, JComponent.class, new ElementVisitor<JComponent>() {
          public boolean visit(JComponent element) {
            if (element instanceof ACollectionComponent) {
              ((ACollectionComponent) element).getSelectionAccessor().ensureSelectionExists();
              return false;
            } else {
              return true;
            }
          }
        });
      } else if (myFormlet.isCollapsible()) {
        myFormlet.expand();
      }
    }
  }
}