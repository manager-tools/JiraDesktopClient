package com.almworks.jira.provider3.services.upload;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.sync.ItemVersion;
import com.almworks.restconnector.RestSession;
import com.almworks.util.Pair;
import com.almworks.util.text.TextUtil;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public interface UploadUnit {
  /**
   * Value for purpose argument of {@link #loadServerState(com.almworks.restconnector.RestSession, com.almworks.items.entities.api.collector.transaction.EntityTransaction, UploadContext, org.almworks.util.TypedKey)}<br>
   * Downloads server state before upload. Unit may require this state to ensure there is no conflicting changes on server or if it requires additional info.
   */
  TypedKey<Boolean> BEFORE_UPLOAD = TypedKey.create("loadServerStateBeforeUpload");
  /**
   * Value for purpose argument of {@link #loadServerState(com.almworks.restconnector.RestSession, com.almworks.items.entities.api.collector.transaction.EntityTransaction, UploadContext, org.almworks.util.TypedKey)}<br>
   * Loads server state after successful or failed upload and stores data to transaction
   */
  TypedKey<Boolean> AFTER_UPLOAD = TypedKey.create("loadServerStateAfterUpload");
  /**
   * Replies if the unit is done - has nothing to upload.<br>
   * If the unit is {@link #isSurelyFailed(UploadContext) already failed} it may return true or false. So isDone() == true does not mean success.
   * @return true if unit needs no server changes. This may happen because of it is already done, or because of it has detected that there is nothing to change on server
   * @see #isSurelyFailed(UploadContext)
   */
  boolean isDone();

  /**
   * This method is intended to be used by upload subsystem only. If a caller needs to know if a particular unit has failed it should ask upload subsystem.
   * @return true if the unit is sure that it won't succeed. However unit may fail due to exception and does not hold corresponding info.
   * @see #isDone()
   * @see UploadContextImpl#isFailed(UploadUnit)
   */
  boolean isSurelyFailed(UploadContext context);

  /**
   * Download server state for specified purpose.
   * @param purpose the purpose. Can be: {@link #BEFORE_UPLOAD}, {@link #AFTER_UPLOAD}
   * @return load of the state affects several unit and some of them has failures allows to return these failures.<br>
   *         null means no failure happened
   * @throws ConnectorException if a problem has occurred. If an exception is thrown unit is marked as failed and it doesn't participate in upload any more.
   * @see #BEFORE_UPLOAD
   * @see #AFTER_UPLOAD
   */
  @Nullable
  Map<UploadUnit, ConnectorException> loadServerState(RestSession session, EntityTransaction transaction, UploadContext context, TypedKey<Boolean> purpose) throws ConnectorException;

  /**
   * Called when all units have loaded {@link #loadServerState(com.almworks.restconnector.RestSession, com.almworks.items.entities.api.collector.transaction.EntityTransaction, UploadContext, org.almworks.util.TypedKey purpose) server state}.
   * The unit may check the current state for conflict or load additional values.
   *
   * @param transaction loaded server state
   * @return null if the unit may proceed with upload.<br>
   * not null if the unit detected server state that blocks it's upload
   */
  @Nullable
  UploadProblem onInitialStateLoaded(EntityTransaction transaction, UploadContext context);

  /**
   * @return null (or empty) if upload is succeeded or deferred. The upload process need to perform subsequent upload attempts for the unit<br>
   * not-null: problems that have occurred. If the problem is not fatal should return {@link UploadProblem#notNow(String)} - it means that another attempt should be made later if
   * any other unit succeeds or surely fail (exception or fatal problem).
   * @throws ConnectorException indicated failure
   */
  @Nullable
  Collection<? extends UploadProblem> perform(RestSession session, UploadContext context) throws ConnectorException, UploadProblem.Thrown;

  /**
   * Called after final server state download is done. The unit should report what part is successfully uploaded
   * @param transaction final server state
   */
  void finishUpload(EntityTransaction transaction, PostUploadContext context);

  /**
   * @return [item, displayableName] collection of pairs that represents master items of this unit - the items it belongs too.
   */
  @NotNull
  Collection<Pair<Long,String>> getMasterItems();

  interface Factory {
    /**
     * Collects dependencies of the mandatory item.<br>
     *
     * @throws CantUploadException means that item surely cannot participate in this upload (for example due to some other item must be uploaded before, and cannot be uploaded now).
     * @see CollectUploadContext#requestUpload(long, boolean)
     */
    void collectRelated(ItemVersion trunk, CollectUploadContext context) throws CantUploadException;

    /**
     * Constructs upload steps (units) for the item. The factory loads item-related data from DB and links the created unit with dependencies.
     * The factory may reply (return null) this means it cannot create steps right now - it requires other items to be prepared<br><br>
     * Example: to create a link both ends must exist on server. But if an end is NEW than create link need reference to the unit to submit the NEW issue.
     * @return not null collection if the item is prepared for upload. All required upload steps (units) are loaded from DB.<br>
     * null if the factory is waiting for dependencies to be prepared. The factory should be called later again.
     * @throws CantUploadException if no upload is possible due to this or other item state or missing related item in upload.
     */
    @Nullable
    Collection<? extends UploadUnit> prepare(ItemVersion item, LoadUploadContext context) throws CantUploadException;
  }

  class CantUploadException extends Exception {
    public CantUploadException(String debugMessage) {
      super(debugMessage);
    }

    public CantUploadException(String debugMessage, Throwable cause) {
      super(debugMessage, cause);
    }

    public static CantUploadException create(Object ... message) {
      return new CantUploadException(TextUtil.separateToString(Arrays.asList(message), " "));
    }

    public static CantUploadException internalError() {
      return new CantUploadException("Internal error has occurred");
    }
  }
}
