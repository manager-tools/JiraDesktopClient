package com.almworks.util.ui.actions;

import com.almworks.util.properties.Role;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author : Dyoma
 */
public interface ActionRegistry {
  Role<ActionRegistry> ROLE = Role.role("actionRegistry", ActionRegistry.class);

  void registerAction(@NotNull String actionKey, @NotNull AnAction action);

  @Nullable
  AnAction getAction(String actionKey);

  void addListener(Lifespan life, @NotNull String actionId, @NotNull Listener listener);

  void removeListener(@Nullable String actionId, @Nullable Listener listener);

  boolean isActionRegistered(@Nullable String actionId);

  void registerKeyStroke(@NotNull String actionKey, @NotNull ScopedKeyStroke stroke);

  @NotNull
  List<ScopedKeyStroke> getScopedKeystrokes(@Nullable KeyStroke keyStroke);

  @Nullable
  AnAction getAction(@Nullable ScopedKeyStroke keyStroke);

  @Nullable
  ScopedKeyStroke getKeyStroke(@NotNull String id);

  void addActionToGroup(@NotNull String actionKey, @NotNull String group);

  @NotNull
  List<String> getActionKeysForGroup(@NotNull String group);

  interface Listener {
    void onActionRegister(@Nullable String actionId, @Nullable AnAction action);
  }
}
