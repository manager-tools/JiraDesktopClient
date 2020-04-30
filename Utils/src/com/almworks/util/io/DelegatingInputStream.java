package com.almworks.util.io;

import com.almworks.util.LogHelper;

import java.io.IOException;
import java.io.InputStream;

public abstract class DelegatingInputStream extends InputStream {
  private final InputStream myStream;

  public DelegatingInputStream(InputStream stream) {
    if (stream == null) LogHelper.error("Delegating to null stream");
    myStream = stream;
  }

  @Override
  public int read() throws IOException {
    int read = myStream.read();
    if (read >= 0) byteRead(read);
    return read;
  }

  protected abstract void byteRead(int read) throws IOException;

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int read = myStream.read(b, off, len);
    if (read > 0) arrayRead(b, off, read);
    return read;
  }

  protected abstract void arrayRead(byte[] b, int off, int read) throws IOException;

  @Override
  public int available() throws IOException {
    return myStream.available();
  }

  @Override
  public synchronized void reset() throws IOException {
    myStream.reset();
  }

  @Override
  public synchronized void mark(int readlimit) {
    myStream.mark(readlimit);
  }

  @Override
  public boolean markSupported() {
    return myStream.markSupported();
  }

  @Override
  public void close() throws IOException {
    myStream.close();
  }
}
