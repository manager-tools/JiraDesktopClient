package com.almworks.api.engine;

import com.almworks.util.Pair;
import com.almworks.util.collections.Convertor;
import com.almworks.util.collections.Functional;
import com.almworks.util.commons.Condition;
import com.almworks.util.commons.Function;
import com.almworks.util.model.SetHolderModel;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.Nullable;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface ItemSyncProblem extends SyncProblem {
  Convertor<ItemSyncProblem, Long> TO_ITEM =
  new Convertor<ItemSyncProblem, Long>() {
    public Long convert(ItemSyncProblem problem) {
      return problem.getItem();
    }
  };

  Function<Iterable<SyncProblem>, Iterable<ItemSyncProblem>> SELECT = Functional.compose(Functional.filterIterable(Condition.<ItemSyncProblem>notNull()), Functional.convertIterable(Convertor.<SyncProblem, ItemSyncProblem>downCastOrNull(ItemSyncProblem.class)));

  long getItem();

  /**
   * @return detach that removes from the collection
   */
  Detach addToCollection(SetHolderModel<SyncProblem> problems);

  void disappear();

  String getDisplayableId();

  Pair<String, Boolean> getCredentialState();

  boolean isCauseForRemoval();

  @Nullable
  Cause getCause();

  enum Cause {
    /**
     * Remote item not found
     */
    REMOTE_NOT_FOUND,
    /**
     * Generic upload error occured
     */
    GENERIC_UPLOAD_FAILURE,
    /**
     * Conflict with remote item detected
     */
    UPLOAD_CONFLICT,
    /**
     * Compatibility problem detected
     */
    COMPATIBILITY,
    /**
     * Suspecting repetitive upload
     */
    REPETITIVE_UPLOAD,
    /**
     * Remote item access denied
     */
    ACCESS_DENIED,
    /**
     * Cannot upload illegal data
     */
    ILLEGAL_DATA
  }
}
