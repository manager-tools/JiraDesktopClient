package com.almworks.jira.provider3.gui.timetrack;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.items.api.Database;
import com.almworks.timetrack.api.TimeTrackingCustomizer;

public class JiraTimeTrackingDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActor(TimeTrackingCustomizer.ROLE, new JiraTimeTrackingCustomizer());
  }

  public static void registerTrigger(Database db) {
    db.registerTrigger(TimeTrigger.INSTANCE);
  }
}
