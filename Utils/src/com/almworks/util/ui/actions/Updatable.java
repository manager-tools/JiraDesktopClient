package com.almworks.util.ui.actions;

import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

/**
 * @author dyoma
 */
public interface Updatable extends UpdateService {
  @NotNull
  Lifespan getLifespan();

  Updatable NEVER = new Updatable() {
    public void requestUpdate() {
    }

    @NotNull
    public Lifespan getLifespan() {
      return Lifespan.NEVER;
    }
  };
}
