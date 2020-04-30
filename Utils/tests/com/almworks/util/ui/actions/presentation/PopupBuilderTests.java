package com.almworks.util.ui.actions.presentation;

import com.almworks.util.Env;
import com.almworks.util.tests.CollectionsCompare;
import com.almworks.util.tests.GUITestCase;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author dyoma
 */
public class PopupBuilderTests extends GUITestCase {
  private static final TypedKey<Object> VALUE_KEY = TypedKey.create("value");
  private static final AnAction MOCK_ACTION = new SimpleAction("1") {
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(VALUE_KEY);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {}
  };
  private static final AnAction ENABLED_ACTION = new SimpleAction("action") {
    protected void customUpdate(UpdateContext context) throws CantPerformException {}
    protected void doPerform(ActionContext context) throws CantPerformException {}
  };

  private final CollectionsCompare CHECK = new CollectionsCompare();
  private final MenuBuilder myBuilder = new MenuBuilder();

  public void testUpdateMenus() {
    if (GraphicsEnvironment.isHeadless())
      return;
    myBuilder.addAction(MOCK_ACTION);
    myBuilder.addAction("id");
    MenuBuilder sub = myBuilder.createSubMenu("Sub");
    sub.addAction(MOCK_ACTION);
    myBuilder.addSeparator();
    myBuilder.addAction(ENABLED_ACTION);
    JPanel component = new JPanel();
    ActionRegistryImpl registry = new ActionRegistryImpl();
    ConstProvider.addRoleValue(component, ActionRegistry.ROLE, registry);
    CHECK.order(new String[] {"1 d", "Sub", "Sub/1 d", "-", "action"}, collectPopupNames(component));
    ConstProvider.addRoleValue(component, VALUE_KEY, "value");
    CHECK.order(new String[] {"1", "Sub", "Sub/1", "-", "action"}, collectPopupNames(component));
    registry.registerAction("id", MOCK_ACTION);
    CHECK.order(new String[] {"1", "1", "Sub", "Sub/1", "-", "action"}, collectPopupNames(component));
  }

  public void testSubMenus() {
    if (GraphicsEnvironment.isHeadless())
      return;
    myBuilder.createSubMenu("Sub1").createSubMenu("Sub2").addAction("id");
    JPanel component = new JPanel();
    ConstProvider.addRoleValue(component, ActionRegistry.ROLE, new ActionRegistryImpl());
    assertNull(myBuilder.createPopupWindow(component, null));

    myBuilder.addAction(MOCK_ACTION);
    CHECK.singleElement("1 d", collectPopupNames(component));
  }

  public void testSubMenuMnemonics() {
    if (GraphicsEnvironment.isHeadless())
      return;
    myBuilder.createSubMenu("S&ub").addAction(MOCK_ACTION);
    JPopupMenu popup = myBuilder.createPopupWindow(new JPanel(), null);
    assertNotNull(popup);
    JMenu menu = (JMenu) popup.getComponent(0);
    if (!Env.isMac())
      assertEquals(1, menu.getDisplayedMnemonicIndex());
  }

  public void testSeparators() {
    if (GraphicsEnvironment.isHeadless())
      return;
    myBuilder.addSeparator();
    myBuilder.createSubMenu("Sub").addAction("id");
    myBuilder.addSeparator();
    myBuilder.addAction(MOCK_ACTION);
    myBuilder.addSeparator();
    myBuilder.addAction("id");
    myBuilder.addSeparator();
    myBuilder.addAction(MOCK_ACTION);
    myBuilder.addSeparator();
    JPanel component = new JPanel();
    CHECK.order(new String[] {"1 d", "-", "1 d"}, collectPopupNames(component));
    ActionRegistryImpl registry = new ActionRegistryImpl();
    ConstProvider.addRoleValue(component, ActionRegistry.ROLE, registry);
    registry.registerAction("id", MOCK_ACTION);
    CHECK.order(new String[] {"Sub", "Sub/1 d", "-", "1 d", "-", "1 d", "-", "1 d"}, collectPopupNames(component));
  }

  private List<String> collectPopupNames(JComponent context) {
    JPopupMenu popup = myBuilder.createPopupWindow(context, null);
    if (popup == null)
      return Collections15.emptyList();
    List<String> result = Collections15.arrayList();
    collectNamesImpl(popup.getComponents(), result, "");
    return result;
  }

  private List<String> collectNames(JComponent menu) {
    List<String> result = Collections15.arrayList();
    collectNamesImpl(menu.getComponents(), result, "");
    return result;
  }

  private void collectNamesImpl(Component[] components, List<String> result, String prefix) {
    for (int i = 0; i < components.length; i++) {
      Component component = components[i];
      if (!component.isVisible())
        continue;
      if (component instanceof JMenu) {
        JMenu menu = (JMenu) component;
        String subMenu = menu.getText();
        result.add(subMenu);
        collectNamesImpl(menu.getMenuComponents(), result, prefix + subMenu + "/");
      } else if (component instanceof JMenuItem) {
        boolean enabled = component.isEnabled();
        result.add(prefix + ((JMenuItem) component).getText() + (enabled ? "" : " d"));
      } else if (component instanceof JLabel) {
        result.add(prefix + ((JLabel) component).getText());
      } else if (component instanceof JPopupMenu.Separator) {
        result.add("-");
      } else
        result.add("Unknown: " + component.getClass());
    }
  }
}
