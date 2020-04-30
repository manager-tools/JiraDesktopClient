package com.almworks.misc;

import com.almworks.api.misc.WorkArea;
import com.almworks.platform.WorkAreaImpl;
import org.almworks.util.Failure;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class TestWorkArea extends WorkAreaImpl implements WorkArea {
  private File myHomeDir = null;

  public synchronized File getHomeDir() {
    try {
      if (myHomeDir == null) {
        File tempFile = File.createTempFile("workarea", ".tmp");
        if (!tempFile.delete())
          throw new Failure("could not delete temp file " + tempFile);
        if (!tempFile.mkdir())
          throw new Failure("could not create temp dir " + tempFile);
        tempFile.deleteOnExit();
        myHomeDir = tempFile;
      }
      return myHomeDir;
    } catch (IOException e) {
      throw new Failure(e);
    }
  }

  @Nullable
  public File getLockFile() {
    return null;
  }

  protected File getWorkspaceDir() {
    return getHomeDir();
  }

  protected boolean isOldFashioned() {
    return true;
  }

  public void cleanUp() {
    if (myHomeDir == null)
      return;
    myHomeDir.delete();
    myHomeDir.deleteOnExit();
    // todo - recursive delete?
  }
}
