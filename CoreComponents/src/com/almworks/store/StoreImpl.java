package com.almworks.store;

import com.almworks.api.store.Store;
import com.almworks.api.store.StoreAccess;
import com.almworks.api.store.StoreFeature;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreImpl implements Store, StoreAccess {
  private static final int STORE_WRITE_ATTEMPTS = 5;

  private final Storer myStorer;
  private final String myPrefix;
  private final StoreFeature[] myFeatures;

  public StoreImpl(Storer storer, String prefix, StoreFeature[] features) {
    assert storer != null;
    assert prefix != null;
    assert features != null;
    myStorer = storer;
    myPrefix = prefix;
    myFeatures = features;
    checkFeatures(features);
  }

  public StoreImpl(Storer storer, String prefix) {
    this(storer, prefix, StoreFeature.PLAIN_STORE);
  }

  public String getPrefixPath() {
    return myPrefix;
  }

  public Store getSubStore(String path) {
    return new StoreImpl(myStorer, myPrefix + DELIMITER + path);
  }

  @NotNull
  public StoreAccess access(String path) {
    return new StoreImpl(myStorer, myPrefix + DELIMITER + path);
  }

  public StoreAccess access(String path, StoreFeature[] features) {
    checkFeatures(features);
    return new StoreImpl(myStorer, path, features);
  }

  public void store(byte[] data) {
    IOException lastException = null;
    for (int i = 0; i < STORE_WRITE_ATTEMPTS; i++) {
      try {
        if (i > 0) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
          }
        }
        myStorer.store(myPrefix, data, myFeatures);
        return;
      } catch (IOException e) {
        Log.debug(e);
        lastException = e;
      }
    }
    assert lastException != null;
    Log.warn("cannot store " + myPrefix);
  }

  public byte[] load() {
    try {
      return myStorer.load(myPrefix, myFeatures);
    } catch (IOException e) {
      assert false : e;
      Log.warn("cannot load " + myPrefix);
      return null;
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    }
  }

  public void clear() {
    try {
      myStorer.clear(myPrefix);
    } catch (IOException e) {
      Log.warn("cannot clear " + myPrefix);
    }
  }

  private void checkFeatures(StoreFeature[] features) {
    if (!myStorer.isSupported(features))
      throw new IllegalArgumentException("requested features are not supported");
  }
}

