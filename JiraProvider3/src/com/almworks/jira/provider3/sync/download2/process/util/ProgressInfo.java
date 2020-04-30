package com.almworks.jira.provider3.sync.download2.process.util;

import com.almworks.api.connector.CancelledException;
import com.almworks.jira.provider3.sync.ConnectorManager;
import com.almworks.util.LogHelper;
import com.almworks.util.files.FileUtil;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.io.StreamTransferTracker;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.progress.Progress;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ProgressInfo {
  private static final LocalizedAccessor.Message3 HTTP_STATUS_ERROR = ConnectorManager.LOCAL.message3("progress.error.httpStatus");
  private static final LocalizedAccessor.Message3 TRACKER_PROGRESS_TOTAL = ConnectorManager.LOCAL.message3("progress.message.tracker.total");
  private final Progress myProgress;
  private final ScalarModel<Boolean> myCancelFlag;
  private double myOwnRatio = 1;

  public ProgressInfo(Progress progress, ScalarModel<Boolean> cancelFlag) {
    myProgress = progress;
    myCancelFlag = cancelFlag;
  }

  public static ProgressInfo createDeaf(ScalarModel<Boolean> cancelFlag) {
    return new ProgressInfo(new Progress.Deaf(), cancelFlag);
  }

  public static ProgressInfo createDeaf() {
    BasicScalarModel<Boolean> cancelFlag = BasicScalarModel.create(true);
    cancelFlag.setValue(false);
    return new ProgressInfo(new Progress.Deaf(), cancelFlag);
  }

  public void checkCancelled() throws CancelledException {
    checkCancelled(getCancelFlag());
  }

  public static void checkCancelled(ScalarModel<Boolean> cancelFlag) throws CancelledException {
    if (Boolean.TRUE.equals(cancelFlag.getValue())) throw new CancelledException();
  }

  public void addError(String message) {
    myProgress.addError(message);
  }

  public void addHttpStatusError(String url, int statusCode, String statusMessage) {
    addError(HTTP_STATUS_ERROR.formatMessage(url, String.valueOf(statusCode), statusMessage));
  }

  public void setDone() {
    myProgress.setDone();
  }

  public void startActivity(String message) throws CancelledException {
    checkCancelled();
    myProgress.setActivity(message);
  }

  public ProgressInfo spawn(double ratio) {
    return spawn(ratio, null);
  }

  public ProgressInfo spawn(double ratio, @Nullable String debugName) {
    ratio = Math.min(1, Math.max(0, ratio));
    Progress spawn = new Progress(Util.NN(debugName));
    myProgress.delegate(spawn, myOwnRatio * ratio);
    myOwnRatio *= (1 - ratio);
    return new ProgressInfo(spawn, myCancelFlag);
  }

  public ProgressInfo spawnAll() {
    return spawn(0.95);
  }

  public Progress getProgress() {
    return myProgress;
  }

  public ScalarModel<Boolean> getCancelFlag() {
    return myCancelFlag;
  }

  public ProgressInfo[] split(int count) {
    LogHelper.assertError(count >= 0, count);
    if (count <= 0) return new ProgressInfo[0];
    double ratio = 0.95 / count;
    ProgressInfo[] result = new ProgressInfo[count];
    for (int i = 0; i < count; i++) {
      Progress spawn = new Progress();
      myProgress.delegate(spawn, myOwnRatio * ratio);
      result[i] = new ProgressInfo(spawn, myCancelFlag);
    }
    myOwnRatio *= 0.05;
    return result;
  }

  public ProgressInfo[] splitRatio(double ... ratios) {
    if (ratios.length == 0) return new ProgressInfo[0];
    double total = 0;
    for (double ratio : ratios) total += ratio;
    ProgressInfo[] result = new ProgressInfo[ratios.length];
    double multiplier = 0.95 / total;
    for (int i = 0; i < ratios.length; i++) {
      double ratio = ratios[i];
      Progress spawn = new Progress();
      myProgress.delegate(spawn, multiplier * ratio * myOwnRatio);
      result[i] = new ProgressInfo(spawn, myCancelFlag);
    }
    myOwnRatio *= 0.05;
    return result;
  }

  public BytesTracker spawnTransferTracker(double ratio) {
    ProgressInfo spawn = spawn(ratio);
    return new BytesTracker(spawn.getProgress(), myCancelFlag);
  }

  public static class BytesTracker implements StreamTransferTracker {
    private final Progress myProgress;
    private final ScalarModel<Boolean> myCancelFlag;
    private long myTotal = -1;
    private String myTotalStr;

    public BytesTracker(Progress progress, ScalarModel<Boolean> cancelFlag) {
      myProgress = progress;
      myCancelFlag = cancelFlag;
    }

    public void onTransfer(long bytesTransferred) throws IOException {
      if (myTotal > 0 && myTotalStr != null && bytesTransferred > 0) {
        float percent = Math.min(1.0F * bytesTransferred / myTotal, 1.0F);
        String percentStr = String.valueOf(Math.floor(percent * 100));
        String transferred = FileUtil.displayableSize(bytesTransferred);
        String activity = TRACKER_PROGRESS_TOTAL.formatMessage(percentStr, transferred, myTotalStr);
        myProgress.setProgress(percent, activity);
      } else {
        myProgress.setActivity(FileUtil.displayableSize(bytesTransferred));
      }
      if (Boolean.TRUE.equals(myCancelFlag.getValue())) throw new IOException("Cancelled");
    }

    @Override
    public void setLength(long length) {
      myTotal = length;
      myTotalStr = length <= 0 ? null : FileUtil.displayableSize(length);
    }

    public void setDone() {
      myProgress.setDone();
    }
  }
}
