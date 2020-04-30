package com.almworks.util.components;

import com.almworks.util.ReflectionUtil;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.swing.Shortcuts;
import com.almworks.util.ui.swing.SwingTreeUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;

/**
 * @author dyoma
 */
public abstract class DropDownListener {
  public static final String HIDE_DROP_DOWN = "hideDropDown";

  private static ComponentProperty<DropDownListener> LISTENER = ComponentProperty.createProperty("dropDownListener");

  private final AbstractAction HIDE_DROP_DOWN_ACTION = new AbstractAction() {
    public void actionPerformed(ActionEvent e) {
      final Object o = e.getSource();
      if(o instanceof JComponent) {
        final DropDownListener listener = LISTENER.getClientValue((JComponent)o);
        if(listener != null) {
          myCancel = true;
          listener.hideDropDown();
        }
      }
    }
  };

  private JWindow myDropDownWindow;
  private int myDropDownReasons = 0;
  private boolean myCancel = false;

  protected void toggleDropDown(Window parentWindow) {
    if (myDropDownWindow == null)
      showDropDown(parentWindow);
    else
      hideDropDown();
  }

  public void showDropDown(Component component) {
    assert component != null;
    showDropDown(SwingTreeUtil.findAncestorOfType(component, Window.class));
  }

  public void showDropDown(Window parentWindow) {
    assert parentWindow != null;
    if(myDropDownWindow != null) {
      return;
    }

    final JComponent component = createPopupComponent();
    if(component == null) {
      return;
    }

    LISTENER.putClientValue(component, this);

    createDropDownWindow(parentWindow, component);
    seupWindowLocation();
    setupWindowFocusHandling();
    setupHideOnEscape(component);
    showAndRequestFocus(component);
    myCancel = false;
    onDropDownShown();
  }

  private void createDropDownWindow(Window parentWindow, JComponent component) {
    myDropDownWindow = new DropDownWindow(parentWindow);
    myDropDownWindow.getContentPane().add(component);
    myDropDownWindow.pack();
    myDropDownWindow.pack(); // Pack twice because of some component inserted into scollPanes has to be validated with new window size before can provide right prefered size
  }

  private void seupWindowLocation() {
    final Rectangle owner = getScreenLocation();
    final GraphicsConfiguration gc = UIUtil.getGraphicsConfigurationForPoint(owner.getLocation());
    final Rectangle screen = UIUtil.getScreenUserSize(gc);
    final Point position = UIUtil.NOTICE_POPUP.getPosition(screen, owner, myDropDownWindow.getSize());
    myDropDownWindow.setLocation(position);
  }

  private void setupWindowFocusHandling() {
    final WindowAdapter childHandler = new WindowAdapter() {
      @Override
      public void windowLostFocus(WindowEvent e) {
        if(!isChildWindow(e.getOppositeWindow())) {
          decrementDropDownReasons();
        }
      }

      @Override
      public void windowClosed(WindowEvent e) {
        e.getWindow().removeWindowFocusListener(this);
        e.getWindow().removeWindowListener(this);
      }
    };

    final WindowFocusListener mainHandler = new WindowFocusListener() {
      @Override
      public void windowGainedFocus(WindowEvent e) {
        if(!isChildWindow(e.getOppositeWindow())) {
          incrementDropDownReasons();
        }
      }

      @Override
      public void windowLostFocus(WindowEvent e) {
        final Window opposite = e.getOppositeWindow();
        if(!isChildWindow(opposite)) {
          decrementDropDownReasons();
        } else {
          opposite.addWindowListener(childHandler);
          opposite.addWindowFocusListener(childHandler);
        }
      }
    };

    myDropDownWindow.addWindowFocusListener(mainHandler);
  }

  private boolean isChildWindow(Window child) {
    return isChildWindow(myDropDownWindow, child);
  }

  private boolean isChildWindow(Window parent, Window child) {
    if(child == null || parent == null) {
      return false;
    }

    if(parent == child) {
      return true;
    }

    for(final Window w : parent.getOwnedWindows()) {
      if(isChildWindow(w, child)) {
        return true;
      }
    }
    return false;
  }

  protected void incrementDropDownReasons() {
    myDropDownReasons++;
  }

  protected void decrementDropDownReasons() {
    if (myDropDownReasons > 0) {
      if (--myDropDownReasons == 0) {
        JWindow window = myDropDownWindow;
        if (window != null)
          window.dispose();
      }
    }
  }

  private void setupHideOnEscape(JComponent component) {
    component.getActionMap().put(HIDE_DROP_DOWN, HIDE_DROP_DOWN_ACTION);
    component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Shortcuts.ESCAPE, HIDE_DROP_DOWN);
  }

  private void showAndRequestFocus(JComponent component) {
    myDropDownWindow.setVisible(true);
    myDropDownWindow.toFront();
    myDropDownWindow.requestFocus();
    component.requestFocus();
  }

  protected void onDropDownShown() {
  }

  protected void onDropDownHidden() {
  }

  protected void onDropDownCancelled() {
    onDropDownHidden();
  }

  protected void hideDropDown() {
    if (myDropDownWindow == null)
      return;
    myDropDownWindow.dispose();
  }

  public boolean isDropDownShown() {
    return myDropDownWindow != null;
  }

  public JWindow getDropDownWindow() {
    return myDropDownWindow;
  }

  @Nullable
  protected abstract JComponent createPopupComponent();

  protected abstract Rectangle getScreenLocation();

  public static boolean isDropDownComponent(Component v) {
    while (v != null) {
      if (v instanceof DropDownWindow) return true;
      v = v.getParent();
    }
    return false;
  }

  public static abstract class ForComponent extends DropDownListener implements AnActionListener, ActionListener {
    private final Component myComponent;

    protected ForComponent(Component component) {
      myComponent = component;
    }

    public void toggleDropDown() {
      toggleDropDown(SwingTreeUtil.findAncestorOfType(myComponent, Window.class));
    }

    protected Rectangle getScreenLocation() {
      return new Rectangle(myComponent.getLocationOnScreen(), myComponent.getSize());
    }

    public void perform(ActionContext context) {
      toggleDropDown();
    }

    public void actionPerformed(ActionEvent e) {
      toggleDropDown();
    }
  }


  private class DropDownWindow extends JWindow {
    public DropDownWindow(Window parentWindow) {
      super(parentWindow);
      Method setType = ReflectionUtil.findMethod(Window.class, "setType");
      Object POPUP = ReflectionUtil.findEnumConstant("java.awt.Window$Type", "POPUP");
      if (setType != null && POPUP != null) ReflectionUtil.safeInvoke(this, setType, POPUP);
    }

    public void dispose() {
      if (this == myDropDownWindow)
        myDropDownWindow = null;
      myDropDownReasons = 0;
      super.dispose();
      if(myCancel) {
        onDropDownCancelled();
      } else {
        onDropDownHidden();
      }
      myCancel = false;
    }
  }
}
