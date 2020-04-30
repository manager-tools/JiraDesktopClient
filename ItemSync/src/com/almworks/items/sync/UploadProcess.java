package com.almworks.items.sync;

import com.almworks.integers.LongList;
import com.almworks.items.api.DBResult;
import org.jetbrains.annotations.Nullable;

public interface UploadProcess {
  /**
   * To be called when upload to server is done and upload result has to be stored to DB. The provided
   * {@link UploadDrain} allows to update item state and set upload result
   * (what upload steps are successful and failed).
   * @param writer procedure to write new server item state
   * @return DBResult of enquired write transaction.
   */
  DBResult<Object> writeUploadState(DownloadProcedure<? super UploadDrain> writer);

  /**
   * Notifies that upload is finished. Upload of any item not written via {@link #writeUploadState(com.almworks.items.sync.DownloadProcedure)}
   * is cancelled and corresponding upload lock is released. If actually the item is uploaded the future self conflict
   * is possible.<br>
   * After the method is called {@link #writeUploadState(com.almworks.items.sync.DownloadProcedure)} still can be used, however
   * the {@link UploadDrain} acts as generic download drain.
   */
  void uploadDone();

  void cancelUpload(long item);

  /**
   * @param items items to cancel upload. null means cancel whole upload
   */
  void cancelUpload(@Nullable LongList items);
}
