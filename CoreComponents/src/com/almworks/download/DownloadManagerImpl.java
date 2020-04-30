package com.almworks.download;

import com.almworks.api.download.*;
import com.almworks.api.http.*;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.util.AppBook;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Condition;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.i18n.LText;
import com.almworks.util.i18n.LText1;
import com.almworks.util.io.IOUtils;
import com.almworks.util.io.StreamTransferTracker;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableString;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.progress.Progress;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.Threads;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;
import util.concurrent.SynchronizedBoolean;

import javax.mail.internet.MimeUtility;
import java.io.*;
import java.util.Collection;
import java.util.Map;

import static com.almworks.util.collections.Functional.filter;
import static org.almworks.util.Collections15.arrayList;

public class DownloadManagerImpl implements DownloadManager, Startable {
  private static final LText WAITING_FOR_REPLY = AppBook.text(DownloadOwner.DownloadTracker.X + "WAITING_FOR_REPLY", "Waiting for reply from server");
  private static final LText1<String> CANCELLED_ERROR = AppBook.text(DownloadOwner.DownloadTracker.X + "CANCELLED_ERROR", "Download cancelled", "");
  private static final LText1<String> CANNOT_INITIATE_DOWNLOAD =
    AppBook.text(DownloadOwner.DownloadTracker.X + "CANNOT_INITIATE_DOWNLOAD", "Cannot initiate download: {0}", "");
  private static final LText1<String> DOWNLOAD_ERROR = AppBook.text(DownloadOwner.DownloadTracker.X + "DOWNLOAD_ERROR", "{0}", "");
  private static final LText1<String> HTTP_DOWNLOAD_ERROR =
    AppBook.text(DownloadOwner.DownloadTracker.X + "HTTP_DOWNLOAD_ERROR", "HTTP response: {0}", "");
  private static final LText DOWNLOAD_INTERRUPTED =
    AppBook.text(DownloadOwner.DownloadTracker.X + "DOWNLOAD_INTERRUPTED", "Download was interrupted");

  private final DownloadOwnerResolverImpl myDownloadOwnerResolver;

  private Bottleneck mySaver;

  private final Map<String, DownloadEntry> myDownloads = Collections15.hashMap();
  private final Map<String, FireEventSupport<FileDownloadListener>> myListeners = Collections15.hashMap();
  private final Map<String, Detach> myDownloadDetaches = Collections15.hashMap();

  private final Object myLock = new Object();

  private final Store myStore;
  private final WorkArea myWorkArea;
  private final SynchronizedBoolean myLoaded = new SynchronizedBoolean(false);

  public DownloadManagerImpl(Store store, WorkArea workArea) {
    myStore = store;
    myWorkArea = workArea;
    myDownloadOwnerResolver = new DownloadOwnerResolverImpl();
  }

  public Detach addFileDownloadListener(final String key, final ThreadGate gate, final FileDownloadListener listener) {
    assert key != null;
    assert gate != null;
    assert listener != null;
    DownloadEntry entry;
    final FireEventSupport<FileDownloadListener> support;
    ensureLoaded();
    synchronized (myLock) {
      entry = myDownloads.get(key);
      FireEventSupport<FileDownloadListener> sup = myListeners.get(key);
      if (sup == null) {
        sup = FireEventSupport.createSynchronized(FileDownloadListener.class);
        myListeners.put(key, sup);
      }
      support = sup;
    }
    final DetachComposite detach = new DetachComposite(true);
    final DownloadedFile current = entry == null ? new EmptyDownloadedFile(key) : entry;
    gate.execute(new Runnable() {
      public void run() {
        listener.onDownloadStatus(current);
        support.addListener(detach, gate, listener);
        detach.add(new Detach() {
          public void doDetach() {
            synchronized (myLock) {
              FireEventSupport<FileDownloadListener> sup = myListeners.get(key);
              if (sup != null && sup.getListenersCount() == 0)
                myListeners.remove(key);
            }
          }
        });
      }
    });
    return detach;
  }

  @NotNull
  public DownloadedFile getDownloadStatus(String key) {
    ensureLoaded();
    DownloadEntry entry;
    synchronized (myLock) {
      entry = myDownloads.get(key);
    }
    return entry == null ? new EmptyDownloadedFile(key) : entry;
  }


  public void removeFileDownloadListener(String key, FileDownloadListener listener) {
    synchronized (myLock) {
      FireEventSupport<FileDownloadListener> sup = myListeners.get(key);
      if (sup != null) {
        sup.removeListener(listener);
        if (sup.getListenersCount() == 0) {
          myListeners.remove(key);
        }
      }
    }
  }

  public Detach initiateDownload(String keyURL, DownloadRequest request, boolean force, boolean noninteractive) {
    if (keyURL == null || request == null) return Detach.NOTHING;
    DownloadEntry entry;
    ensureLoaded();
    synchronized (myLock) {
      entry = myDownloads.get(keyURL);
      if (entry == null) {
        entry = new DownloadEntry(keyURL);
        myDownloads.put(keyURL, entry);
      } else {
        assert keyURL.equals(entry.getKeyURL());
      }
      entry.updateRequest(request);
    }
    entryChanged(keyURL);
    return download(entry, force, noninteractive);
  }

  public Detach registerDownloadOwner(DownloadOwner owner) {
    return myDownloadOwnerResolver.register(owner);
  }

  public void cancelDownload(String keyURL) {
    Detach detach;
    synchronized (myDownloadDetaches) {
      detach = myDownloadDetaches.get(keyURL);
    }
    if (detach != null)
      detach.detach();
  }

  public void checkDownloadedFile(String url) {
    DownloadEntry entry;
    synchronized (myLock) {
      entry = myDownloads.get(url);
    }
    checkEntryFile(entry);
  }

  public DownloadedFile storeDownloadedFile(String attachmentURL, File file, String mimeType) throws IOException {
    if (file == null)
      return null;
    return storeFile(attachmentURL, file.getName(), mimeType, file);
  }

  public DownloadedFile storeDownloadedFile(String attachmentURL, String filename, String mimeType, byte[] data)
    throws IOException
  {
    if (data == null)
      return null;
    return storeFile(attachmentURL, filename, mimeType, data);
  }

  private DownloadedFile storeFile(String attachmentURL, String filename, String mimeType, final Object source)
    throws IOException
  {
    if (source == null)
      return null;
    assert source instanceof File || source instanceof byte[] : source.getClass();
    Threads.assertLongOperationsAllowed();
    ensureLoaded();
    DownloadEntry entry;
    synchronized (myLock) {
      entry = myDownloads.get(attachmentURL);
      if (entry == null) {
        entry = new DownloadEntry(attachmentURL);
        myDownloads.put(attachmentURL, entry);
      }
    }
    cancelDownload(entry.getKeyURL());
    entry.setState(DownloadedFile.State.UNKNOWN);
    entryChanged(entry);
    File existingFile = entry.getFile();
    if (existingFile != null) {
      try {
        FileUtil.deleteFile(existingFile, true);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }

    long length = source instanceof byte[] ? ((byte[]) source).length : ((File) source).length();

    entry.updateRequest(new DownloadRequest(DownloadOwnerResolverImpl.DisabledOwner.INSTANCE, null, filename, length));
    entry.setLastDownloadError(null);
    entry.setMimeType(mimeType);
    entryChanged(entry);

    try {
      File resultFile;
      if (source instanceof byte[]) {
        resultFile = makeFile(filename, null, mimeType, (byte[]) source);
      } else {
        resultFile = makeFile(filename, null, mimeType, (File) source);
      }
      entry.setFile(resultFile);
      entry.setMimeType(mimeType);
      entry.setSize(resultFile.length());
      entry.setLastModified(resultFile.lastModified());
      entry.setState(DownloadedFile.State.READY);
      entryChanged(entry);
    } catch (IOException e) {
      entry.setState(DownloadedFile.State.DOWNLOAD_ERROR);
      entry.setLastDownloadError("Cannot copy file: " + e.getMessage());
      entryChanged(entry);
      throw e;
    }
    return entry;
  }

  private void checkEntryFile(DownloadEntry entry) {
    if (entry == null)
      return;
    if (entry.getState() != DownloadedFile.State.READY)
      return;
    File file = entry.getFile();
    if (file == null)
      return;
    if (!(file.isFile() && file.canRead())) {
      entry.setState(DownloadedFile.State.LOST);
      entryChanged(entry);
    } else {
      long modified = file.lastModified();
      long size = file.length();
      if (entry.getLastModifed() != modified || entry.getSize() != size) {
        entry.setLastModified(modified);
        entry.setSize(size);
        entryChanged(entry);
      }
    }
  }

  public void start() {
    mySaver = new Bottleneck(1000, ThreadGate.LONG(DownloadManagerImpl.class), new Runnable() {
      public void run() {
        doSave();
      }
    });
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        doLoad();
      }
    });
  }

  public void stop() {
  }

  public Detach download(final DownloadEntry entry, boolean force, final boolean noninteractive) {
    DownloadedFile.State state = entry.getState();
    if (state == DownloadedFile.State.DOWNLOADING || state == DownloadedFile.State.QUEUED ||
      state == DownloadedFile.State.READY)
    {
      if (!force)
        return Detach.NOTHING;
      cancelDownload(entry.getKeyURL());
      entry.setState(DownloadedFile.State.UNKNOWN);
      entryChanged(entry);
    }
    final DownloadRequest request = entry.getLastRequest();
    if (request == null)
      return Detach.NOTHING;
    final DownloadOwner owner = request.getOwner();
    if (!owner.isValid())
      return Detach.NOTHING;
    entry.setLastDownloadError(null);
    entry.setState(DownloadedFile.State.QUEUED);
    entryChanged(entry);
    final DetachComposite detach = new DetachComposite(true);
    synchronized (myDownloadDetaches) {
      myDownloadDetaches.put(entry.getKeyURL(), detach);
    }
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        try {
          boolean shouldRetry = doDownload(entry, request, owner, detach, noninteractive, false);
          if (shouldRetry) {
            doDownload(entry, request, owner, detach, noninteractive, true);
          }
        } finally {
          synchronized (myDownloadDetaches) {
            myDownloadDetaches.remove(entry.getKeyURL());
          }
        }
      }
    });
    return detach;
  }

  /**
   * @return true if operation should be retried
   */
  private boolean doDownload(DownloadEntry entry, DownloadRequest request, DownloadOwner owner,
    DetachComposite detach, boolean noninteractive, boolean retrying)
  {
    final BasicScalarModel<Boolean> cancelFlag = BasicScalarModel.createWithValue(Boolean.FALSE, true);
    detach.add(new Detach() {
      public void doDetach() {
        cancelFlag.setValue(Boolean.TRUE);
      }
    });
    DetachComposite life = new DetachComposite();
    detach.add(life);
    File tempFile = null;
    try {
      try {
        DownloadOwner.DownloadTracker tracker = new DownloadOwner.DownloadTracker();
        Progress progress = new Progress("DM.AGR");
        Progress waiting = progress.createDelegate(0.1F, "DM.WAIT", WAITING_FOR_REPLY.format());
        progress.delegate(tracker.getProgress(), 0.9F);
        entry.setProgressSource(progress);
        entry.setState(DownloadedFile.State.DOWNLOADING);
        entryChanged(entry);
        String argument = request.getArgument();
        Pair<File, FileOutputStream> pair = createTemporaryFile();
        tempFile = pair.getFirst();
        FileOutputStream output = pair.getSecond();
        Pair<String, String> loadResult = loadFile(owner, life, argument, retrying, noninteractive, cancelFlag, waiting, output, tracker);
        String mimeType = loadResult.getFirst();
        String contentFilename = loadResult.getSecond();
        File resultFile = makeFile(contentFilename, request.getSuggestedFilename(), mimeType, tempFile);
        entry.setFile(resultFile);
        entry.setMimeType(mimeType);
        entry.setSize(resultFile.length());
        entry.setLastModified(resultFile.lastModified());
        entry.setState(DownloadedFile.State.READY);
        entryChanged(entry);
// done!
      } catch (CannotCreateLoaderException e) {
        LogHelper.warning(e.getMessage());
        LogHelper.debug(e);
        setError(entry, CANNOT_INITIATE_DOWNLOAD, e, cancelFlag);
      } catch (IOException e) {
        LogHelper.warning("Download failed:", e.getMessage(), entry.getKeyURL());
        LogHelper.debug("Download failed", e, entry.getKeyURL());
        setError(entry, DOWNLOAD_ERROR, e, cancelFlag);
      } catch (HttpConnectionException e) {
        LogHelper.warning(e.getMessage());
        LogHelper.debug(e);
        setError(entry, HTTP_DOWNLOAD_ERROR, e, cancelFlag);
      } catch (HttpLoaderException e) {
        if (e instanceof HttpCancelledException)
          throw (HttpCancelledException) e;
        LogHelper.warning(e.getMessage());
        LogHelper.debug(e);
        setError(entry, DOWNLOAD_ERROR, e, cancelFlag);
      }
    } catch (HttpCancelledException e) {
      LogHelper.warning(e.getMessage());
      LogHelper.debug(e);
      setError(entry, CANCELLED_ERROR, e);
    } finally {
      if (tempFile != null) {
        try {
          FileUtil.deleteFile(tempFile, true);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
// ignore
        } catch (Exception e) {
          // ignore
          Log.warn("fin", e);
        }
      }
      try {
        entry.setProgressSource(null);
      } catch (Exception e) {
        // ignore
        Log.warn(e);
      }
      life.detach();
    }
    return false;
  }

  private Pair<String, String> loadFile(DownloadOwner owner, DetachComposite life, String argument, boolean retrying, boolean noninteractive, BasicScalarModel<Boolean> cancelFlag,
    Progress waiting, FileOutputStream output, StreamTransferTracker tracker)
    throws CannotCreateLoaderException, HttpLoaderException, IOException
  {
    String mimeType;
    String contentFilename;
    HttpResponseData loader = owner.load(life, argument, retrying, noninteractive, cancelFlag);
    waiting.setDone();
    //noinspection CatchGenericClass
    try {
      mimeType = Util.NN(loader.getContentType());
      contentFilename = decodeFilename(loader.getContentFilename());
      HttpUtils.transferToStream(loader, output, tracker);
    } catch (RuntimeException e) {
      // any exception may happen if we close connection concurrently
      if (Boolean.TRUE.equals(cancelFlag.getValue()))
        throw new HttpCancelledException();
      else
        throw e;
    } finally {
      IOUtils.closeStreamIgnoreExceptions(output);
      loader.releaseConnection();
    }
    return Pair.create(mimeType, contentFilename);
  }

  @Nullable
  private static String decodeFilename(@Nullable String filename) {
    if (filename == null) return filename;
    String processedName;
    try {
      processedName = MimeUtility.decodeText(filename);
      Log.debug("[DMI] received file name = \"" + filename + "\", decoded file name = \"" + processedName + '\"');
    } catch (UnsupportedEncodingException e) {
      Log.warn("[DMI] Cannot get decode file name \"" + filename + "\": " + e.getMessage());
      // Use suggested name
      return null;
    }
//    String processedName = processEncodedWordsRfc2047(name);
    return processedName;
  }

  private Pair<File, FileOutputStream> createTemporaryFile() throws IOException {
    File dir = myWorkArea.getDownloadDir();
    int c = 0;
    IOException lastException = null;
    int attempts = 10;
    for (int i = 0; i < attempts; i++) {
      c += (int) (System.currentTimeMillis() / 1000);
      String name = "__" + Integer.toHexString(c) + ".tmp";
      File file = new File(dir, name);
      if (!file.exists()) {
        try {
          FileOutputStream stream = new FileOutputStream(file);
          return Pair.create(file, stream);
        } catch (FileNotFoundException e) {
          lastException = new IOException("cannot create temporary file in " + dir);
        }
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
    throw lastException != null ? lastException : new IOException("cannot create temporary file in " + dir);
  }

  private File makeFile(String contentFilename, String suggestedName, String mimeType, File tempFile)
    throws IOException
  {
    File file = chooseTargetFile(contentFilename, suggestedName, mimeType);
    FileUtil.copyFile(tempFile, file);
    return file;
  }

  private File makeFile(String contentFilename, String suggestedName, String mimeType, byte[] data) throws IOException {
    File file = chooseTargetFile(contentFilename, suggestedName, mimeType);
    FileUtil.writeFile(file, data);
    return file;
  }

  private File chooseTargetFile(String contentFilename, String suggestedName, String mimeType) throws IOException {
    File dir = myWorkArea.getDownloadDir();
    File file = null;
    for (String s : filter(arrayList(contentFilename, suggestedName, "download"), Condition.<String>notNull())) {
      // strip any dir references
      String fileName = new File(s).getName();
      Pair<String, String> nameExt = FileUtil.getNameAndExtension(fileName, "", null);
      String extension = nameExt.getSecond();
      if (extension == null)
        extension = MimeExtensionMap.guessExtension(mimeType, "bin");
      
      file = FileUtil.createFileMaybeAdjusted(dir, nameExt.getFirst(), extension);
      if (file != null)
        return file;
    }
    throw new IOException("cannot create file in " + dir);
  }

  private void setError(final DownloadEntry entry, LText1<String> text, Exception e,
    BasicScalarModel<Boolean> cancelFlag) throws HttpCancelledException
  {
    if (cancelFlag != null && Boolean.TRUE == cancelFlag.getValue())
      throw new HttpCancelledException();
    setError(entry, text, e);
  }

  private void setError(final DownloadEntry entry, LText1<String> text, Exception e) {
    entry.setState(DownloadedFile.State.DOWNLOAD_ERROR);
    entry.setLastDownloadError(text.format(e.getLocalizedMessage()));
    entryChanged(entry);
  }

  private void entryChanged(DownloadEntry entry) {
    if (entry == null)
      return;
    entryChanged(entry.getKeyURL());
  }

  private void doSave() {
    if (!myLoaded.get()) {
      // retry later
      saveEntries();
      return;
    }
    PersistableHashMap<String, DownloadEntry> map = createPersister();
    synchronized (myLock) {
      map.set(myDownloads);
    }
    StoreUtils.storePersistable(myStore, "*", map);
  }

  private PersistableHashMap<String, DownloadEntry> createPersister() {
    return PersistableHashMap.create(new PersistableString(), new DownloadEntry.Persistable(myDownloadOwnerResolver));
  }

  private void doLoad() {
    PersistableHashMap<String, DownloadEntry> map = createPersister();
    if (StoreUtils.restorePersistable(myStore, "*", map)) {
      synchronized (myLock) {
        myDownloads.putAll(map.access());
      }
    }
    map.clear();
// clear state from entries that were downloading when the application terminated
    DownloadEntry[] entriesArray;
    synchronized (myLock) {
      Collection<DownloadEntry> entries = myDownloads.values();
      entriesArray = entries.toArray(new DownloadEntry[entries.size()]);
    }
    for (DownloadEntry entry : entriesArray) {
      DownloadedFile.State state = entry.getState();
      if (state == DownloadedFile.State.DOWNLOADING || state == DownloadedFile.State.QUEUED) {
        entry.setState(DownloadedFile.State.DOWNLOAD_ERROR);
        entry.setLastDownloadError(DOWNLOAD_INTERRUPTED.format());
      }
      checkEntryFile(entry);
    }
    myLoaded.set(true);
  }

  private void entryChanged(String keyURL) {
    FileDownloadListener dispatcher;
    DownloadEntry entry;
    synchronized (myLock) {
      FireEventSupport<FileDownloadListener> events = myListeners.get(keyURL);
      dispatcher = events == null ? null : events.getDispatcher();
      entry = myDownloads.get(keyURL);
    }
    if (dispatcher != null)
      dispatcher.onDownloadStatus(entry);
    saveEntries();
  }

  private void saveEntries() {
    if (mySaver != null)
      mySaver.request();
    else
      Log.warn(this + " has null mySaver");
  }

  private void ensureLoaded() {
    assert myLoaded.get();
    if (!myLoaded.get()) {
      // todo what to do?
      Log.debug("waiting while download manager is loading");
      try {
        myLoaded.waitForValue(true);
      } catch (InterruptedException e) {
        throw new RuntimeInterruptedException(e);
      }
    }
  }
}
