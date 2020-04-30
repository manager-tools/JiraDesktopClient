package com.almworks.api.engine.util;

import com.almworks.api.connector.ConnectorException;

/**
 * An excpetion thrown to indicate that synchronization
 * has been disabled for a connection.
 */
public class SyncNotAllowedException extends ConnectorException {
  private final String myMediumDescription;
  
  private SyncNotAllowedException(String mediumDescription, String longDescription) {
    super(mediumDescription, "Synchronization is disabled", longDescription);
    myMediumDescription = mediumDescription;
  }
  
  public static SyncNotAllowedException shortReason(String reason) {
    return new SyncNotAllowedException(reason, reason);
  }
  
  public static SyncNotAllowedException longReason(String shortReason, String longReason) {
    return new SyncNotAllowedException(shortReason, longReason);
  }

  @Override
  public String getMediumDescription() {
    return myMediumDescription;
  }
}
