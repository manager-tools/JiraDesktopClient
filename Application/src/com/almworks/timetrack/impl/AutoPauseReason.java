package com.almworks.timetrack.impl;

public enum AutoPauseReason {
  /**
   * JIRA Client was not started
   */
  APPLICATION_INACTIVE("application not being run"),

  /**
   * Computer seems to have been in sleep or hibernation
   */
  COMPUTER_INACTIVE("computer inactivity"),

  /**
   * No user activity detected
   */
  USER_INACTIVE("inactivity");

  private final String myReason;

  AutoPauseReason(String reason) {
    myReason = reason;
  }

  public String getReason() {
    return myReason;
  }
}
