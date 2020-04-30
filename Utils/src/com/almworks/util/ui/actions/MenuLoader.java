package com.almworks.util.ui.actions;

import com.almworks.util.components.AMenu;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.presentation.ActionGroupVisitor;
import com.almworks.util.ui.actions.presentation.MenuBuilder;
import org.almworks.util.Failure;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.util.Properties;

/**
 * @author : Dyoma
 */
public class MenuLoader {
  public static final String MENU_ID = "id";
  public static final String ACTION_ITEM = "action";
  public static final String TOGGLE_ACTION_ITEM = "toggleaction";
  public static final String MENU_ITEM = "menu";

  private static final Properties EMPTY_I18N = new Properties();
//  private static final PresentationMapping<Icon> CLEAR_ICON = PresentationMapping.constant(null);
//  private static final PresentationMapping<Boolean> ALWAYS_VISIBLE = PresentationMapping.constant(true);

  public JMenuBar loadMenuBar(ReadonlyConfiguration configuration, Properties i18n) {
    if (i18n == null)
      i18n = EMPTY_I18N;
    MenuBuilder builder = new MenuBuilder();
    try {
      for (ReadonlyConfiguration menu : configuration.getAllSubsets(MENU_ITEM)) {
        String id = menu.getMandatorySetting(MENU_ID);
        String name = i18n.getProperty(id, id);
        loadMenuBar2(menu, i18n, "", builder.createSubMenu(name));
      }
//      loadMenuBar2(configuration, i18n, "", builder);
    } catch (ReadonlyConfiguration.NoSettingException e) {
      throw new Failure("cannot load menu", e);
    }
    final JMenuBar menuBar = new JMenuBar() {
      public void addNotify() {
        super.addNotify();
        for (int i = 0; i < getMenuCount(); i++)
          ((AMenu) getMenu(i)).parentStartsUpdate();
      }

      public void removeNotify() {
        for (int i = 0; i < getMenuCount(); i++)
          ((AMenu) getMenu(i)).parentStopsUpdate();
        super.removeNotify();
      }
    };
    builder.buildMenu(new ActionGroupVisitor.ComponentBuilder(menuBar, ActionGroupVisitor.MAIN_MENU_FACTORY) {
      protected void addComponent(JComponent item) {
        menuBar.add(item);
      }
    });
    menuBar.setBorder(
      new CompoundBorder(new EmptyBorder(0, 0, 2, 0), UIUtil.createSouthBevel(menuBar.getBackground())));
    return menuBar;
  }

  private void loadMenuBar2(ReadonlyConfiguration configuration, Properties i18n, String prefix, MenuBuilder builder)
    throws ReadonlyConfiguration.NoSettingException {
    for (ReadonlyConfiguration menu : configuration.getAllSubsets(null)) {
      String itemType = menu.getName();
      if (MENU_ITEM.equals(itemType)) {
      String id = menu.getMandatorySetting(MENU_ID);
        MenuBuilder subMenu = builder.createSubMenu(i18n.getProperty(prefix + id, id));
        loadMenuBar2(menu, i18n, prefix + id + ".", subMenu);
      } else if ("separator".equals(menu.getName()))
        builder.addSeparator();
      else if (ACTION_ITEM.equals(menu.getName())) {
        builder.addAction(menu.getMandatorySetting(MENU_ID));
      } else if (TOGGLE_ACTION_ITEM.equals(itemType)) {
        final IdActionProxy action = new IdActionProxy(menu.getMandatorySetting(MENU_ID));
        builder.addToggleAction(action);
      }
    }
  }
}
