package com.almworks.util.ui.actions.presentation;

import com.almworks.util.advmodel.AListModel;
import com.almworks.util.components.ACollectionComponent;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.PopupMenuListener;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author dyoma
 */
public class MenuBuilder {
  public static DataRole<InputEvent> POPUP_INPUT_EVENT = DataRole.createRole(InputEvent.class);

  private final MenuBuilder myParent;
  private final PopupEntry.CompositeMenuEntry myRoot;
  private final boolean myAllowDefault;

  private MenuBuilder(PopupEntry.CompositeMenuEntry subMenuEntry, boolean allowDefault, MenuBuilder parent) {
    myRoot = subMenuEntry;
    myAllowDefault = allowDefault;
    myParent = parent;
  }

  public MenuBuilder() {
    this(new RootMenuEnty(), true, null);
  }

  public static void addActions(ActionGroupVisitor component, Iterator<? extends AnAction> actions) {
    addActions(component, actions, null);
  }

  public static void addActions(ActionGroupVisitor component, Iterator<? extends AnAction> actions,
    Map<String, PresentationMapping<?>> override)
  {
    while (actions.hasNext()) {
      AnAction action = actions.next();
      if (override != null)
        component.visitAction(action, override);
      else
        component.visitAction(action);
    }
  }

  public MenuBuilder addAction(String actionId) {
    return addAction(new IdActionProxy(actionId));
  }

  public MenuBuilder addAction(AnAction action) {
    return addEntry(new PopupEntry.ActionEntry(action));
  }

  public MenuBuilder addEnabledActionsFromModel(final AListModel<? extends AnAction> actions) {
    return addEntry(new PopupEntry.EnabledActionsEntry(actions));
  }

  public MenuBuilder addActions(AnAction... actions) {
    for (AnAction action : actions) {
      addAction(action);
    }
    return this;
  }

  public MenuBuilder addAllActions(List<? extends AnAction> actions) {
    for (AnAction action : actions) {
      addAction(action);
    }
    return this;
  }

  public MenuBuilder addAllActionIds(Collection<String> actionIds) {
    for (String actionId : actionIds) addAction(actionId);
    return this;
  }

  public MenuBuilder addDefaultAction(String actionId) {
    return addDefaultAction(new IdActionProxy(actionId));
  }

  public MenuBuilder addDefaultAction(final AnAction action) {
    if (!myAllowDefault)
      return addAction(action);
    return addEntry(new PopupEntry.DefaultPopupEntry(action));
  }

  public MenuBuilder addEntry(PopupEntry entry) {
    myRoot.addEntry(entry);
    return this;
  }

  public void clearMenu() {
    myRoot.clearEntries();
  }

  public MenuBuilder addSeparator() {
    return addEntry(PopupEntry.SEPARATOR);
  }

  public void addToComponent(Lifespan life, final JComponent component) {
    assert !(component instanceof ACollectionComponent<?>) : component;
    life.add(ActionUtil.setDefaultActionHandler(new AnActionListener() {
      public void perform(ActionContext context) {
        invokeDefaultAction(component);
      }
    }, component));
    new PopupMenuListener() {
      @Override
      protected void showPopup(JComponent component, int x, int y, InputEvent event) {
        MenuBuilder.this.showPopupMenu(component, x, y, event);
      }
    }.attach(life, component);
  }

  public MenuBuilder addToggleAction(final AnAction action) {
    return addEntry(new PopupEntry.ToggleActionEntry(action));
  }

  public MenuBuilder addToggleAction(String actionId) {
    return addToggleAction(new IdActionProxy(actionId));
  }

  public void buildMenu(ActionGroupVisitor popupComponent) {
    myRoot.addToPopup(popupComponent);
  }

  @Nullable
  public JPopupMenu createPopupWindow(@NotNull JComponent contextComponent, @Nullable InputEvent event) {
    final JPopupMenu popup = UIUtil.createJPopupMenu();
    ComponentProperty.JUMP.putClientValue(popup, contextComponent);
    if (event != null) {
      ConstProvider.addRoleValue(popup, POPUP_INPUT_EVENT, event);
    }
    buildMenu(ActionGroupVisitor.ComponentBuilder.popupBuilder(popup, contextComponent));
    Component[] components = popup.getComponents();
    if (!ActionUtil.updateMenu(Arrays.asList(components)))
      return null;
    return popup;
  }

  public MenuBuilder createSubMenu(PopupEntry.CompositeMenuEntry entry) {
    addEntry(entry);
    return new MenuBuilder(entry, false, this);
  }

  public MenuBuilder createSubMenu(String name) {
    return createSubMenu(new PopupEntry.SubMenuEntry(name));
  }

  public MenuBuilder endSubMenu() {
    return myParent;
  }

  public void invokeDefaultAction(@NotNull JComponent component) {
    DefaultActionLookup lookup = new DefaultActionLookup(component);
    myRoot.addToPopup(lookup);
    AnAction action = lookup.getDefaultAction();
    if (action == null)
      return;
    ActionUtil.performAction(action, component);
  }

  public void showPopupMenu(MouseEvent e) {
    Component component = e.getComponent();
    assert component instanceof JComponent : e.getSource() + " " + e;
    int x = e.getX();
    int y = e.getY();
    showPopupMenu((JComponent) component, x, y, e);
  }

  public void showPopupMenu(JComponent component, int x, int y, @Nullable InputEvent event) {
    if (component == null || !component.isShowing())
      return;
    final JPopupMenu popup = createPopupWindow(component, event);
    if (popup == null)
      return;
    if (popup.getComponentCount() > 0) {
      popup.pack();
      popup.show(component, x, y);
    }
  }

  private static class RootMenuEnty extends PopupEntry.CompositeMenuEntry {
    public void addToPopup(ActionGroupVisitor popup) {
      doAddToMenu(popup);
    }
  }


  private static class DefaultActionLookup extends ActionGroupVisitor.DummyVisitor {
    private final JComponent myComponent;
    private AnAction myDefaultAction;

    public DefaultActionLookup(JComponent component) {
      myComponent = component;
      myDefaultAction = null;
    }

    public JComponent getContextComponent() {
      return myComponent;
    }

    public AnAction getDefaultAction() {
      return myDefaultAction;
    }

    public void visitDefault(AnAction action) {
      if (myDefaultAction != null)
        return;
      if (ActionUtil.isActionEnabled(action, myComponent))
        myDefaultAction = action;
    }
  }
}
