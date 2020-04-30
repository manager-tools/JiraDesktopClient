package com.almworks.download;

import com.almworks.api.download.DownloadOwner;

public interface DownloadOwnerResolver {
  DownloadOwner getOwner(String ownerId);
}
