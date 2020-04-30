package com.almworks.image;

import com.almworks.api.download.*;
import com.almworks.api.image.RemoteIconChangeListener;
import com.almworks.api.image.RemoteIconManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.store.Store;
import com.almworks.api.store.StoreUtils;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.LogHelper;
import com.almworks.util.Pair;
import com.almworks.util.commons.Factory;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.images.ImageUtil;
import com.almworks.util.images.SvgWrapper;
import com.almworks.util.io.persist.Persistable;
import com.almworks.util.io.persist.PersistableFile;
import com.almworks.util.io.persist.PersistableHashMap;
import com.almworks.util.io.persist.PersistableString;
import com.almworks.util.model.LightScalarModel;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.threads.CanBlock;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Startable;
import sun.awt.image.ToolkitImage;
import util.concurrent.SynchronizedBoolean;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RemoteIconManagerImpl implements RemoteIconManager, FileDownloadListener, Startable {
  private static final boolean ALLOW_ICON_AUTH = Env.getBoolean(GlobalProperties.ALLOW_ICON_AUTH);

  private static final String ICONS_SUBDIR = ".icons";
  private static final int NAME_ATTEMPTS = 50;
  private static final String FILES_STORE = "files";
  private static final String MIMES_STORE = "mimes";
  private static final int PREFIX_STEP = 20;

  private final WorkArea myWorkArea;
  private final DownloadManager myDownloadManager;
  private final Store myStore;

  private final Map<String, RemoteIconImpl> myMap = Collections15.hashMap();
  private final Object myLock = new Object();
  private final SynchronizedBoolean myStarted = new SynchronizedBoolean(false);
  private final Lifecycle myLife = new Lifecycle();

  private final FireEventSupport<RemoteIconChangeListener> myEventSupport =
    FireEventSupport.createSynchronized(RemoteIconChangeListener.class);

  private final Bottleneck mySaveBottleneck = new Bottleneck(1000, ThreadGate.LONG, new Runnable() {
    public void run() {
      saveMap();
    }
  });

  public RemoteIconManagerImpl(WorkArea workArea, DownloadManager downloadManager, Store store) {
    myWorkArea = workArea;
    myDownloadManager = downloadManager;
    myStore = store;
  }

  @NotNull
  public Icon getRemoteIcon(String url, DownloadOwner downloadOwner, String downloadArgument)
    throws MalformedURLException, InterruptedException
  {
    Pair<DownloadOwner, String> pair = Pair.create(downloadOwner, downloadArgument);
    return getRemoteIcon(url, ThreadGate.STRAIGHT, new Factory.Const<Pair<DownloadOwner, String>>(pair));
  }

  @NotNull
  @ThreadSafe
  public Icon getRemoteIcon(String url, ThreadGate factoryGate,
    final Factory<Pair<DownloadOwner, String>> downloaderFactory) throws MalformedURLException, InterruptedException
  {
    final LightScalarModel<Pair<DownloadOwner, String>> downloader = LightScalarModel.create();
    Icon remoteIcon = getRemoteIcon(url, factoryGate, downloader);
    factoryGate.execute(new Runnable() {
      @Override
      public void run() {
        downloader.setValue(downloaderFactory.create());
      }
    });
    return remoteIcon;
  }

  @Override
  public Icon getRemoteIcon(String url, ThreadGate downloadGate, ScalarModel<Pair<DownloadOwner, String>> downloader) throws MalformedURLException, InterruptedException {
    myStarted.waitForValue(true);
    RemoteIconImpl icon;
    synchronized (myLock) {
      icon = myMap.get(url);
      if (icon == null) {
        icon = new RemoteIconImpl(url);
        myMap.put(url, icon);
      }
    }
    if (icon.markForDownload()) {
      scheduleDownload(url, downloadGate, downloader, icon);
    }
    return icon;
  }

  private void scheduleDownload(String url, ThreadGate downloadGate, ScalarModel<Pair<DownloadOwner, String>> downloader, final RemoteIconImpl icon) throws MalformedURLException {
    // verify url
    new URL(url);
    final Lifecycle once = new Lifecycle();
    myLife.lifespan().add(once.getDisposeDetach());
    downloader.getEventSource().addListener(once.lifespan(), downloadGate, new ScalarModel.Adapter<Pair<DownloadOwner, String>>() {
      AtomicBoolean myStartedDownload = new AtomicBoolean(false);
      @Override
      public void onScalarChanged(ScalarModelEvent<Pair<DownloadOwner, String>> event) {
        Pair<DownloadOwner, String> pair = event.getNewValue();
        if (pair != null && myStartedDownload.compareAndSet(false, true)) {
          once.dispose();
          try {
            initiateDownload(icon, pair.getFirst(), pair.getSecond());
          } catch (MalformedURLException e) {
            Log.error(e);
          }
        }
      }
    });
  }

  private void initiateDownload(RemoteIconImpl icon, DownloadOwner downloadOwner, String downloadArgument)
    throws MalformedURLException
  {
    String stringUrl = icon.getUrl();
    URL url = new URL(stringUrl);
    String path = url.getPath();
    int k = path.lastIndexOf('/');
    if (k >= 0)
      path = path.substring(k + 1);
    DownloadRequest request = new DownloadRequest(downloadOwner, downloadArgument, path, -1);
    myDownloadManager.initiateDownload(stringUrl, request, false, !ALLOW_ICON_AUTH);
    myDownloadManager.addFileDownloadListener(stringUrl, ThreadGate.LONG, this);
  }

  public void onDownloadStatus(DownloadedFile dfile) {
    String url = dfile.getKeyURL();
    RemoteIconImpl icon;
    synchronized (myLock) {
      icon = myMap.get(url);
    }
    boolean listenFurther = false;
    if (icon != null) {
      DownloadedFile.State state = dfile.getState();
      if (state == DownloadedFile.State.READY) {
        onFileDownloaded(icon, dfile);
      } else if (state == DownloadedFile.State.DOWNLOAD_ERROR) {
        icon.setBroken("download problem: " + dfile.getLastDownloadError());
      } else {
        listenFurther = true;
      }
    }

    if (!listenFurther) {
      fireChanged(url, icon);
      myDownloadManager.removeFileDownloadListener(url, this);
    }
  }

  private void fireChanged(String url, RemoteIconImpl icon) {
    mySaveBottleneck.request();
    myEventSupport.getDispatcher().onRemoteIconChanged(url, icon);
  }

  private void onFileDownloaded(RemoteIconImpl icon, DownloadedFile dfile) {
    File file = dfile.getFile();
    String mimeType = dfile.getMimeType();
    if (!ImageUtil.isImageMimeType(mimeType)) {
      icon.setBroken("not an image");
    } else {
      try {
        File targetFile = getTargetFile(file);
        FileUtil.copyFile(file, targetFile, true);
        try {
          FileUtil.deleteFile(file, true);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
        Image image = loadImage(targetFile, mimeType);
        if (image == null) {
          icon.setBroken("cannot read image from " + targetFile);
        } else {
          icon.setData(targetFile, mimeType, image);
        }
      } catch (IOException e) {
        icon.setBroken("IO error: " + e);
      }
    }
  }

  private File getTargetFile(File sourceFile) throws IOException {
    File dir = new File(myWorkArea.getDownloadDir(), ICONS_SUBDIR);
    if (!dir.exists()) {
      boolean success = dir.mkdir();
      if (!success) {
        throw new IOException("cannot create directory " + dir);
      }
    } else {
      if (!dir.isDirectory()) {
        throw new IOException(dir + " is not a directory");
      }
    }

    String name = sourceFile.getName();
    File targetFile = new File(dir, name);
    if (!targetFile.exists())
      return targetFile;

    int prefix = new Random().nextInt(PREFIX_STEP);
    for (int i = 0; i < NAME_ATTEMPTS; i++) {
      targetFile = new File(dir, prefix + "_" + name);
      if (!targetFile.exists())
        return targetFile;
      prefix += PREFIX_STEP;
    }

    throw new IOException("cannot create target file for " + sourceFile);
  }

  public void addRemoteIconChangeListener(Lifespan lifespan, ThreadGate gate, RemoteIconChangeListener listener) {
    myEventSupport.addListener(lifespan, gate, listener);
  }

  public void removeRemoteIconChangeListener(RemoteIconChangeListener listener) {
    myEventSupport.removeListener(listener);
  }


  public void start() {
    myLife.cycleStart();
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        try {
          loadMap();
        } finally {
          myStarted.set(true);
        }
      }
    });
  }

  public void stop() {
    myLife.cycleEnd();
  }

  @CanBlock
  private void saveMap() {
    Map<String, File> files = Collections15.hashMap();
    Map<String, String> mimes = Collections15.hashMap();
    synchronized (myLock) {
      for (Map.Entry<String, RemoteIconImpl> entry : myMap.entrySet()) {
        String url = entry.getKey();
        RemoteIconImpl icon = entry.getValue();
        files.put(url, icon.getFile());
        mimes.put(url, icon.getMimeType());
      }
    }
    Persistable<Map<String, File>> filePersister =
      PersistableHashMap.create(new PersistableString(), new PersistableFile());
    filePersister.set(files);
    StoreUtils.storePersistable(myStore, FILES_STORE, filePersister);
    Persistable<Map<String, String>> mimePersister =
      PersistableHashMap.create(new PersistableString(), new PersistableString());
    mimePersister.set(mimes);
    StoreUtils.storePersistable(myStore, MIMES_STORE, mimePersister);
  }


  @CanBlock
  private void loadMap() {
    if (myStarted.get()) {
      assert false : this;
      return;
    }
    Persistable<Map<String, File>> filePersister =
      PersistableHashMap.create(new PersistableString(), new PersistableFile());
    if (!StoreUtils.restorePersistable(myStore, FILES_STORE, filePersister))
      return;
    Map<String, File> files = filePersister.access();
    Persistable<Map<String, String>> mimePersister =
      PersistableHashMap.create(new PersistableString(), new PersistableString());
    if (!StoreUtils.restorePersistable(myStore, MIMES_STORE, mimePersister))
      return;

    final List<LoadedImage> loadedImages = Collections15.arrayList();
    Map<String, String> mimes = mimePersister.access();
    for (Map.Entry<String, File> entry : files.entrySet()) {
      String url = entry.getKey();
      File file = entry.getValue();
      String mime = mimes.get(url);
      if (file == null || mime == null)
        continue;
      Image image = loadImage(file, mime);
      if (image == null)
        continue;
      loadedImages.add(new LoadedImage(url, file, mime, image));
    }

    addReady(loadedImages);
    if (loadedImages.isEmpty()) return;
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        int iteration = 1;
        while (iteration < 10) {
          addReady(loadedImages);
          if (loadedImages.isEmpty())
            return;
          if (iteration < 3) LogHelper.warning("ConnectionIcons: Not all images loaded.", iteration, loadedImages.size());
          else LogHelper.warning("ConnectionIcons: Not all images loaded.", iteration, loadedImages);
          iteration++;
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            LogHelper.warning("ConnectionIcons: Terminated", iteration, loadedImages);
          }
        }
      }
    }, "ConnectionIcons:waitLoad");
    thread.setDaemon(true);
    thread.start();
  }

  private Image loadImage(File file, String mime) {
    if (SvgWrapper.isSVG(mime)) {
      try {
        SvgWrapper svg = SvgWrapper.load(file);
        return svg.renderImage(RemoteIconImpl.DEFAULT_WIDTH, RemoteIconImpl.DEFAULT_HEIGHT);
      } catch (IOException e) {
        LogHelper.debug("Failed to load SVG", file, e);
        return null;
      }
    }
    return ImageUtil.loadImageFromFile(file, mime);
  }

  private void addReady(List<LoadedImage> loadedImages) {
    for (int i = loadedImages.size() - 1; i >= 0; i--) {
      LoadedImage image = loadedImages.get(i);
      if (image.isReady()) {
        synchronized (myLock) {
          image.putTo(myMap);
        }
        loadedImages.remove(i);
      } else if (image.isFailed()) {
        LogHelper.warning("Deleting bad image", image);
        loadedImages.remove(i);
        image.delete();
      }
    }
  }

  private static class LoadedImage {
    private final String myUrl;
    private final File myFile;
    private final String myMime;
    private final Image myImage;

    public LoadedImage(String url, File file, String mime, Image image) {
      myUrl = url;
      myFile = file;
      myMime = mime;
      myImage = image;
    }

    public void putTo(Map<String, RemoteIconImpl> map) {
      if (map.containsKey(myUrl)) return;
      RemoteIconImpl icon = new RemoteIconImpl(myUrl, myFile, myMime, myImage);
      map.put(myUrl, icon);
    }

    public void delete() {
      myFile.delete();
    }

    public boolean isReady() {
      return myImage.getWidth(null) > 0 || myImage.getHeight(null) > 0;
    }

    public boolean isFailed() {
      ToolkitImage toolkitImage = Util.castNullable(ToolkitImage.class, myImage);
      return toolkitImage != null && toolkitImage.hasError();
    }

    @Override
    public String toString() {
      return "LoadedImage[file=" + myFile + ", source=" + myUrl + "]";
    }
  }
}
