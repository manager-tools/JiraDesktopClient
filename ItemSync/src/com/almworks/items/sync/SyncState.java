package com.almworks.items.sync;

import org.almworks.util.Log;

public enum SyncState {
  /**
   * No local changes. Synchronized state
   */
  SYNC,
  /**
   * Item is new local, not ever uploaded to server
   */
  NEW,
  /**
   * Item is modified version of remote item.
   */
  EDITED,
  /**
   * Item is locally deleted, no concurrent remote change is known
   */
  LOCAL_DELETE,
  /**
   * Item is locally deleted but concurrently updated on server
   */
  DELETE_MODIFIED,
  /**
   * Item is locally modified but deleted on server
   */
  MODIFIED_CORPSE,
  /**
   * Item has local and remote concurrent modifications
   */
  CONFLICT;

  public boolean isLocallyDeleted() {
    switch (this) {
    case SYNC:
    case NEW:
    case EDITED:
    case MODIFIED_CORPSE:
    case CONFLICT: return false;
    case LOCAL_DELETE:
    case DELETE_MODIFIED: return true;
    default: Log.error("Wrong state " + this); return false;
    }
  }

  /**
   * @return the item doesnt exists on server (not yet submitted or already deleted)
   */
  public boolean isLocalOnly() {
    switch (this) {
    case NEW:
    case MODIFIED_CORPSE: return true;
    case SYNC:
    case EDITED:
    case LOCAL_DELETE:
    case DELETE_MODIFIED:
    case CONFLICT: return false;
    default: Log.error("Wrong state " + this); return false;
    }
  }

  /**
   * @return item has concurrent remote change
   */
  public boolean isConflict() {
    switch (this) {
    case SYNC:
    case NEW:
    case EDITED:
    case LOCAL_DELETE: return false;
    case DELETE_MODIFIED:
    case MODIFIED_CORPSE:
    case CONFLICT: return true;
    default: Log.error("Wrong state " + this); return false;
    }
  }

  public SyncState afterLocalDelete() {
    switch (this) {
    case LOCAL_DELETE:
    case EDITED:
    case SYNC: return LOCAL_DELETE;
    case DELETE_MODIFIED:
    case CONFLICT: return DELETE_MODIFIED;
    case NEW:
    case MODIFIED_CORPSE: return SYNC;
    default: Log.error("Wrong state " + this); return this;
    }
  }

  public SyncState afterEdit() {
    switch (this) {
    case LOCAL_DELETE:
    case SYNC: return EDITED;
    case NEW:
    case CONFLICT:
    case MODIFIED_CORPSE:
    case EDITED: return this;
    case DELETE_MODIFIED: return CONFLICT;
    default: Log.error("Wrong state " + this); return this;
    }
  }
}
