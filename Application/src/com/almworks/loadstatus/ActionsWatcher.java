package com.almworks.loadstatus;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.gui.MainMenu;
import com.almworks.util.Env;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

class ActionsWatcher implements Startable {
  private static final String[] WATCHED_ACTIONS = Env.isMac()
    ? new String[] {
    MainMenu.Edit.EDIT_ITEM,
    MainMenu.File.SHOW_CONNECTION_INFO, MainMenu.File.REMOVE_CONNECTION, MainMenu.Edit.UPLOAD,
    MainMenu.Search.KEEP_LIVE_RESULTS, MainMenu.Search.NEW_DISTRIBUTION, MainMenu.Search.RUN_QUERY,
    MainMenu.Tools.SHOW_SYNCHRONIZATION_WINDOW, MainMenu.Tools.CONFIGURE_PROXY, }
    : new String[] {
    MainMenu.Edit.EDIT_ITEM, MainMenu.File.EXIT,
    MainMenu.File.SHOW_CONNECTION_INFO, MainMenu.File.REMOVE_CONNECTION, MainMenu.Edit.UPLOAD,
    MainMenu.Search.KEEP_LIVE_RESULTS, MainMenu.Search.NEW_DISTRIBUTION, MainMenu.Search.RUN_QUERY,
    MainMenu.Tools.SHOW_SYNCHRONIZATION_WINDOW, MainMenu.Tools.CONFIGURE_PROXY, MainMenu.Help.ABOUT, };

  private final ActionRegistry myActions;
  private final ApplicationLoadStatus.StartupActivity myStartup;

  public ActionsWatcher(ActionRegistry actions, ApplicationLoadStatus startup) {
    myActions = actions;
    myStartup = startup.createActivity("actions");
  }

  @Override
  public void start() {
    for (String id : WATCHED_ACTIONS) {
      final ApplicationLoadStatus.StartupActivity activity = myStartup.createSubActivity(id);
      myActions.addListener(activity.getLife(), id, new ActionRegistry.Listener() {
        @Override
        public void onActionRegister(@Nullable String actionId, @Nullable AnAction action) {
          activity.done();
        }
      });
    }
    myStartup.done();
  }

  @Override
  public void stop() {}
}
