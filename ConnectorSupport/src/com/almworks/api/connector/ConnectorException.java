package com.almworks.api.connector;

public class ConnectorException extends Exception {
  private final String myShortDescription;
  private final String myLongDescription;
  private boolean myFailedOperationRerunnable;

  public ConnectorException(String message, String shortDescription, String longDescription) {
    this(message, null, shortDescription, longDescription);
  }

  public ConnectorException(String message, Throwable cause, String shortDescription, String longDescription) {
    super(message, cause);
    assert shortDescription != null;
    assert longDescription != null;
    myShortDescription = shortDescription;
    myLongDescription = longDescription;
  }

  public String getLongDescription() {
    return myLongDescription;
  }

  public String getShortDescription() {
    return myShortDescription;
  }

  public String getMediumDescription() {
    return getShortDescription();
  }

  public boolean isFailedOperationRerunnable() {
    return myFailedOperationRerunnable;
  }

  public void setFailedOperationRerunnable(boolean failedOperationRerunnable) {
    myFailedOperationRerunnable = failedOperationRerunnable;
  }
}
