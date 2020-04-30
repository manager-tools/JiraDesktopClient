package com.almworks.download;

import com.almworks.api.download.DownloadOwner;
import com.almworks.api.download.DownloadRequest;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class DownloadRequestUtils {
  public static DownloadRequest restoreRequest(DataInput in, DownloadOwnerResolver ownerResolver) throws IOException {
    String ownerId = CompactChar.readString(in);
    if (ownerId == null)
      return null;
    long expectedSize = CompactInt.readLong(in);
    String argument = CompactChar.readString(in);
    String suggestedFilename = CompactChar.readString(in);

    DownloadOwner owner = ownerResolver.getOwner(ownerId);
    return new DownloadRequest(owner, argument, suggestedFilename, expectedSize);
  }

  public static void storeRequest(DataOutput out, DownloadRequest request) throws IOException {
    if (request == null) {
      CompactChar.writeString(out, null);
      return;
    }
    DownloadOwner owner = request.getOwner();
    CompactChar.writeString(out, owner.getDownloadOwnerID());
    CompactInt.writeLong(out, request.getExpectedSize());
    CompactChar.writeString(out, request.getArgument());
    CompactChar.writeString(out, request.getSuggestedFilename());
  }
}
