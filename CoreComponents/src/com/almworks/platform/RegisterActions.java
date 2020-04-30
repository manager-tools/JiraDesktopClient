package com.almworks.platform;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.Collections15;
import org.picocontainer.Startable;

import java.util.List;

public class RegisterActions implements Startable {
  private static final Role<RegisterActions> ROLE = Role.role("actionRegister");
  private static final Role<List<Pair<String, AnAction>>> ACTIONS = Role.role("actionsToRegister");

  private final ActionRegistry myRegistry;
  private final List<Pair<String,AnAction>> myPairs;

  RegisterActions(ActionRegistry registry, ComponentContainer container) {
    myRegistry = registry;
    myPairs = container.getActor(ACTIONS);
  }

  public static void registerAction(MutableComponentContainer container, final String actionId, final AnAction action) {
    List<Pair<String, AnAction>> pairs;
    if (container.isRegistered(ACTIONS)) {
      pairs = container.getActor(ACTIONS);
      if (pairs == null) {
        LogHelper.error();
        return;
      }
    } else {
      pairs = Collections15.arrayList();
      container.registerActor(ACTIONS, pairs);
      container.registerActorClass(RegisterActions.ROLE, RegisterActions.class);
    }
    pairs.add(Pair.create(actionId, action));
  }

  @Override
  public void start() {
    for (Pair<String, AnAction> pair : myPairs) {
      myRegistry.registerAction(pair.getFirst(), pair.getSecond());
    }
  }

  @Override
  public void stop() {
  }
}
