package com.almworks.tools.tagexporter;

import com.almworks.util.progress.Progress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public abstract class TagExporterEnv {
  // error keys
  public static String WORKSPACE_KEY = "workspace";
  public static String OUT_FILE_KEY = "outFile";
  public static String GLOBAL_KEY = "global";

  public abstract String getFullName();

  /** @return null if workspace directory contains database file */
  @Nullable
  public final String verifyWorkspace(File workspace) {
    if (workspace == null) {
      return "Please specify workspace folder";
    } else if (!workspace.isDirectory()) {
      return "Specified workspace is not a folder";
    } else if (!workspace.canRead() || !workspace.canWrite()) {
      return "Insufficient permissions for the specified folder";
    } else {
      WorkspaceStructure workArea = new WorkspaceStructure(workspace);
      if (!isWorkspace(workArea)) {
        return "Cannot find database file in the workspace folder";
      }
    }
    return null;
  }
  
  protected abstract boolean isWorkspace(WorkspaceStructure structure);

  @NotNull
  public abstract List<TagInfo> readTags(WorkspaceStructure workArea, Progress progress) throws Exception;
}
