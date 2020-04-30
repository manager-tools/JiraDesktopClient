package com.almworks.api.download;

import com.almworks.api.http.HttpLoaderException;
import com.almworks.api.http.HttpResponseData;
import com.almworks.util.AppBook;
import com.almworks.util.i18n.LText2;
import com.almworks.util.io.StreamTransferTracker;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import org.almworks.util.detach.DetachComposite;

import java.io.IOException;

public interface DownloadOwner {

  String getDownloadOwnerID();

  boolean isValid();

  HttpResponseData load(DetachComposite life, String argument, boolean retrying, boolean noninteractive, BasicScalarModel<Boolean> cancelFlag)
    throws CannotCreateLoaderException, IOException, HttpLoaderException;

  class DownloadTracker implements StreamTransferTracker {
    public static final String X = "Application.Download.";
    private static final LText2 DOWNLOADED_PERCENT =
      AppBook.text(X + "DOWNLOADED_PERCENT", "Downloaded {0}% of {1,number,#######0}kb", 0, 0L);

    private final Progress myProgress = new Progress("DTR");
    private long myTotal = -1;

    public DownloadTracker() {
    }

    public void onTransfer(long bytesTransferred) {
      if (myTotal > 0 && bytesTransferred > 0) {
        float percent = Math.min(1.0F * bytesTransferred / myTotal, 1.0F);
        String activity = DOWNLOADED_PERCENT.format((int) (percent * 100), (long) (myTotal / 1024));
        myProgress.setProgress(percent, activity);
      }
    }

    public ProgressSource getProgress() {
      return myProgress;
    }

    @Override
    public void setLength(long length) {
      myTotal = length;
    }
  }
}
