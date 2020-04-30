package com.almworks.util.bmp;

import java.io.*;

public class LEDataInputStream extends FilterInputStream implements DataInput {
  private final DataInputStream myStream;

  public LEDataInputStream(InputStream in) {
    super(in);
    myStream = new DataInputStream(in);
  }

  public void close() throws IOException {
    myStream.close();
  }

  public synchronized final int read(byte b[]) throws IOException {
    return myStream.read(b, 0, b.length);
  }

  public synchronized final int read(byte b[], int off, int len) throws IOException {
    int rl = myStream.read(b, off, len);
    return rl;
  }

  public final void readFully(byte b[]) throws IOException {
    myStream.readFully(b, 0, b.length);
  }

  public final void readFully(byte b[], int off, int len) throws IOException {
    myStream.readFully(b, off, len);
  }

  public final int skipBytes(int n) throws IOException {
    return myStream.skipBytes(n);
  }

  public final boolean readBoolean() throws IOException {
    int ch = myStream.read();
    if (ch < 0)
      throw new EOFException();
    return (ch != 0);
  }

  public final byte readByte() throws IOException {
    int ch = myStream.read();
    if (ch < 0)
      throw new EOFException();
    return (byte) (ch);
  }

  public final int readUnsignedByte() throws IOException {
    int ch = myStream.read();
    if (ch < 0)
      throw new EOFException();
    return ch;
  }

  public final short readShort() throws IOException {
    int ch1 = myStream.read();
    int ch2 = myStream.read();
    if ((ch1 | ch2) < 0)
      throw new EOFException();
    return (short) ((ch1 << 0) + (ch2 << 8));
  }

  public final int readUnsignedShort() throws IOException {
    int ch1 = myStream.read();
    int ch2 = myStream.read();
    if ((ch1 | ch2) < 0)
      throw new EOFException();
    return (ch1 << 0) + (ch2 << 8);
  }

  public final char readChar() throws IOException {
    int ch1 = myStream.read();
    int ch2 = myStream.read();
    if ((ch1 | ch2) < 0)
      throw new EOFException();
    return (char) ((ch1 << 0) + (ch2 << 8));
  }

  public final int readInt() throws IOException {
    int ch1 = myStream.read();
    int ch2 = myStream.read();
    int ch3 = myStream.read();
    int ch4 = myStream.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0)
      throw new EOFException();
    return ((ch1 << 0) + (ch2 << 8) + (ch3 << 16) + (ch4 << 24));
  }

  public final long readLong() throws IOException {
    int i1 = readInt();
    int i2 = readInt();
    return ((long) (i1) & 0xFFFFFFFFL) + ((long)i2 << 32);
  }

  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  public final String readLine() throws IOException {
    throw new UnsupportedOperationException();
  }

  public final String readUTF() throws IOException {
    throw new UnsupportedOperationException();
  }
}

