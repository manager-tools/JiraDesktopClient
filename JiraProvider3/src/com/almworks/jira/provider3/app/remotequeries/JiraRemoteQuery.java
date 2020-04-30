package com.almworks.jira.provider3.app.remotequeries;

import com.almworks.api.application.qb.FilterNode;
import com.almworks.api.engine.RemoteQuery2;

class JiraRemoteQuery implements RemoteQuery2 {
  private final String myId;
  private final String myName;
  private final FilterNode myFilter;

  public JiraRemoteQuery(String id, String name, FilterNode filter) {
    myId = id;
    myName = name;
    myFilter = filter;
  }


  public String getDisplayableName() {
    return myName;
  }

  public String getId() {
    return myId;
  }

  public FilterNode getFilterNode() {
    return myFilter;
  }
}
