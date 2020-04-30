package com.almworks.api.engine;

import com.almworks.api.application.qb.FilterNode;

public interface RemoteQuery2 {
  String getDisplayableName();

  String getId();

  FilterNode getFilterNode();
}
