package com.almworks.screenshot;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.screenshot.ScreenShooter;

public class ScreenShooterDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(ScreenShooter.ROLE, ScreenShooterImpl.class);
  }
}
