package com.almworks.api.download;

import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import org.almworks.util.detach.Detach;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public interface DownloadManager {
  Role<DownloadManager> ROLE = Role.role(DownloadManager.class);

  Detach addFileDownloadListener(String keyURL, ThreadGate gate, FileDownloadListener listener);

  void removeFileDownloadListener(String url, FileDownloadListener listener);

  /**
   * @param noninteractive if true, it is preferred not to ask user about anything. For example,
   * if the source site requires authentication and we don't have corresponding credentials, then silently fail
   */
  Detach initiateDownload(String keyURL, DownloadRequest request, boolean force, boolean noninteractive);

  Detach registerDownloadOwner(DownloadOwner owner);

  void cancelDownload(String keyURL);

  void checkDownloadedFile(String url);

  /**
   * Put into download cache the given file that is treated as if it was downloaded from
   * the given URL.
   *
   * long synchronous operation operation
   */
  DownloadedFile storeDownloadedFile(String attachmentURL, File file, String mimeType) throws IOException;

  DownloadedFile storeDownloadedFile(String attachmentURL, String filename, String mimeType, byte[] data) throws IOException;

  @NotNull
  DownloadedFile getDownloadStatus(String key);
}
