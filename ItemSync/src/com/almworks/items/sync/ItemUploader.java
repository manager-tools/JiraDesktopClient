package com.almworks.items.sync;

import com.almworks.integers.LongList;
import org.jetbrains.annotations.Nullable;

/**
 * Upload process consists of two steps:<br>
 * 1. Prepare ({@link #prepare(ItemUploader.UploadPrepare)} - reads DB changes
 * and collect data for upload.<br>
 * 2. Upload ({@link #doUpload(UploadProcess)} - calls server API to upload user changes
 * to server. When done invokes {@link UploadProcess#writeUploadState(DownloadProcedure)} to store new server state to DB.
 */
public interface ItemUploader {
  /**
   * Prepares upload. Locks items for upload and reads data to be uploaded.
   * @param prepare facility to modify set of items which needs upload
   * @see UploadPrepare#addToUpload(long)
   */
  void prepare(UploadPrepare prepare);

  /**
   * Second step of upload process. Invoked when preparation DB transaction is finished (see {@link #prepare(ItemUploader.UploadPrepare)}).<br>
   * This method is call on the thread the {@link SyncManager#syncUpload(ItemUploader)} is invoked.<br>
   * When the method return (normally or with an exception) upload finishes - all items locked for the upload are unlocked.<br>
   * @param process sink for result of upload
   * @see UploadProcess#writeUploadState(DownloadProcedure)
   * @see com.almworks.items.sync.UploadDrain#setAllDone(long)
   * @see UploadDrain#finishUpload(long, java.util.Collection, int)
   * @throws InterruptedException if the thread is interrupted
   */
  void doUpload(UploadProcess process) throws InterruptedException;

  interface UploadPrepare {
    /**
     * Checks that the item can be uploaded and locks it for upload.<br>
     * Item cannot be added to the upload if:<br>
     * 1. It does not {@link com.almworks.items.util.SyncAttributes#EXISTING exist}<br>
     * 2a. It is already participating in another upload - no item can be uploaded twice at the same time<br>
     * 2b. Previous upload is not {@link com.almworks.items.sync.impl.SyncSchema#DONE_UPLOAD processed} yet - server communication is already complete but result is not clear yet.<br>
     * 3. It needs merge<br>
     * 4. It has not changes. Any changed item must have {@link com.almworks.items.sync.impl.SyncSchema#BASE original} server state.
     * @param item to upload
     * @return if the item is successfully locked for upload returns TRUNK version of the item.
     * <br>If the item cannot be uplaoded right now returns null
     */
    @Nullable
    ItemVersion addToUpload(long item);

    /**
     * Saves upload attempt data for the item (set {@link com.almworks.items.sync.impl.SyncSchema#UPLOAD_ATTEMPT}).<br>
     * If the item is added for upload - does nothing.<br>
     * If the item upload is cancelled later (via {@link #removeFromUpload(long)} or {@link #cancelUpload()}) restores previous attempt value
     * @param attempt value for {@link com.almworks.items.sync.impl.SyncSchema#UPLOAD_ATTEMPT} attribute
     * @return true iff the value is changed, false means the item is not added to upload.
     * @see com.almworks.items.sync.impl.SyncSchema#UPLOAD_ATTEMPT
     * @see #addToUpload(long)
     * @see #addAllToUpload(com.almworks.integers.LongList)
     * @see #cancelUpload()
     * @see #removeFromUpload(long)
     */
    boolean setUploadAttempt(long item, byte[] attempt);

    boolean addAllToUpload(LongList items);

    /**
     * Check that item can be added to the upload
     * @param item item to test
     * @return true if {@link #addToUpload(long)} like succeed, false means that {@link #addToUpload(long)} surely fail
     */
    boolean isUploadable(long item);

    /**
     * @param item item to test
     * @return true if the item is already registered for this upload - {@link #addToUpload(long) add to upload} is already successfully done.
     */
    boolean isPrepared(long item);

    void cancelUpload();

    void removeFromUpload(LongList items);

    void removeFromUpload(long item);

    VersionSource getTrunk();
  }
}
