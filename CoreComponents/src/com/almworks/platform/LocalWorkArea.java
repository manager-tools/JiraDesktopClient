package com.almworks.platform;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LocalWorkArea extends WorkAreaImpl {
  private final File myDir;

  public LocalWorkArea(@NotNull String workspaceDir) {
    if (workspaceDir == null) throw new NullPointerException();
    myDir = new File(workspaceDir);
  }

  public LocalWorkArea(@NotNull File workspaceDir) {
    if (workspaceDir == null) throw new NullPointerException();
    myDir = workspaceDir;
  }

  public File getHomeDir() {
    return myDir;
  }

  protected File getWorkspaceDir() {
    return myDir;
  }

  protected boolean isOldFashioned() {
    return false;
  }
}
