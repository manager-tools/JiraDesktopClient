package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBAttribute;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface UploadDrain extends DBDrain {
  /**
   * Notifies that item upload is successfully complete. All changes are uploaded. New server state should be written to return value.
   * @param item the item that was uploaded
   * @return version creator to hold new server values
   * @deprecated use {@link #finishUpload(long, java.util.Collection, int)}
   */
  @Deprecated
  ItemVersionCreator setAllDone(long item);

  /**
   * Notifies that item upload is complete (may be partially or completely failed). Informs what part of initial upload
   * task was surely uploaded.<br>
   * This method unlocks this item upload if item is locked by this upload process. If the item is locked by another
   * upload process the method does nothing and returns null.<br>
   * If the item is not locked for upload the method does nothing and returns creator for general download.<br><br>
   * <b>Note on conflicts</b><br>
   * After (even) partial upload new server state becomes new base version for the item.<br><br>
   * If the item is partially upload it blocks conflict check. So if item conflict is detected then uploader should not report that
   * any state value or history record is uploaded (and the uploader should not perform any upload too)<br>
   * @param item the uploaded item
   * @param uploaded part of state change that was uploaded. Null means empty collection
   * @param uploadedHistory number of first history steps that was uploaded
   * @return creator to write actual server state after upload or null if the item is locked for upload by another upload
   * process (even download is not possible).
   */
  @Nullable
  ItemVersionCreator finishUpload(long item, Collection<? extends DBAttribute<?>> uploaded, int uploadedHistory);

  /**
   * Cancel upload of the specified item. The item upload lock is released - items becomes uploadable and downloadable.<br>
   * Invocation of this method is equal to {@link #finishUpload(long, java.util.Collection, int) finishUpload(item, null, 0)}
   * @param item to cancel upload
   */
  void cancelUpload(long item);

  /**
   * @return items allowed for upload within this upload process.<br>
   * Returned list doesn't include items which are already cancelled or upload is finished (including items which are
   * cancelled or finished within this transaction).
   * @see #cancelUpload(long)
   * @see #setAllDone(long)
   */
  LongList getLockedForUpload();
}
