package com.almworks.tools.tagexporter;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Note: this class is used instead of WorkArea to reduce module dependencies
 * */
public class WorkspaceStructure {
  private final File myWorkspaceDir;

  public WorkspaceStructure(@NotNull String workspaceDir) {
    this(new File(workspaceDir));
  }

  public WorkspaceStructure(@NotNull File workspaceDir) {
    myWorkspaceDir = workspaceDir;
  }

  public File getRootDir() {
    return myWorkspaceDir;
  }

  public File getDatabaseDir() {
    return new File(myWorkspaceDir, "db");
  }

  public File getConfigFile() {
    return new File(myWorkspaceDir, "config.xml");
  }

  public File getConfigBackupDir() {
    return new File(myWorkspaceDir, "backup");
  }
  
  public File getTempDir() {
    return new File(myWorkspaceDir, "temp");
  }
}
