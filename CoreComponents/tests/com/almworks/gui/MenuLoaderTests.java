package com.almworks.gui;

import com.almworks.util.Env;
import com.almworks.util.components.AMenu;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.MapMedium;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.tests.AwtTestsGate;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.ui.AMenuItem;
import com.almworks.util.ui.EmptyIcon;
import com.almworks.util.ui.actions.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * @author : Dyoma
 */
public class MenuLoaderTests extends BaseTestCase {
  private final ActionRegistryImpl myActionRegistry = new ActionRegistryImpl();
  private final MenuLoader myMenuLoader = new MenuLoader();
  private final MockAction myMockAction = new MockAction();

  protected void setUp() throws Exception {
    super.setUp();
    myActionRegistry.registerAction("2.1", myMockAction);
    flushAWTQueue();
  }

  public void testTopMenuEnable() throws InvocationTargetException, InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;
    Configuration configuration = MapMedium.createConfig();
    Configuration first = configuration.createSubset(MenuLoader.MENU_ITEM);
    first.setSetting(MenuLoader.MENU_ID, "1");
    first.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "2.1");

    JMenuBar bar = myMenuLoader.loadMenuBar(configuration, null);
    addNotifyMenuBar(bar);
    flushAWTQueue();
    flushAWTQueue();
    assertTrue(bar.getMenu(0).getMenuComponent(0).isVisible());
    assertTrue(bar.getMenu(0).isEnabled());
  }

  public void testRecursiveUpdate() throws InvocationTargetException, InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;
    Configuration config = MapMedium.createConfig();
    Configuration top = config.createSubset(MenuLoader.MENU_ITEM);
    top.setSetting(MenuLoader.MENU_ID, "top");
    Configuration menu = top.createSubset(MenuLoader.MENU_ITEM);
    menu.setSetting(MenuLoader.MENU_ID, "1");
    menu.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "2.1");

    JMenuBar bar = myMenuLoader.loadMenuBar(config, null);
    addNotifyMenuBar(bar);

    JMenu topMenu = bar.getMenu(0);
    JMenu subMenu = (JMenu) topMenu.getMenuComponent(0);
    Component item = subMenu.getMenuComponent(0);
    assertTrue(topMenu.isVisible());
    assertTrue(item.isVisible());
    assertTrue(subMenu.isVisible());
    assertTrue(topMenu.isEnabled());
  }

  public void testRegisteringActions() throws ReadonlyConfiguration.NoSettingException, InvocationTargetException,
    InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;
    Configuration configuration = MapMedium.createConfig();
    Configuration first = configuration.createSubset(MenuLoader.MENU_ITEM);
    first.setSetting(MenuLoader.MENU_ID, "1");
    Configuration first1 = first.createSubset(MenuLoader.MENU_ITEM);
    first1.setSetting(MenuLoader.MENU_ID, "1.1");
    first1.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "1.1.1");
    Configuration second = configuration.createSubset(MenuLoader.MENU_ITEM);
    second.setSetting(MenuLoader.MENU_ID, "2");
    second.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "2.1");

    Properties i18n = new Properties();
    i18n.put("1", "Menu1");

    JMenuBar menuBar = myMenuLoader.loadMenuBar(configuration, i18n);
    addNotifyMenuBar(menuBar);
    assertEquals(2, menuBar.getMenuCount());
    assertEquals("Menu1", menuBar.getMenu(0).getText());
    assertTrue(menuBar.getMenu(0).isVisible());
    assertTrue(menuBar.getMenu(0).isEnabled());
    assertTrue(menuBar.getMenu(0).getMenuComponentCount() > 0);
    assertEquals("2", menuBar.getMenu(1).getText());
    assertTrue(menuBar.getMenu(1).isVisible());

    myActionRegistry.registerAction("1.1.1", myMockAction);
    flushAWTQueue();
    ActionBridge.updateActionsNow();
    flushAWTQueue();
    flushAWTQueue();
    flushAWTQueue();
    assertTrue(menuBar.getMenu(0).isVisible());
    assertTrue(menuBar.getMenu(0).getMenuComponent(0).isVisible());
    menuBar.removeNotify();
  }

  private void addNotifyMenuBar(JMenuBar menuBar) throws InterruptedException, InvocationTargetException {
    final JFrame frame = new JFrame();
    frame.add(menuBar);
    AwtTestsGate.AWT_FOR_TEST.execute(new Runnable() {
      public void run() {
        ConstProvider.addRoleValue(frame.getRootPane(), ActionRegistry.ROLE, myActionRegistry);
      }
    });
    menuBar.addNotify();
    flushAWTQueue();
  }

  private void flushAWTQueue() throws InterruptedException, InvocationTargetException {
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
      }
    });
  }

  public void testMenuMnemonic() throws ReadonlyConfiguration.NoSettingException, InvocationTargetException,
    InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;

    Configuration config = MapMedium.createConfig();
    Configuration menuCfg = config.createSubset(MenuLoader.MENU_ITEM);
    menuCfg.setSetting(MenuLoader.MENU_ID, "menu");
    menuCfg.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "action");
    menuCfg.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "action2");
    MockAction action = new MockAction();
    action.setName("&my action");
    myActionRegistry.registerAction("action", action);
    myActionRegistry.registerAction("action2", new MockAction());
    flushAWTQueue();
    Configuration menu2Cfg = config.createSubset(MenuLoader.MENU_ITEM);
    menu2Cfg.setSetting(MenuLoader.MENU_ID, "no mnemonic");
    menu2Cfg.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "some action");

    Properties i18n = new Properties();
    i18n.put("menu", "a&a");

    JMenuBar menuBar = myMenuLoader.loadMenuBar(config, i18n);
    addNotifyMenuBar(menuBar);
    AMenu menu = (AMenu) menuBar.getMenu(0);
//    menu.updateItems();
    assertEquals("aa", menu.getText());
    if (!Env.isMac()) {
      assertEquals(1, menu.getDisplayedMnemonicIndex());
      assertEquals(KeyEvent.VK_A, menu.getMnemonic());
      JMenuItem item = (JMenuItem) menu.getMenuComponent(0);
      assertEquals("my action", item.getText());
      assertEquals(0, item.getDisplayedMnemonicIndex());
      assertEquals(KeyEvent.VK_M, item.getMnemonic());
      item = (JMenuItem) menu.getMenuComponent(1);
      JMenuItem noMnemonic = new JMenuItem();
      assertEquals(noMnemonic.getDisplayedMnemonicIndex(), item.getDisplayedMnemonicIndex());
      assertEquals(-1, item.getMnemonic());
      JMenu noMnemonicMenu = new JMenu();
      menu = (AMenu) menuBar.getMenu(1);
      assertEquals(noMnemonicMenu.getDisplayedMnemonicIndex(), menu.getDisplayedMnemonicIndex());
    }
  }

  public void testItemsPresentation() throws InvocationTargetException, InterruptedException {
    if (GraphicsEnvironment.isHeadless())
      return;
    Configuration config = MapMedium.createConfig();
    Configuration menuCfg = config.createSubset(MenuLoader.MENU_ITEM);
    menuCfg.setSetting(MenuLoader.MENU_ID, "menu");
    menuCfg.createSubset(MenuLoader.ACTION_ITEM).setSetting(MenuLoader.MENU_ID, "iconAction");
    AnAbstractAction action = new AnAbstractAction("Action") {
      public void perform(ActionContext context) {
      }
    };
    myActionRegistry.registerAction("iconAction", action);

    JMenuBar bar = myMenuLoader.loadMenuBar(config, null);
    addNotifyMenuBar(bar);
    AMenuItem item = (AMenuItem) bar.getMenu(0).getMenuComponent(0);
    assertTrue(item.isVisible());
    assertTrue(item.isEnabled());
    EmptyIcon customIcon = new EmptyIcon(5, 5);
    action.setDefaultPresentation(PresentationKey.SMALL_ICON, customIcon);
    updateNow(item);
    assertNull(item.getIcon());

    action.setDefaultPresentation(PresentationKey.ENABLE, EnableState.INVISIBLE);
    updateNow(item);
    assertTrue(item.isVisible());
    assertFalse(item.isEnabled());
  }

  private void updateNow(final AMenuItem item) throws InvocationTargetException, InterruptedException {
    AwtTestsGate.AWT_FOR_TEST.execute(new Runnable() {
      public void run() {
        item.updateNow();
      }
    });
    flushAWTQueue();
  }

  private static class MockAction extends AnAbstractAction {
    public MockAction() {
      super("Action");
    }

    public void setName(String text) {
      setDefaultPresentation(PresentationKey.NAME, text);
    }

    public void perform(ActionContext context) {
      // yo
    }

    public void update(UpdateContext context) throws CantPerformException {
      super.update(context);
    }
  }
}
