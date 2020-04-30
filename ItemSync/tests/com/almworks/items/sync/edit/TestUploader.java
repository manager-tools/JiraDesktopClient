package com.almworks.items.sync.edit;

import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.items.sync.*;
import com.almworks.items.sync.util.TestReference;
import com.almworks.items.sync.util.UploadAllToDrain;
import com.almworks.items.util.AttributeMap;
import com.almworks.util.LogHelper;
import com.almworks.util.collections.LongSet;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import gnu.trove.TLongObjectHashMap;
import junit.framework.Assert;
import org.almworks.util.RuntimeInterruptedException;
import org.jetbrains.annotations.Nullable;
import util.concurrent.Synchronized;
import util.concurrent.SynchronizedBoolean;

class TestUploader implements ItemUploader {
  private final TestReference<UploadProcess> myProcess = new TestReference<UploadProcess>();
  private final LongList myItems;
  private final TLongObjectHashMap<AttributeMap> myUploadValues = new TLongObjectHashMap<>();
  private final LongSet mySeenItems = new LongSet();
  private final Synchronized<Procedure<UploadProcess>> myFinish = new Synchronized<Procedure<UploadProcess>>(null);
  private final SynchronizedBoolean myUploadComplete = new SynchronizedBoolean(false);

  private TestUploader(LongList items) {
    myItems = items;
  }

  public static void uploadItem(SyncManager manager, long item) throws InterruptedException {
    TestUploader uploader = beginUpload(manager, item);
    Assert.assertNotNull(uploader);
    Assert.assertTrue(uploader.waitStarted());
    AttributeMap values = uploader.myUploadValues.get(item);
    Assert.assertNotNull(values);
    UploadAllToDrain completer = new UploadAllToDrain(item, values);
    uploader.finishUpload(completer);
    Assert.assertTrue(completer.waitDone());
  }

  @Nullable
  public static TestUploader beginUpload(final SyncManager manager, long ... items) {
    LongArray itemList = LongArray.create(items);
    TestUploader uploader = new TestUploader(itemList);
    UploadWrapper wrapper = new UploadWrapper(manager, uploader);
    ThreadGate.LONG(wrapper).execute(wrapper);
    return wrapper.isStarted() ? wrapper.getUploader() : null;
  }

  public UploadAllToDrain finishUpload(final long item, final AttributeMap uploaded) throws InterruptedException {
    UploadAllToDrain completer = new UploadAllToDrain(item, uploaded);
    finishUpload(completer);
    return completer;
  }

  public void finishUpload(final DownloadProcedure<UploadDrain> finish) throws InterruptedException {
    sendToUploadProcess(new Procedure<UploadProcess>() {
      @Override
      public void invoke(UploadProcess arg) {
        arg.writeUploadState(finish);
      }
    });
  }

  private void sendToUploadProcess(Procedure<UploadProcess> procedure) throws InterruptedException {
    boolean ok = myFinish.commit(null, procedure);
    Assert.assertTrue(ok);
  }

  @Override
  public void prepare(UploadPrepare prepare) {
    for (int i = 0; i < myItems.size(); i++) {
      long item = myItems.get(i);
      ItemVersion trunk = prepare.addToUpload(item);
      mySeenItems.add(item);
      if (trunk != null) myUploadValues.put(item, trunk.getAllShadowableMap());
    }
  }

  @Override
  public void doUpload(UploadProcess process) throws InterruptedException {
    Assert.assertTrue(myProcess.compareAndSet(null, process));
    Procedure<UploadProcess> finish = myFinish.waitForNotNull();
    finish.invoke(process);
  }

  public void waitUploadComplete() throws InterruptedException {
    myUploadComplete.waitForValue(true);
  }

  public boolean waitStarted() throws InterruptedException {
    return myProcess.waitForPublished() != null;
  }

  public LongList getRequestedItems() {
    return LongArray.create(myUploadValues.keys());
  }

  public LongList getSeenItems() {
    return mySeenItems;
  }

  public void sendUploadDone() throws InterruptedException {
    sendToUploadProcess(new Procedure<UploadProcess>() {
      @Override
      public void invoke(UploadProcess arg) {
        arg.uploadDone();
      }
    });
  }

  private static class UploadWrapper implements Runnable, ItemUploader {
    private final SyncManager myManager;
    private final TestUploader myUploader;
    private final Synchronized<Boolean> myStarted = new Synchronized<Boolean>(null);
    private final Throwable myCreatedFrame = new Throwable();

    public UploadWrapper(SyncManager manager, TestUploader uploader) {
      myManager = manager;
      myUploader = uploader;
    }

    @Override
    public void run() {
      try {
        myManager.syncUpload(this);
      } catch (InterruptedException e) {
        LogHelper.error(e);
      } finally {
        myUploader.myUploadComplete.set(true);
        myStarted.commit(null, false);
      }
    }

    public TestUploader getUploader() {
      return myUploader;
    }

    public boolean isStarted() {
      try {
        return myStarted.waitForNotNull();
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }

    @Override
    public void prepare(UploadPrepare prepare) {
      myUploader.prepare(prepare);
    }

    @Override
    public void doUpload(UploadProcess process) throws InterruptedException {
      myStarted.set(true);
      myUploader.doUpload(process);
    }
  }
}
