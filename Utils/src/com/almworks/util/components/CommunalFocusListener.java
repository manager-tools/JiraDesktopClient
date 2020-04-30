package com.almworks.util.components;

import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * @author dyoma
 */
public class CommunalFocusListener<C extends JComponent> implements FocusListener {
  private static final long MOUSE_POPUP_ASSUMED_TIME = 100;
  private final List<Activity<? super C>> myActivities = Collections15.arrayList();
  private final C myComponent;
  private Component myInitialFocusOwner;

  // keep the time of last mouse click in the field. if focus is lost quickly after mouse has been clicked,
  // we think that this is popup event
  private long myLastMouseEventTime;

  private CommunalFocusListener(C component) {
    assert component != null;
    myComponent = component;
  }

  public static <C extends JComponent> CommunalFocusListener<C> create(C component) {
    return new CommunalFocusListener(component);
  }

  public void focusGained(FocusEvent e) {
    Component c = e.getOppositeComponent();
    CommunalFocusListener<?> other = searchCommunalFocusListener(c);

    if (other == null) {
      myInitialFocusOwner = c;
    } else {
      myInitialFocusOwner = other.getInitialFocusOwner();
    }

    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myActivities.size(); i++) {
      Activity<? super C> activity = myActivities.get(i);
      activity.onFocusGained(myComponent);
    }
  }

  protected JComponent getComponent() {
    return myComponent;
  }

  public void focusLost(FocusEvent e) {
    if (handleFocusLostToPopup(e))
      return;
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < myActivities.size(); i++) {
      Activity<? super C> activity = myActivities.get(i);
      activity.onFocusLost(myComponent);
    }
  }

  public void addActivity(Activity<? super C> activity) {
    myActivities.add(activity);
  }

  public void clear() {
    myInitialFocusOwner = null;
  }

  public Component getInitialFocusOwner() {
    return myInitialFocusOwner;
  }

  public void onMouseEvent(MouseEvent e) {
    myLastMouseEventTime = System.currentTimeMillis();
  }

  private boolean handleFocusLostToPopup(FocusEvent e) {
    if (e == null)
      return false;

    final Component opposite = e.getOppositeComponent();
    if (opposite == null)
      return false;

    if (!(opposite instanceof JRootPane))
      return false;

    long time = System.currentTimeMillis();
    return time <= myLastMouseEventTime + MOUSE_POPUP_ASSUMED_TIME;
  }

  @Nullable
  private static CommunalFocusListener<?> searchCommunalFocusListener(Component c) {
    if (c == null)
      return null;
    FocusListener[] listeners = c.getFocusListeners();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < listeners.length; i++) {
      FocusListener listener = listeners[i];
      if (listener instanceof CommunalFocusListener) {
        return (CommunalFocusListener<?>) listener;
      }
    }
    return null;
  }

  private static Detach listenEscape(final JComponent component) {
    InputMap inputMap = component.getInputMap(JComponent.WHEN_FOCUSED);
    final Object prevAction = inputMap.get(Shortcuts.ESCAPE);
    if (prevAction != null)
      Log.warn(inputMap.get(Shortcuts.ESCAPE) + " is overriden for " + component + " replaced with communal focus support");
    inputMap.put(Shortcuts.ESCAPE, "focusNextOrNone");
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        CommunalFocusListener<?> focusListener = searchCommunalFocusListener(component);
        Component component = focusListener == null ? null : focusListener.getInitialFocusOwner();
        if (component == null)
          KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        else
          component.requestFocusInWindow();
      }
    };
    component.getActionMap().put("focusNextOrNone", action);
    return new Detach() {
      protected void doDetach() {
        if (prevAction == null) component.getInputMap(JComponent.WHEN_FOCUSED).remove(Shortcuts.ESCAPE);
        else component.getInputMap(JComponent.WHEN_FOCUSED).put(Shortcuts.ESCAPE, prevAction);
        component.getActionMap().remove("focusNextOrNone");
      }
    };
  }

  public Detach attach() {
    myComponent.addFocusListener(this);
    focusLost(null);

    final MouseAdapter mouseListener = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        onMouseEvent(e);
      }

      public void mouseReleased(MouseEvent e) {
        onMouseEvent(e);
      }
    };
    myComponent.addMouseListener(mouseListener);
    final Detach escDetach = listenEscape(myComponent);

    return new Detach() {
      protected void doDetach() {
        focusLost(null);
        myComponent.removeMouseListener(mouseListener);
        myComponent.removeFocusListener(CommunalFocusListener.this);
        clear();
        escDetach.detach();
      }
    };
  }

  public static void setupJTextField(JTextField component) {
    ReadOnlyTextFields.basicReadonlySetup(component);
    CommunalFocusListener.ChangeBorder.prepare(component);
    CommunalFocusListener<JTextField> modificationFocus = new CommunalFocusListener(component);
    modificationFocus.addActivity(HANDLE_TEXT);
    modificationFocus.addActivity(ChangeBorder.FOCUSED_COMPONENT);
    modificationFocus.attach();
  }

  public static void setupJTextArea(JTextArea component) {
    commonTextComponentSetup(component);
  }

  public static void setupJEditorPane(JEditorPane component) {
    commonTextComponentSetup(component);
  }

  /**
   * Actually is applicable to {@link JTextArea} and {@link JEditorPane}.
   * These components should be inserted into {@link JScrollPane}
   */
  private static void commonTextComponentSetup(JTextComponent component) {
    ReadOnlyTextFields.basicReadonlySetup(component);
    CommunalFocusListener<JTextComponent> focus = CommunalFocusListener.create(component);
    focus.addActivity(HANDLE_TEXT);
    focus.addActivity(MAKE_CARET_VISIBLE);
    ChangeBorder<JComponent> changeBorder = new ChangeBorder<JComponent>() {
      protected JComponent getTargetComponent(JComponent component) {
        return SwingTreeUtil.findAncestorOfType(component, ScrollPaneBorder.class);
      }
    };
    changeBorder.addTo(focus);
    focus.attach();
    setupJTextAreaPopupPatch(component);
  }

  private static Detach setupJTextAreaPopupPatch(final JTextComponent component) {
    final MouseAdapter listener = new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (!e.isPopupTrigger())
          return;
        if (component.hasFocus())
          return;
        component.selectAll(); // Keep this line. The same will be done by HANDLE_TEXT Activity when Summary'll gain focus,
        // but we need text to be selected before menu pops up.
        component.requestFocusInWindow();
      }

      public void mousePressed(MouseEvent e) {
        mouseClicked(e);
      }

      public void mouseReleased(MouseEvent e) {
        mouseClicked(e);
      }
    };
    component.addMouseListener(listener);
    return new Detach() {
      protected void doDetach() {
        component.removeMouseListener(listener);
      }
    };
  }

  public interface Activity<C extends JComponent> {
    void onFocusGained(C component);

    void onFocusLost(C component);
  }


  public static abstract class ChangeBorder<C extends JComponent> implements Activity<JComponent> {
    public static final Activity<JComponent> FOCUSED_COMPONENT = new ChangeBorder<JComponent>() {
      @NotNull
      protected JComponent getTargetComponent(JComponent component) {
        return component;
      }
    };
    private static final ComponentProperty<Border> DEFAULT_BORDER = ComponentProperty.createProperty("DEFAULT_BORDER");
    private static final ComponentProperty<Border> EMPTY_BORDER = ComponentProperty.createProperty("EMPTY_BORDER");

    public void onFocusGained(JComponent component) {
      C target = getTargetComponent(component);
      if (target != null) {
        Border border = DEFAULT_BORDER.getClientValue(target);
        if (border != null) {
          target.setBorder(border);
        }
      }
    }

    public void onFocusLost(JComponent component) {
      C target = getTargetComponent(component);
      if (target != null) {
        Border border = EMPTY_BORDER.getClientValue(target);
        if (border != null)
          target.setBorder(border);
      }
    }

    public <T extends JComponent> void addTo(CommunalFocusListener<T> focus) {
      JComponent component = focus.getComponent();
      C comp = getTargetComponent(component);
      if (comp != null) {
        prepare(comp);
      }
      focus.addActivity(this);
    }

    @Nullable
    protected abstract C getTargetComponent(JComponent component);

    public static void prepare(JComponent component) {
      Border border = component.getBorder();
      DEFAULT_BORDER.putClientValue(component, border);
      border = border != null ? new EmptyBorder(border.getBorderInsets(component)) : null;
      EMPTY_BORDER.putClientValue(component, border);
      component.setBorder(border);
    }

    public static Border getEmptyBorder(JComponent component) {
      return EMPTY_BORDER.getClientValue(component);
    }
  }


  public static final Activity<JTextComponent> HANDLE_TEXT = new Activity<JTextComponent>() {
    public void onFocusGained(JTextComponent component) {
      int length = component.getDocument().getLength();
      if (length > 0) {
        component.setCaretPosition(length);
        component.moveCaretPosition(0);
      }
    }

    public void onFocusLost(JTextComponent component) {
      UIUtil.scrollToTop(component);
      component.setCaretPosition(0);
    }
  };

  public static final Activity<JTextComponent> MAKE_CARET_VISIBLE = new Activity<JTextComponent>() {
    private final ComponentProperty<Boolean> WAS_VISIBLE = ComponentProperty.createProperty("caretWasVisible");

    public void onFocusGained(JTextComponent component) {
      WAS_VISIBLE.putClientValue(component, component.getCaret().isVisible());
      component.getCaret().setVisible(true);
    }

    public void onFocusLost(JTextComponent component) {
      Boolean wasVisible = WAS_VISIBLE.getClientValue(component);
      if (wasVisible == null)
        return;
      component.getCaret().setVisible(wasVisible);
    }
  };
}
