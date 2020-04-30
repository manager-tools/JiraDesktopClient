package com.almworks.actions.console.actionsource;

import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleActionsComponent {
  public static final Role<ConsoleActionsComponent> ROLE = Role.role("consoleActions", ConsoleActionsComponent.class);
  public static final AnAction OPEN_CONSOLE = new OperationConsoleAction();

  private final List<ActionGroup> myGroups = Collections15.arrayList();

  public ConsoleActionsComponent() {
  }
  
  public void addGroup(ActionGroup group) {
    synchronized (myGroups) {
      myGroups.add(group);
    }
  }

  public List<UpdatedAction.Group> getEnabledGroups(Component contextComponent) {
    ActionGroup[] groups;
    synchronized (myGroups) {
      groups = myGroups.toArray(new ActionGroup[myGroups.size()]);
    }
    ArrayList<UpdatedAction.Group> result = Collections15.arrayList();
    for (ActionGroup group : groups) {
      DefaultUpdateContext context = new DefaultUpdateContext(contextComponent, Updatable.NEVER);
      try {
        group.update(context);
      } catch (CantPerformException e) {
        continue;
      }
      Map<PresentationKey<?>, Object> values = context.getAllValues();
      if (EnableState.ENABLED != PresentationKey.ENABLE.getFrom(values)) continue;
      List<AnAction> actions = ActionGroup.ACTIONS.getFrom(values);
      if (actions == null || actions.isEmpty()) continue;
      List<UpdatedAction.Action> enabled = selectEnabled(contextComponent, actions);
      if (!enabled.isEmpty()) result.add(new UpdatedAction.Group(group, values, enabled));
    }
    return result;
  }

  private List<UpdatedAction.Action> selectEnabled(Component contextComponent, List<AnAction> actions) {
    ArrayList<UpdatedAction.Action> result = Collections15.arrayList();
    for (AnAction action : actions) {
      DefaultUpdateContext context = new DefaultUpdateContext(contextComponent, Updatable.NEVER);
      try {
        action.update(context);
      } catch (CantPerformException e) {
        continue;
      }
      Map<PresentationKey<?>, Object> values = context.getAllValues();
      if (EnableState.ENABLED != PresentationKey.ENABLE.getFrom(values)) continue;
      result.add(new UpdatedAction.Action(action, values));
    }
    return result;
  }
}
