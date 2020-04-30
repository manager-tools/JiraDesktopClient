package com.almworks.image;

import com.almworks.api.container.RootContainer;
import com.almworks.api.image.RemoteIconManager;
import com.almworks.api.image.Thumbnailer;
import com.almworks.api.platform.ComponentDescriptor;

public class ImageComponentsDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(Thumbnailer.ROLE, ThumbnailerImpl.class);
    container.registerActorClass(RemoteIconManager.ROLE, RemoteIconManagerImpl.class);
  }
}
