package com.almworks.api.image;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

public interface ThumbnailSourceFactory {
  @Nullable
  Image createSourceImage(String imageId, Dimension maxSize);
}
