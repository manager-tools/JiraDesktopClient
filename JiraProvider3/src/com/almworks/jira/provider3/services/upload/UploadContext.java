package com.almworks.jira.provider3.services.upload;

import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.util.collections.UserDataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface UploadContext {
  @NotNull
  JiraConnection3 getConnection();

  @NotNull
  UserDataHolder getUserData();

  @NotNull
  UserDataHolder getItemCache(long item);

  /**
   * Checks that unit is already failed and removed from upload queue.<br>
   * The unit is failed if it reports {@link UploadUnit#isSurelyFailed(UploadContext) surely failure} or if it has been asked to {@link UploadUnit#perform(com.almworks.restconnector.RestSession, UploadContext) upload}
   * but has returned or thrown a problem.
   * @param unit unit to test
   * @return true if the unit never succeeds
   */
  boolean isFailed(UploadUnit unit);

  Map<String, FieldKind> getCustomFieldKinds();

  /**
   * Adds problem visible in UI, but do not mark the unit as failed.<br>
   * This method allows to show a problem to a user without blocking or terminating upload.
   * @see #isFailed(UploadUnit)
   */
  void addMessage(UploadUnit unit, UploadProblem message);
}
