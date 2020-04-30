package com.almworks.passman;

import com.almworks.api.container.RootContainer;
import com.almworks.api.passman.PasswordManager;
import com.almworks.api.platform.ComponentDescriptor;

/**
 * :todoc:
 *
 * @author sereda
 */
public class PasswordManagerComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(PasswordManager.ROLE, PasswordManagerImpl.class);
  }
}
