package com.almworks.util.components.tabs;

import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author dyoma
 */
public class TabActions {
  public static final AnAction CLOSE_TAB = new CloseTabAction();
  public static final AnAction CLOSE_ALL = new CloseAllAction();
  public static final AnAction CLOSE_ALL_BUT_THIS = new CloseAllButThisAction();

  public static final String EXPAND_SHRINK_TABS_ID = "TabActions.ExpandShrinkTabs";
  public static final AnAction EXPAND_SHRINK_TABS = new IdActionProxy(EXPAND_SHRINK_TABS_ID);

  public static AnAction createCloseTabAction(@NotNull TypedKey<TabsManager> managerKey,
    @NotNull String name, @Nullable String actionId)
  {
    return new AnotherCloseTabAction(managerKey, name, actionId);
  }

  public static AnAction createForwardTabAction(@NotNull TypedKey<TabsManager> managerKey,
    @NotNull String name, @Nullable String actionId) {
    return new NextTabAction(name, managerKey, actionId, true);
  }

  public static AnAction createBackwardTabAction(@NotNull TypedKey<TabsManager> managerKey,
    @NotNull String name, @Nullable String actionId) {
    return new NextTabAction(name, managerKey, actionId, false);
  }

  private static class CloseTabAction extends SimpleAction {
    public CloseTabAction() {
      super("&Close");
      watchRole(ContentTab.DATA_ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ContentTab.DATA_ROLE);
      IdActionProxy.setShortcut(context, "Window.CloseTab");
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(ContentTab.DATA_ROLE).delete();
    }
  }


  private static class AnotherCloseTabAction extends SimpleAction {
    private final TypedKey<TabsManager> myManagerKey;
    private final String myActionId;

    public AnotherCloseTabAction(TypedKey<TabsManager> managerKey, String name, String actionId) {
      super(name);
      myManagerKey = managerKey;
      myActionId = actionId;
      watchRole(managerKey);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      IdActionProxy.setShortcut(context, myActionId);
      TabsManager manager = context.getSourceObject(myManagerKey);
      context.updateOnChange(manager.getModifiable());
      ContentTab tab = manager.getSelectedTab();
      context.setEnabled(tab != null ? EnableState.ENABLED : EnableState.DISABLED);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      ContentTab tab = context.getSourceObject(myManagerKey).getSelectedTab();
      if (tab != null) {
        tab.delete();
      }
    }
  }

  private static class CloseAllAction extends SimpleAction {
    public CloseAllAction() {
      super("Close All Tabs");
      watchRole(ContentTab.DATA_ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ContentTab.DATA_ROLE);
      int tabsCount = context.getSourceObject(TabsManager.ROLE).getTabsCount();
      context.setEnabled(tabsCount > 1);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      TabsManager manager = context.getSourceObject(TabsManager.ROLE);
      List<ContentTab> tabsList = manager.getTabs();
      ContentTab[] allTabs = tabsList.toArray(new ContentTab[tabsList.size()]);
      for (ContentTab tab : allTabs) {
        tab.delete();
      }
    }
  }


  private static class CloseAllButThisAction extends SimpleAction {
    public CloseAllButThisAction() {
      super("Close All &Other Tabs");
      watchRole(ContentTab.DATA_ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.getSourceObject(ContentTab.DATA_ROLE);
      int tabsCount = context.getSourceObject(TabsManager.ROLE).getTabsCount();
      context.setEnabled(tabsCount > 1);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      TabsManager manager = context.getSourceObject(TabsManager.ROLE);
      ContentTab selectedTab = context.getSourceObject(ContentTab.DATA_ROLE);
      List<ContentTab> tabsList = manager.getTabs();
      ContentTab[] allTabs = tabsList.toArray(new ContentTab[tabsList.size()]);
      for (ContentTab tab : allTabs) {
        if (selectedTab != tab)
          tab.delete();
      }
    }
  }

  private static class NextTabAction extends SimpleAction {
    private final TypedKey<TabsManager> myManagerKey;
    private final String myActionId;
    private final boolean myIncrement;

    public NextTabAction(@NotNull String name, TypedKey<TabsManager> managerKey, String actionId, boolean increment) {
      super(name);
      myManagerKey = managerKey;
      myActionId = actionId;
      myIncrement = increment;
      watchRole(managerKey);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      IdActionProxy.setShortcut(context, myActionId);
      TabsManager manager = context.getSourceObject(myManagerKey);
      context.updateOnChange(manager.getModifiable());
      if (manager.getTabsCount() < 2)
        context.setEnabled(EnableState.DISABLED);
      else
        context.setEnabled(manager.getSelectedTabIndex() != -1 ? EnableState.ENABLED : EnableState.DISABLED);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      TabsManager manager = context.getSourceObject(myManagerKey);
      int selectedIndex = manager.getSelectedTabIndex();
      int tabsCount = manager.getTabsCount();
      int next = selectedIndex + (myIncrement ? 1 : -1) + tabsCount;
      manager.selectTabIndex(next % tabsCount);
    }
  }
}
