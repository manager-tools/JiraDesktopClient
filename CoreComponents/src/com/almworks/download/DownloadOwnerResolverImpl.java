package com.almworks.download;

import com.almworks.api.download.CannotCreateLoaderException;
import com.almworks.api.download.DownloadOwner;
import com.almworks.api.http.HttpLoaderException;
import com.almworks.api.http.HttpResponseData;
import com.almworks.util.model.BasicScalarModel;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import java.io.IOException;
import java.util.Map;

public class DownloadOwnerResolverImpl implements DownloadOwnerResolver {
  private final Map<String, DownloadOwner> myMap = Collections15.hashMap();

  public DownloadOwner getOwner(String ownerId) {
    DownloadOwner owner = getOwnerImpl(ownerId);
    if (owner == null)
      owner = new DelegatingOwner(ownerId);
    return owner;
  }

  private synchronized DownloadOwner getOwnerImpl(String ownerId) {
    return myMap.get(ownerId);
  }

  public synchronized Detach register(final DownloadOwner owner) {
    assert owner != null;
    final String id = owner.getDownloadOwnerID();
    DownloadOwner existing = myMap.get(id);
    if (owner.equals(existing))
      return Detach.NOTHING;
    if (existing != null) {
      assert false : existing + " " + owner;
      return Detach.NOTHING;
    }
    myMap.put(id, owner);
    return new Detach() {
      protected void doDetach() {
        synchronized (DownloadOwnerResolverImpl.this) {
          DownloadOwner removed = myMap.remove(id);
          if (!owner.equals(removed)) {
            assert false : owner + " " + removed;
            myMap.put(id, removed);
          }
        }
      }
    };
  }

  private class DelegatingOwner implements DownloadOwner {
    private final String myOwnerId;

    public DelegatingOwner(String ownerId) {
      myOwnerId = ownerId;
    }

    private DownloadOwner getDelegate() {
      DownloadOwner owner = getOwnerImpl(myOwnerId);
      return owner != null ? owner : DisabledOwner.INSTANCE;
    }

    public String getDownloadOwnerID() {
      return getDelegate().getDownloadOwnerID();
    }

    public boolean isValid() {
      return getDelegate().isValid();
    }

    @Override
    public HttpResponseData load(DetachComposite life, String argument, boolean retrying, boolean noninteractive, BasicScalarModel<Boolean> cancelFlag)
      throws CannotCreateLoaderException, IOException, HttpLoaderException {
      return getDelegate().load(life, argument, retrying, noninteractive, cancelFlag);
    }
  }

  public static class DisabledOwner implements DownloadOwner {
    public static final DisabledOwner INSTANCE = new DisabledOwner();

    public String getDownloadOwnerID() {
      return "no-owner";
    }

    @Override
    public HttpResponseData load(DetachComposite life, String argument, boolean retrying, boolean noninteractive,
      BasicScalarModel<Boolean> cancelFlag) throws CannotCreateLoaderException {
      throw new CannotCreateLoaderException();
    }

    public boolean isValid() {
      return false;
    }
  }
}
