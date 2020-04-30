package com.almworks.api.application.viewer;

import java.awt.*;

public interface CommentRenderingHelper<T extends Comment> {
  Color getForeground(T comment);

  String getHeaderPrefix(T comment);

  String getHeaderSuffix(T comment);
}
