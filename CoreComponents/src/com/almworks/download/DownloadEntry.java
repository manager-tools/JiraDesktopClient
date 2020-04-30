package com.almworks.download;

import com.almworks.api.download.DownloadRequest;
import com.almworks.api.download.DownloadedFile;
import com.almworks.util.io.persist.FormatException;
import com.almworks.util.io.persist.LeafPersistable;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.Util;
import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.List;

class DownloadEntry implements DownloadedFile {
  private final String myKeyURL;
  private DownloadRequest myLastRequest;
  private final PropertyMap myProperties = new PropertyMap();
  private final Progress myProgress;

  public DownloadEntry(String keyURL) {
    assert keyURL != null;
    myKeyURL = keyURL;
    myProgress = Progress.delegator("D[" + keyURL + "]");
    myProperties.put(DownloadEntryKey.SIZE, -1L);
    myProperties.put(DownloadEntryKey.STATE, State.UNKNOWN);
  }

  private DownloadEntry(DownloadEntry entry) {
    assert entry != null;
    myKeyURL = entry.getKeyURL();
    myLastRequest = entry.getLastRequest();
    entry.myProperties.copyTo(myProperties);
    myProgress = new Progress("D[" + myKeyURL + "]");
  }

  public ProgressSource getDownloadProgressSource() {
    return myProgress;
  }

  public synchronized File getFile() {
    return myProperties.get(DownloadEntryKey.FILE);
  }

  public String getKeyURL() {
    return myKeyURL;
  }

  public synchronized String getLastDownloadError() {
    return myProperties.get(DownloadEntryKey.LAST_DOWNLOAD_ERROR);
  }

  public synchronized long getSize() {
    Long size = myProperties.get(DownloadEntryKey.SIZE);
    return size == null ? -1 : size.longValue();
  }

  public synchronized State getState() {
    State state = myProperties.get(DownloadEntryKey.STATE);
    return state == null ? State.UNKNOWN : state;
  }

  public synchronized String getMimeType() {
    return myProperties.get(DownloadEntryKey.MIME_TYPE);
  }

  public synchronized long getLastModifed() {
    Long v = myProperties.get(DownloadEntryKey.LAST_MODIFIED);
    return v == null ? 0 : v;
  }

  public synchronized void updateRequest(DownloadRequest request) {
    myLastRequest = request;
  }

  public synchronized DownloadRequest getLastRequest() {
    return myLastRequest;
  }

  synchronized void setState(State state) {
    myProperties.put(DownloadEntryKey.STATE, Util.NN(state, State.UNKNOWN));
  }

  synchronized void setLastDownloadError(String error) {
    myProperties.put(DownloadEntryKey.LAST_DOWNLOAD_ERROR, error);
  }

  synchronized void setFile(File file) {
    myProperties.put(DownloadEntryKey.FILE, file);
  }

  synchronized void setMimeType(String mimeType) {
    myProperties.put(DownloadEntryKey.MIME_TYPE, mimeType);
  }

  synchronized public void setSize(long size) {
    myProperties.put(DownloadEntryKey.SIZE, size);
  }

  synchronized public void setLastModified(long lastModified) {
    myProperties.put(DownloadEntryKey.LAST_MODIFIED, lastModified);
  }

  synchronized public void setProgressSource(ProgressSource source) {
    if (source != null)
      myProgress.delegate(source);
    else
      myProgress.setDone();
  }

  public static class Persistable extends LeafPersistable<DownloadEntry> {
    private DownloadEntry myEntry;
    private final DownloadOwnerResolver myResolver;

    public Persistable(DownloadOwnerResolver resolver) {
      myResolver = resolver;
    }

    protected void doClear() {
      myEntry = null;
    }

    protected DownloadEntry doAccess() {
      return myEntry;
    }

    protected DownloadEntry doCopy() {
      return new DownloadEntry(myEntry);
    }

    protected void doRestore(DataInput in) throws IOException, FormatException {
      myEntry = null;
      String keyURL = CompactChar.readString(in);
      if (keyURL == null) {
        // null entry
      } else {
        DownloadEntry entry = new DownloadEntry(keyURL);
        synchronized (entry) {
          entry.myLastRequest = DownloadRequestUtils.restoreRequest(in, myResolver);
          synchronized (DownloadEntryKey.class) {
            List<DownloadEntryKey> keys = DownloadEntryKey.all();
            for (DownloadEntryKey key : keys) {
              LeafPersistable persistable = key.getPersistable();
              persistable.restore(in);
              entry.myProperties.put(key, persistable.access());
            }
          }
        }
        myEntry = entry;
      }
    }

    protected void doSet(DownloadEntry downloadEntry) {
      myEntry = downloadEntry;
    }

    protected void doStore(DataOutput out) throws IOException {
      DownloadEntry entry = myEntry;
      if (entry == null) {
        CompactChar.writeString(out, null);
        return;
      }
      synchronized (entry) {
        CompactChar.writeString(out, entry.getKeyURL());
        DownloadRequestUtils.storeRequest(out, entry.getLastRequest());
        synchronized (DownloadEntryKey.class) {
          List<DownloadEntryKey> keys = DownloadEntryKey.all();
          for (DownloadEntryKey key : keys) {
            Object object = entry.myProperties.get(key);
            LeafPersistable persistable = key.getPersistable();
            persistable.set(object);
            persistable.store(out);
          }
        }
      }
    }
  }
}
