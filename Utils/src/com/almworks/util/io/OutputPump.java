package com.almworks.util.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * :todoc:
 *
 * @author sereda
 */
public class OutputPump extends DataOutputStream implements BufferedDataOutput {
  private static final int DEFAULT_BUFFER_SIZE = 2048;
  private final FileChannel myChannel;
  private long myPosition = 0;

  public OutputPump(FileChannel channel, int bufferSize, long startingPosition) {
    super(new PumpOutputStream(bufferSize));
    if (channel == null)
      throw new NullPointerException("channel");
    if (startingPosition < 0)
      throw new IllegalArgumentException("startingPosition " + startingPosition);
    myChannel = channel;
    myPosition = startingPosition;
  }

  public OutputPump(FileChannel channel, int bufferSize) throws IOException {
    this(channel, bufferSize, channel.position());
  }

  public OutputPump(FileChannel channel) throws IOException {
    this(channel, DEFAULT_BUFFER_SIZE, channel.position());
  }

  public int getPosition() {
    return ((PumpOutputStream) out).getPosition();
  }

  public void setPosition(int position) {
    ((PumpOutputStream) out).setPosition(position);
  }

  public ByteBuffer getBuffer() {
    return ((PumpOutputStream) out).getBuffer();
  }

  public void flush() throws IOException {
    int written = myChannel.write(getBuffer(), myPosition);
    if (written > 0) {
      myPosition += written;
      ((PumpOutputStream) out).remove(written);
    }
  }

  public void unwrite(int count) {
    ((PumpOutputStream) out).unwrite(count);
  }

  private static class PumpOutputStream extends ByteArrayOutputStream {
    public PumpOutputStream(int bufferSize) {
      super(bufferSize);
    }

    public int getPosition() {
      return count;
    }

    public void setPosition(int position) {
      if (position < 0)
        throw new IllegalArgumentException("position " + position);
      if (position >= buf.length)
        throw new IllegalArgumentException("position is too high (" + position + ")");
      count = position;
    }

    public ByteBuffer getBuffer() {
      return ByteBuffer.wrap(buf, 0, count).asReadOnlyBuffer();
    }

    public void unwrite(int unwriteCount) {
      if (unwriteCount > count)
        throw new IllegalArgumentException("cannot unwrite more than was written (maybe flush intervened?)");
      count -= unwriteCount;
    }

    private void remove(int writtenCount) {
      if (writtenCount > count)
        throw new IllegalArgumentException("cannot remove more than was written");
      count = count - writtenCount;
      System.arraycopy(buf, writtenCount, buf, 0, count);
    }
  }
}
