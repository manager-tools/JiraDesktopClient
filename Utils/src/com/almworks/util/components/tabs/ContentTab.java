package com.almworks.util.components.tabs;

import com.almworks.util.commons.Condition;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.DataRole;
import com.almworks.util.ui.actions.DefaultActionContext;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * @author dyoma
 */
public class ContentTab {
  public static final DataRole<ContentTab> DATA_ROLE = DataRole.createRole(ContentTab.class);
  @NotNull
  private final TabsManager myManager;
  private final Map<TypedKey<?>, Object> myProperties = Collections15.hashMap();
  @Nullable
  private String myName = null;
  @Nullable
  private String myTooltip = null;
  @Nullable
  private UIComponentWrapper myWrapper = null;
  private boolean myVisible = true;
  private final MenuBuilder myMenuBuilder = new MenuBuilder();
  public static final Condition<ContentTab> SHOWN = new Condition<ContentTab>() {
    public boolean isAccepted(ContentTab value) {
      return value.isShowing();
    }
  };

  public ContentTab(@NotNull TabsManager manager) {
    myManager = manager;
    resetTabMenu();
  }

  public MenuBuilder getMenuBuilder() {
    return myMenuBuilder;
  }

  /**
   * Changes the content {@link UIComponentWrapper} to new one, unless the same {@link UIComponentWrapper} already shown. <br>
   * If the new {@link UIComponentWrapper} (not same) wraps the same JComponent (actually is going to provide the same JComponent) then
   * first disposes previous {@link UIComponentWrapper} and then gets new JComponent and inserts it.
   *
   * @param component new {@link UIComponentWrapper} to be shown. <code>null</code> means that no content component should be
   *                  shown and the tab become invisible.
   */
  public void setComponent(@Nullable UIComponentWrapper component) {
    if (myWrapper == component)
      return;
    disposeComponent();
    myWrapper = component;
    if (component != null) {
      TabsManager.TAB.putClientValue(component.getComponent(), this);
      if (myVisible)
        insertTab();
    }
  }

  /**
   * Changes content component to new one, unless the same component is already shown
   *
   * @param component new content component. <code>null</code> means hide tab
   * @see #setComponent(com.almworks.util.ui.UIComponentWrapper)
   */
  public void setComponent(@Nullable JComponent component) {
    if (myWrapper != null && myWrapper.getComponent() == component)
      return;
    if (component != null)
      setComponent(new UIComponentWrapper.Simple(component));
    else
      setComponent((UIComponentWrapper) null);
  }

  public void setName(@Nullable String name) {
    if (getName().equals(name))
      return;
    myName = name;
    if (isShowing())
      myManager.updateTab(this);
  }

  public void setTooltip(@Nullable String tooltip) {
    if (tooltip != null)
      tooltip = tooltip.trim();
    if (tooltip != null && tooltip.length() == 0)
      tooltip = null;
    if (Util.equals(myTooltip, tooltip))
      return;
    myTooltip = tooltip;
    if (isShowing())
      myManager.updateTab(this);
  }

  public <T> void setUserProperty(TypedKey<? extends T> key, T value) {
    ((TypedKey<T>) key).putTo(myProperties, value);
  }

  public <T> T getUserProperty(TypedKey<? extends T> key) {
    //noinspection ConstantConditions
    return key.getFrom(myProperties);
  }

  public boolean isShowing() {
    return myVisible && myWrapper != null && myWrapper.getComponent() != null;
  }

  public boolean isSelected() {
    return isShowing() && myManager.getSelectedTab() == this;
  }

  public void delete() {
    disposeComponent();
    myManager.deleteTab(this);
  }

  public void toggleExpand(JComponent context) {
    try {
      TabActions.EXPAND_SHRINK_TABS.perform(new DefaultActionContext(context));
    } catch (CantPerformException e) {
      Log.warn(e);
    }
  }

  void disposeComponent() {
    UIComponentWrapper wrapper = myWrapper;
    if (wrapper == null)
      return;
    hideTab();
    TabsManager.TAB.putClientValue(wrapper.getComponent(), null);
    wrapper.dispose();
    myWrapper = null;
  }

  private void insertTab() {
    if (myWrapper != null)
      myManager.insertTab(this);
  }

  private void hideTab() {
    if (myVisible && myWrapper != null)
      myManager.removeComponent(this);
  }

  @Nullable
  public UIComponentWrapper getComponent() {
    return myWrapper;
  }

  @NotNull
  public String getName() {
    return myName != null ? myName : "";
  }

  @Nullable
  public String getTooltip() {
    return myTooltip;
  }

  @NotNull
  JComponent getJComponent() {
    UIComponentWrapper wrapper = myWrapper;
    assert wrapper != null : this;
    JComponent component = wrapper.getComponent();
    assert component != null : this;
    return component;
  }

  public void clearComponent() {
    setComponent((UIComponentWrapper) null);
  }

  public void setVisible(boolean visible) {
    if (myVisible == visible)
      return;
    if (visible) {
      myVisible = true;
      insertTab();
    } else {
      hideTab();
      myVisible = false;
    }
  }

  void showPopup(MouseEvent e) {
    myMenuBuilder.showPopupMenu(e);
  }

  public String toString() {
    return "ContentTab [name=" + myName + " visible=" + myVisible + " component=" + myWrapper + "]@" +
      Integer.toHexString(System.identityHashCode(this));
  }

  public void select() {
    myManager.selectTab(this);
  }

  public void resetTabMenu() {
    myMenuBuilder.clearMenu();
    myMenuBuilder.addAction(TabActions.CLOSE_TAB);
    myMenuBuilder.addAction(TabActions.CLOSE_ALL);
    myMenuBuilder.addAction(TabActions.CLOSE_ALL_BUT_THIS);
    myMenuBuilder.addSeparator();
  }

  public void requestFocusInWindow() {
    myManager.requestFocusInWindow();
  }
}
