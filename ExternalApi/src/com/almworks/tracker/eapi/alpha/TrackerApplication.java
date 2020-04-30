package com.almworks.tracker.eapi.alpha;

public enum TrackerApplication {
  DESKZILLA("deskzilla"),
  JIRA_CLIENT("jiraclient");

  private final String myApplicationId;

  private TrackerApplication(String applicationId) {
    myApplicationId = applicationId;
  }

  public String getApplicationId() {
    return myApplicationId;
  }
}
