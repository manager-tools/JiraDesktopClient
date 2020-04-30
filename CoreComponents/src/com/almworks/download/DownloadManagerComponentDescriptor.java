package com.almworks.download;

import com.almworks.api.container.RootContainer;
import com.almworks.api.download.DownloadManager;
import com.almworks.api.platform.ComponentDescriptor;

public class DownloadManagerComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(DownloadManager.ROLE, DownloadManagerImpl.class);
  }
}
