package com.almworks.util.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class InputPump extends DataInputStream implements BufferedDataInput {
  private static final int DEFAULT_BUFFER_SIZE = 2048;

  public InputPump(FileChannel channel, int bufferSize, long startingPosition) {
    super(new PumpInputStream(channel, bufferSize, startingPosition));
  }

  public InputPump(FileChannel channel, int bufferSize) {
    super(new PumpInputStream(channel, bufferSize, 0));
  }

  public InputPump(FileChannel channel) {
    super(new PumpInputStream(channel, DEFAULT_BUFFER_SIZE, 0));
  }

  public int getPosition() {
    return ((PumpInputStream) in).getPosition();
  }

  public ByteBuffer getBuffer() {
    return ((PumpInputStream) in).getBuffer();
  }

  public void discard() {
    ((PumpInputStream) in).discard();
  }

  public void unread(int count) {
    ((PumpInputStream) in).unread(count);
  }

  public long getCarrierFilePosition() {
    return ((PumpInputStream) in).getCarrierFilePosition();
  }


  private static class PumpInputStream extends InputStream {
    private final FileChannel myChannel;
    private byte[] myArray;
    private ByteBuffer myBuffer;
    private long myChannelPosition;
    private int myPosition;
    private int myCount;
    private int myDiscarded;
    private long myBufferCarrierPosition;

    public PumpInputStream(FileChannel channel, int bufferSize, long startingPosition) {
      if (channel == null)
        throw new NullPointerException("channel");
      if (bufferSize < 1)
        throw new IllegalArgumentException("bufferSize " + bufferSize);
      myChannel = channel;
      myArray = new byte[bufferSize];
      myPosition = 0;
      myCount = 0;
      myDiscarded = 0;
      myBuffer = ByteBuffer.wrap(myArray);
      myChannelPosition = startingPosition;
    }

    public synchronized int read(byte buffer[], int offset, int length) throws IOException {
      if (buffer == null)
        throw new NullPointerException();
      if ((offset < 0) || (offset > buffer.length) || (length < 0) || ((offset + length) > buffer.length) ||
        ((offset + length) < 0)) {

        throw new IndexOutOfBoundsException();
      }

      if (myPosition == myCount)
        pump();
      if (myCount == -1)
        return -1;
      if (myPosition + length > myCount) {
        length = myCount - myPosition;
      }
      if (length <= 0) {
        return 0;
      }
      System.arraycopy(myArray, myPosition, buffer, offset, length);
      myPosition += length;
      return length;
    }

    public synchronized long skip(long n) throws IOException {
      if (myPosition == myCount)
        pump();
      if (myCount == -1)
        return 0;
      if (myPosition + n > myCount) {
        n = myCount - myPosition;
      }
      if (n < 0) {
        return 0;
      }
      myPosition += n;
      return n;
    }

    public synchronized int available() throws IOException {
      if (myCount == myPosition)
        pump();
      if (myCount == -1)
        return 0;
      return myCount - myPosition;
    }

    public void close() throws IOException {
      myCount = -1;
    }

    public synchronized int read() throws IOException {
      if (myPosition == myCount)
        pump();
      return myCount == -1 ? -1 : (myArray[myPosition++] & 0xff);
    }

    public boolean markSupported() {
      return false;
    }

    public synchronized void reset() {
      throw new UnsupportedOperationException();
    }

    public void mark(int readAheadLimit) {
      throw new UnsupportedOperationException();
    }

    private void pump() throws IOException {
      assert myCount == myPosition : this;
      if (myDiscarded > 0) {
        assert myDiscarded <= myCount : this;
        int length = myCount - myDiscarded;
        if (length > 0) {
          System.arraycopy(myArray, myDiscarded, myArray, 0, length);
        }
        myDiscarded = 0;
        myCount = length;
        myPosition = length;
        myBuffer.position(myCount);
      }
      if (myPosition == myArray.length) {
        assert myBuffer.position() == myBuffer.capacity() : myBuffer.position() + " " + myBuffer.capacity();
        byte[] newArray = new byte[myArray.length * 2 + 1];
        System.arraycopy(myArray, 0, newArray, 0, myArray.length);
        ByteBuffer newBuffer = ByteBuffer.wrap(newArray);
        newBuffer.position(myCount);
        myBuffer = newBuffer;
        myArray = newArray;
      }
      myBufferCarrierPosition = myChannelPosition - myBuffer.position();
      int read = myChannel.read(myBuffer, myChannelPosition);
      if (read > 0) {
        myChannelPosition += read;
        myCount += read;
      } else {
        if (read < 0)
          myCount = -1;
      }
    }

    public void discard() {
      if (myPosition < myCount && myPosition > 0) {
        assert myPosition >= myDiscarded : this;
        myDiscarded = myPosition;
      }
    }

    public int getPosition() {
      return myPosition - myDiscarded;
    }

    public ByteBuffer getBuffer() {
      assert myDiscarded <= myPosition : this;
      return ByteBuffer.wrap(myArray, myDiscarded, myPosition - myDiscarded);
    }

    public void unread(int count) {
      if (myPosition - count < myDiscarded)
        throw new IllegalArgumentException("count " + count);
      myPosition -= count;
    }

    public String toString() {
      return "PumpInputStream[" + myCount + "," + myPosition + "," + myDiscarded + "]";
    }

    public long getCarrierFilePosition() {
      return myBufferCarrierPosition + myPosition;
    }
  }
}
