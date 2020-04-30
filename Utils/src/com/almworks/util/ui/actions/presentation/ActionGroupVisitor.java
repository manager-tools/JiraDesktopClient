package com.almworks.util.ui.actions.presentation;

import com.almworks.util.components.AMenu;
import com.almworks.util.components.NeighbourAwareSeparator;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.AnAction;
import com.almworks.util.ui.actions.PresentationMapping;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author dyoma
 */
public interface ActionGroupVisitor {
  Icon EMPTY_ICON = new EmptyIcon(1, 16);

  void visitAction(AnAction action);

  void visitAction(AnAction action, Map<String, PresentationMapping<?>> override);

  void visitToggle(AnAction action);

  void visitDefault(AnAction action);

  void visitGroup(PopupEntry.CompositeMenuEntry group, NameMnemonic name);

  JComponent getContextComponent();

  void visitSeparator();

  abstract class DummyVisitor implements ActionGroupVisitor {
    public void visitAction(AnAction action) {
      visitAction(action, null);
    }

    public void visitAction(AnAction action, Map<String, PresentationMapping<?>> override) {
    }

    public void visitToggle(AnAction action) {
    }

    public void visitDefault(AnAction action) {
    }

    public void visitGroup(PopupEntry.CompositeMenuEntry group, NameMnemonic name) {
    }

    public void visitSeparator() {
    }
  }

  abstract class ComponentBuilder implements ActionGroupVisitor {
    private final JComponent myContext;
    private final ComponentFactory myFactory;

    protected ComponentBuilder(JComponent context, ComponentFactory factory) {
      myContext = context;
      myFactory = factory;
    }

    public JComponent getContextComponent() {
      return myContext;
    }

    public void visitAction(AnAction action) {
      visitAction(action, null);
    }

    public void visitAction(AnAction action, Map<String, PresentationMapping<?>> override) {
      AActionComponent<?> item = myFactory.createActionComponent();
      item.setAnAction(action);
      if (override != null) {
        item.overridePresentation((Map)override);
      }
      addComponent(item.toComponent());
    }

    public void visitToggle(AnAction action) {
      AActionComponent<?> checkBoxMenuItem = myFactory.createCheckBoxComponent();
      checkBoxMenuItem.setAnAction(action);
      addComponent(checkBoxMenuItem.toComponent());
    }

    public void visitDefault(AnAction action) {
      AMenuItem item = (AMenuItem) myFactory.createDefaultComponent();
      item.setAnAction(action);
      addComponent(item.toComponent());
    }

    public void visitSeparator() {
      addComponent(new NeighbourAwareSeparator());
    }

    public void visitGroup(PopupEntry.CompositeMenuEntry group, NameMnemonic name) {
      AMenu menu = myFactory.createSubMenu();
      addComponent(menu);
      name.setToButton(menu);
      ComponentProperty.JUMP.putClientValue(menu.getPopupMenu(), myContext);
      ComponentBuilder builder = subMenuBuilder(menu, this, myFactory);
      for (PopupEntry entry : group.getEntries())
        entry.addToPopup(builder);
    }

    protected abstract void addComponent(JComponent item);

    public static ComponentBuilder popupBuilder(final JPopupMenu popup, JComponent conext) {
      return new ComponentBuilder(conext, POPUP_FACTORY) {
        protected void addComponent(JComponent item) {
          if (item instanceof JMenuItem)
            popup.add((JMenuItem) item);
          else
            popup.add(item);
        }
      };
    }

    public static ComponentBuilder subMenuBuilder(final JMenu menu, ComponentBuilder parent, ComponentFactory factory) {
      return new ComponentBuilder(parent.getContextComponent(), factory) {
        protected void addComponent(JComponent item) {
          if (item instanceof JMenuItem)
            menu.add((JMenuItem) item);
          else
            menu.add(item);
        }
      };
    }
  }

  interface ComponentFactory {
    AActionComponent<?> createActionComponent();

    AActionComponent<?> createCheckBoxComponent();

    AActionComponent<?> createDefaultComponent();

    AMenu createSubMenu();
  }

  ComponentFactory POPUP_FACTORY = new ComponentFactory() {
    public AMenuItem createActionComponent() {
      return new AMenuItem(true);
    }

    public ACheckBoxMenuItem createCheckBoxComponent() {
      return new ACheckBoxMenuItem();
    }

    public AMenuItem createDefaultComponent() {
      AMenuItem item = new AMenuItem(true);
      item.setFont(item.getFont().deriveFont(Font.BOLD));
      return item;
    }

    public AMenu createSubMenu() {
      AMenu menu = AMenu.hidding();
      menu.setIcon(PresentationMapping.EMPTY_ICON);
      return menu;
    }
  };

  ComponentFactory MAIN_MENU_FACTORY = new ComponentFactory() {
    public AActionComponent<?> createActionComponent() {
      AMenuItem item = new AMenuItem(false);
      item.overridePresentation(PresentationMapping.VISIBLE_NULL_ICON);
      return item;
    }

    public AActionComponent<?> createCheckBoxComponent() {
      ACheckBoxMenuItem item = new ACheckBoxMenuItem();
      item.overridePresentation(PresentationMapping.VISIBLE_NULL_ICON);
      return item;
    }

    public AActionComponent<?> createDefaultComponent() {
      return createActionComponent();
    }

    public AMenu createSubMenu() {
      AMenu menu = AMenu.disabling();
      menu.setIcon(EMPTY_ICON);
      return menu;
    }
  };
}
