package com.almworks.jira.connector2;

import com.almworks.api.connector.ConnectorException;
import org.jetbrains.annotations.Nullable;

public class JiraException extends ConnectorException {
  private final JiraCause myCause;

  public JiraException(String message, String shortDescription, String longDescription, JiraCause cause) {
    super(message, shortDescription, longDescription);
    myCause = cause;
  }

  public JiraException(String message, Throwable cause, String shortDescription, String longDescription, JiraCause jiraCause) {
    super(message, cause, shortDescription, longDescription);
    if ((jiraCause == null || jiraCause == JiraCause.GENERIC_UPDATE) && cause instanceof JiraException) {
      JiraCause c = ((JiraException) cause).getJiraCause();
      jiraCause = c != null ? c : jiraCause;
    }
    myCause = jiraCause;
  }

  @Nullable
  public JiraCause getJiraCause() {
    return myCause;
  }

  public enum JiraCause {
    /**
     * Missing mandatory data
     */
    MISSING_DATA,
    /**
     * Compatibility error
     */
    COMPATIBILITY,
    /**
     * Generic update failure
     */
    GENERIC_UPDATE,
    /**
     * Move failed
     */
    MOVE_FAILED,
    /**
     * Cannot upload invalid data
     */
    INVALID_DATA,
    /**
     * Suspecting repetitive upload
     */
    REPETITIVE_UPLOAD,
    /**
     * Problem uploading attachments
     */
    ATTACHMENTS_UPLOAD,
    /**
     * Remote conflict detected
     */
    CONFLICT,
    /**
     * Remote issue not found
     */
    ISSUE_NOT_FOUND,
    /**
     * Access denied
     */
    ACCESS_DENIED
  }
}
