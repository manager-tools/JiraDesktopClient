package com.almworks.timetrack.impl;

import com.almworks.api.application.LoadedItemServices;
import com.almworks.api.application.ModelMap;
import com.almworks.api.gui.MainMenu;
import com.almworks.engine.gui.ItemMessage;
import com.almworks.engine.gui.ItemMessageProvider;
import com.almworks.engine.gui.ItemMessages;
import com.almworks.engine.gui.ItemMessagesRegistry;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackerWindow;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.IconHandle;
import com.almworks.util.images.Icons;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.picocontainer.Startable;

public class TimeItemMessage implements ItemMessageProvider, Startable {
  public static final Role<TimeItemMessage> ROLE = Role.role(TimeItemMessage.class);
  private static final TypedKey<Object> TIME_KEY = TypedKey.create("timeTracker");
  private final ItemMessagesRegistry myRegistry;

  public TimeItemMessage(ItemMessagesRegistry registry) {
    myRegistry = registry;
  }

  public void attachMessages(Lifespan life, ModelMap model, ItemMessages itemMessages) {
    LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(model);
    if (lis == null) return;
    TimeTracker timeTracker = lis.getActor(TimeTracker.TIME_TRACKER);
    ArtifactUpdater updater = new ArtifactUpdater(model, itemMessages, timeTracker);
    timeTracker.getModifiable().addAWTChangeListener(life, updater);
    updater.updateMessage();
  }

  public void start() {
    myRegistry.addMessageProvider(this);
  }

  public void stop() {}

  private static class ArtifactUpdater implements ChangeListener {
    private static final String WORKING_SHORT = Local.parse("You are working on this " + Terms.ref_artifact);
    private static final String UNPUBLISHED_SHORT = Local.parse("Time tracking data is not published for this " + Terms.ref_artifact);
    private static final String UNPUBLISHED_LONG = Local.parse(
      "You have tracked some time or changed the remaining time estimate for this " + Terms.ref_artifact + 
      ", but you have not published this data yet.");
    private static final IconHandle MESSAGE_ICON = Icons.TIME_TRACKING_STARTED;

    private final ModelMap myModel;
    private final ItemMessages myMessages;
    private final TimeTracker myTimeTracker;

    public ArtifactUpdater(ModelMap model, ItemMessages messages, TimeTracker timeTracker) {
      myModel = model;
      myMessages = messages;
      myTimeTracker = timeTracker;
    }

    public void onChange() {
      updateMessage();
    }

    private void updateMessage() {
      LoadedItemServices lis = LoadedItemServices.VALUE_KEY.getValue(myModel);
      if (lis == null) return;
      long item = lis.getItem();
      TimeTrackerTask task = myTimeTracker.getCurrentTask();
      boolean timeTicking = task != null && task.getKey() == item;
      ItemMessage message;
      if (timeTicking) 
        message = ItemMessage.information(MESSAGE_ICON, WORKING_SHORT, null,
          new IdActionProxy(MainMenu.Tools.TIME_TRACKING_STOP_WORK_ON_ISSUE), OpenWindowAction.INSTANCE);
      else if (myTimeTracker.hasUnpublished(item)) {
        message = ItemMessage.information(MESSAGE_ICON, UNPUBLISHED_SHORT, UNPUBLISHED_LONG,
          ItemMessage.getActions(lis, MainMenu.Tools.TIME_TRACKING_PUBLISH));
      } else message = null;
      myMessages.setMessage(TIME_KEY, message);
    }
  }


  private static class OpenWindowAction extends SimpleAction {
    public static AnAction INSTANCE = new OpenWindowAction();

    private OpenWindowAction() {
      super("Open Time Tracker", Icons.ACTION_TIME_TRACKING);
      watchRole(TimeTrackerWindow.ROLE);
    }

    protected void customUpdate(UpdateContext context) throws CantPerformException {
      TimeTrackerWindow window = context.getSourceObject(TimeTrackerWindow.ROLE);
      context.updateOnChange(window.getShowingModifiable());
      context.setEnabled(window.isWindowShowing() ? EnableState.INVISIBLE : EnableState.ENABLED);
    }

    protected void doPerform(ActionContext context) throws CantPerformException {
      context.getSourceObject(TimeTrackerWindow.ROLE).show();
    }
  }
}
