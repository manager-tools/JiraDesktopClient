package com.almworks.inquiry;

import com.almworks.api.container.RootContainer;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public class InquiriesComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(Role.role("inquiries"), InquiriesImpl.class);
    container.registerActor(Role.role("dummyDisplayer"), Dummies.DUMMY_DISPLAYER);
    container.registerActor(Role.role("dummyHandler"), Dummies.DUMMY_HANDLER);
  }
}
