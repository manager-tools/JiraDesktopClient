package com.almworks.api.application.viewer;

import com.almworks.api.application.viewer.textdecorator.TextDecoration;
import com.almworks.util.ErrorHunt;
import com.almworks.util.components.ReadOnlyTextFields;
import com.almworks.util.components.ScrollPaneBorder;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import com.almworks.util.ui.swing.AwtUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public abstract class TextComponentWrapper implements TextAreaWrapper {
  private static Border EMPTY_SCROLLPANE_BORDER = null;
  private final JScrollPane myScrollPane;
  protected static final SimpleAttributeSet COMMON_LINK_ATTR = new SimpleAttributeSet();
  static {
    COMMON_LINK_ATTR.addAttribute(StyleConstants.Foreground, ColorUtil.between(Color.BLUE, AwtUtil.getTextComponentForeground(), 0.1F));
    COMMON_LINK_ATTR.addAttribute(StyleConstants.Underline, true);
  }

  protected TextComponentWrapper(JTextComponent component, boolean viewer) {
    myScrollPane = setupWrapper(component, viewer);
  }

  @Nullable
  private static JScrollPane setupWrapper(JTextComponent component, boolean viewer) {
    if (viewer) {
      ReadOnlyTextFields.basicReadonlySetup(component);
      component.setBorder(getEmptyScrollPaneBorder());
      return null;
    } else {
      ScrollPaneBorder scrollPane = new ScrollPaneBorder(component);
      scrollPane.setOpaque(false);
      scrollPane.getViewport().setOpaque(false);
      return scrollPane;
    }
  }

  static Border getEmptyScrollPaneBorder() {
    Threads.assertAWTThread();
    if (EMPTY_SCROLLPANE_BORDER == null) {
      ScrollPaneBorder scrollpane = new ScrollPaneBorder(new JLabel());
      EMPTY_SCROLLPANE_BORDER = new EmptyBorder(scrollpane.getBorder().getBorderInsets(scrollpane));
    }
    return EMPTY_SCROLLPANE_BORDER;
  }

  public JComponent getComponent() {
    return myScrollPane != null ? myScrollPane : getTextComponent();
  }

  protected abstract JTextComponent getTextComponent();

  public int getPreferedHeight(CellState state, int width) {
    return calcPreferedHeight(width, getTextComponent(), myScrollPane);
  }

  private static int calcPreferedHeight(int width, JTextComponent component, JScrollPane scrollPane) {
    int addToHeight;
    if (scrollPane != null) {
      Insets scrollpaneInsets = AwtUtil.uniteInsetsFromTo((JComponent) component.getParent(), scrollPane);
      width -= AwtUtil.getInsetWidth(scrollpaneInsets);
      addToHeight = AwtUtil.getInsetHeight(scrollpaneInsets);
    } else
      addToHeight = 0;
    return UIUtil.getTextComponentPreferredHeight(component, width) + addToHeight;
  }

  public void paintAt(Graphics g, int x, int y) {
    View rootView = getRootView();
    JComponent component = (JComponent) rootView.getContainer();
    rootView
      .paint(g, new Rectangle(x, y, component.getWidth() - AwtUtil.getInsetWidth(component),
        component.getHeight() - AwtUtil.getInsetHeight(component)));
//      g.translate(-x, -y);
//      myArea.paint(g);
//      g.translate(x, y);
  }

  private View getRootView() {
    JTextComponent component = getTextComponent();
    View rootView = component.getUI().getRootView(component);
    return rootView;
  }

  @Nullable
  public String getTooltipAt(int x, int y) {
    return null;
  }

  public boolean processMouse(MouseEvent e) {
    return processMouse(e, getTextComponent());
  }

  public static boolean processMouse(MouseEvent e, JTextComponent component) {
    if (e.getID() == MouseEvent.MOUSE_EXITED) return false;
    TextDecoration link = findLink(component, e.getPoint());
    if (link == null) return false;
    e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    if (link != null && e.isPopupTrigger())
      showPopup(e, link);
    else if (link != null && e.getID() == MouseEvent.MOUSE_CLICKED && e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1)
      processMouse(e, link);
    return true;
  }

  private static void showPopup(MouseEvent e, TextDecoration link) {
    MenuBuilder builder = new MenuBuilder();
    builder.addDefaultAction(link.getDefaultAction());
    builder.addAllActions(link.getNotDefaultActions());
    builder.showPopupMenu(e);
  }

  private static TextDecoration findLink(JTextComponent textComponent, Point point) {
    int pos = textComponent.viewToModel(point);
    if (!checkTextPosition(textComponent, pos, point))
      return null;
    View view = textComponent.getUI().getRootView(textComponent);
    Element element = ViewWrapper.findElementAtPosition(view, pos);
    if (element == null)
      return null;
    TextDecoration link = null;
    while (element != null) {
      link = (TextDecoration) element.getAttributes().getAttribute(TextDecoration.ATTRIBUTE);
      if (link != null)
        return link;
      element = element.getParentElement();
    }
    return null;
  }

  private static boolean checkTextPosition(JTextComponent component, int pos, Point point) {
    try {
      if (pos > 0) {
        Rectangle rect = component.modelToView(pos - 1);
        if (rect.y > point.y || rect.y+rect.height < point.y)
          return false;
      }
      if (pos < component.getDocument().getLength() - 1) {
        Rectangle rect = component.modelToView(pos + 1);
        if (rect.y > point.y || rect.y+rect.height < point.y)
          return false;
      }
    } catch (BadLocationException e) {
      return false;
    }
    return true;
  }

  private static void processMouse(MouseEvent e, TextDecoration link) {
    Component c = e.getComponent();
    if (!(c instanceof JComponent))
      return;
    JComponent component = (JComponent) c;
    AnAction action = link.getDefaultAction();
    if (action == null)
      return;
    UpdateContext context = DefaultUpdateContext.singleUpdate(component);
    try {
      action.update(context);
    } catch (CantPerformException e1) {
      return;
    }
    if (context.isDisabled())
      return;
    ActionUtil.performAction(action, component);
    e.consume();
  }

  public void setTextForeground(Color foreground) {
    getTextComponent().setForeground(foreground);
  }

  public void selectAll() {
    if (getTextComponent().getDocument() != null)
      getTextComponent().selectAll();
  }

  public void scrollToBeginning() {
    JTextComponent component = getTextComponent();
    if (component.getDocument() != null) {
      component.setCaretPosition(0);
      component.moveCaretPosition(0);
    }
  }

  public static void setComponentText(JTextComponent component, String text) {
    if (component instanceof JEditorPane) {
      JEditorPane pane = (JEditorPane) component;
      ErrorHunt.setEditorPaneText(pane, text);
    }
    component.setText(text);
  }
}
