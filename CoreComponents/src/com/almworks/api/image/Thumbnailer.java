package com.almworks.api.image;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface Thumbnailer {
  Role<Thumbnailer> ROLE = Role.role("thumbnails");

  @ThreadAWT
  @Nullable
  Image getThumbnail(String imageId, Dimension maxSize, ThumbnailSourceFactory sourceImageFactory,
    ThreadGate factoryGate, ThumbnailReadyNotificator notificator);

  @ThreadAWT
  @Nullable
  Image getThumbnail(String imageId, Dimension maxSize);
}
