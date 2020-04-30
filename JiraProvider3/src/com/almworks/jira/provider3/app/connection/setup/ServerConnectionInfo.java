package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.restconnector.JiraCredentials;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class ServerConnectionInfo {
  private final JiraCredentials myCredentials;
  private final String myBaseUrl;
  private final String myTitle;
  private final String myUserDisplayName;
  private final List<ConnectionTestController.Project> myProjects;
  private final boolean myConnectionAllowed;

  ServerConnectionInfo(String baseUrl, String title, JiraCredentials credentials, String userDisplayName, List<ConnectionTestController.Project> projects, boolean connectionAllowed) {
    myBaseUrl = baseUrl;
    myTitle = title;
    myCredentials = credentials;
    myUserDisplayName = userDisplayName;
    myProjects = projects;
    myConnectionAllowed = connectionAllowed;
  }

  @Nullable("When anonymous or failed to load")
  public String getUserDisplayName() {
    return myUserDisplayName;
  }

  public JiraCredentials getCredentials() {
    return myCredentials;
  }

  public String getBaseUrl() {
    return myBaseUrl;
  }

  public String getTitle() {
    return myTitle;
  }

  public List<ConnectionTestController.Project> getProjects() {
    return myProjects;
  }

  public boolean isConnectionAllowed() {
    return myConnectionAllowed;
  }
}
