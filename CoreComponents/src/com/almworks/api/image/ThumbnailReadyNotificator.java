package com.almworks.api.image;

import com.almworks.util.threads.ThreadAWT;

import java.awt.*;

public interface ThumbnailReadyNotificator {
  @ThreadAWT
  void onThumbnailReady(String imageId, Image thumbnail);
}
