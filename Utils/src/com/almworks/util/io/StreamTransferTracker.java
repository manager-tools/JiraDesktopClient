package com.almworks.util.io;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

public interface StreamTransferTracker {
  /**
   * @param bytesTransferred total bytes that has been transferred
   *
   */
  void onTransfer(long bytesTransferred) throws IOException;

  void setLength(long length);

  class TrackStream extends DelegatingInputStream {
    private final StreamTransferTracker myTracker;
    private long myTotal = 0;

    public TrackStream(@NotNull InputStream stream, @NotNull StreamTransferTracker tracker) {
      super(stream);
      myTracker = tracker;
    }

    public static InputStream wrap(InputStream stream, @Nullable StreamTransferTracker tracker) {
      return tracker == null ? stream : new TrackStream(stream, tracker);
    }

    @Override
    protected void byteRead(int read) throws IOException {
      myTotal++;
      myTracker.onTransfer(myTotal);
    }

    @Override
    protected void arrayRead(byte[] b, int off, int read) throws IOException {
      myTotal += read;
      myTracker.onTransfer(myTotal);
    }
  }
}
