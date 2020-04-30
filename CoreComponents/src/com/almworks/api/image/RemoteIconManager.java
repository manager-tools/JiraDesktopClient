package com.almworks.api.image;

import com.almworks.api.download.DownloadOwner;
import com.almworks.util.Pair;
import com.almworks.util.commons.Factory;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.MalformedURLException;

public interface RemoteIconManager {
  Role<RemoteIconManager> ROLE = Role.role("remoteIconManager");

  /**
   * Returns an icon instance that would display an icon residing on the given URL. If the icon is not in cache, it
   * will be downloaded. If it is in cache, it will be validated but not sooner than N hours since the last validation.
   *
   * The icon instance is usable immediately upon return. If the icon changes, a change event will be generated.
   *
   * @param url icon's url
   * @param downloadOwner see {@link com.almworks.api.download.DownloadManager}
   * @param downloadArgument see {@link com.almworks.api.download.DownloadManager}
   * @throws MalformedURLException if url is invalid
   * @return icon instance, immediately paintable
   */
  @NotNull
  @ThreadSafe
  Icon getRemoteIcon(String url, DownloadOwner downloadOwner, String downloadArgument)
    throws MalformedURLException, InterruptedException;

  @NotNull
  @ThreadSafe
  Icon getRemoteIcon(String url, ThreadGate factoryGate, Factory<Pair<DownloadOwner, String>> downloaderFactory)
    throws MalformedURLException, InterruptedException;

  @NotNull
  @ThreadSafe
  Icon getRemoteIcon(String url, ThreadGate downloadGate, ScalarModel<Pair<DownloadOwner, String>> downloader)
    throws MalformedURLException, InterruptedException;

  void addRemoteIconChangeListener(Lifespan lifespan, ThreadGate gate, RemoteIconChangeListener listener);

  void removeRemoteIconChangeListener(RemoteIconChangeListener listener);
}
