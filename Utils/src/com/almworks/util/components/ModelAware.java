package com.almworks.util.components;

import com.almworks.util.threads.ThreadAWT;

public interface ModelAware {
  @ThreadAWT
  void onInsertToModel();

  @ThreadAWT
  void onRemoveFromModel();

  @ThreadAWT
  void onChildrenChanged();
}
