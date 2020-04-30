package com.almworks.util.collections;

import com.almworks.util.LogHelper;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * @author Dyoma
 */
public class ByteArray {
  private byte[] myBytes;
  private int mySize = 0;

  public ByteArray() {
    this(10);
  }

  public ByteArray(int size) {
    this(new byte[size], 0);
  }

  public ByteArray(byte[] bytes, int size) {
    myBytes = bytes;
    mySize = size;
  }

  public static ByteArray wrap(byte[] bytes) {
    return new ByteArray(bytes, bytes.length);
  }

  /**
   *
   * @param stream an {@link InputStream} to read from
   * @param count maximum number of bytes to read
   * @return See {@link InputStream#read(byte[], int, int)}
   * @throws IOException
   */
  public int readFrom(InputStream stream, int count) throws IOException {
    ensureCapacity(mySize + count);
    int readBytes = stream.read(myBytes, mySize, count);
    if (readBytes > 0) mySize += readBytes;
    return readBytes;
  }

  public void readAllFromStream(InputStream stream) throws IOException {
    //noinspection StatementWithEmptyBody
    while (readFrom(stream, Math.max(1, stream.available())) >= 0);
  }

  public byte[] toNativeArray() {
    return getBytes(0, mySize);
  }

  public int size() {
    return mySize;
  }

  public void add(byte[] bytes) {
    ensureCapacity(mySize + bytes.length);
    setBytes(mySize, bytes, 0, bytes.length);
  }

  public void addLong(long value) {
    setLong(mySize, value);
  }

  public void addInt(int value) {
    setInt(mySize, value);
  }

  public void addByte(byte value) {
    setByte(mySize, value);
  }

  public void addBoolean(boolean value) {
    if (value) addByte((byte) 1);
    else addByte((byte) 0);
  }

  private void ensureCapacity(int expectedSize) {
    if (expectedSize <= myBytes.length) return;
    byte[] newBytes = new byte[Math.max(expectedSize, myBytes.length*2)];
    System.arraycopy(myBytes, 0, newBytes, 0, mySize);
    myBytes = newBytes;
  }

  public void copyBytes(int position, byte[] dest, int offset, int length) {
    System.arraycopy(myBytes, position, dest, offset, length);
  }

  public void setBytes(int pos, byte[] bytes, int offset, int length) {
    ensureCapacity(pos + length);
    System.arraycopy(bytes, offset, myBytes, pos, length);
    mySize = Math.max(mySize, pos + length);
  }

  public void setSize(int size) {
    if (mySize < size)
      ensureCapacity(size);
    mySize = size;
  }

  public long getLong(int offset) {
    if (offset + 8> mySize)
      throw new IndexOutOfBoundsException(offset + " " + mySize);
    return getLong(myBytes, offset);
  }

  public static long getLong(byte[] bytes, int offset) {
    long r = 0;
    for (int i = 0; i < 8; i++) {
      r |= (((long) bytes[offset + i]) & 0xFF) << i*8;
    }
    return r;
  }

  public int getInt(int offset) {
    if (offset + 4 > mySize)
      throw new IndexOutOfBoundsException(offset + " " + mySize);
    return getInt(myBytes, offset);
  }

  public static int getInt(byte[] bytes, int offset) {
    int r = 0;
    for (int i = 0; i < 4; i++)
      r |=  (((int) bytes[offset + i]) & 0xFF) << i*8;
    return r;
  }

  public byte[] getBytes(int offset, int length) {
    byte[] bytes = new byte[length];
    getBytes(offset, bytes, length);
    return bytes;
  }

  public void getBytes(int offset, byte[] bytes, int length) {
    if (offset  + length > mySize)
      throw new IndexOutOfBoundsException(offset + " " + mySize);
    System.arraycopy(myBytes, offset, bytes, 0, length);
  }

  public void getBytes(int offset, byte[] bytes, int arrayOffset, int count) {
    if (offset + count > mySize)
      throw new IndexOutOfBoundsException(offset + " " + mySize);
    System.arraycopy(myBytes, offset, bytes, arrayOffset, count);
  }

  public void setInt(int offset, int value) {
    ensureCapacity(offset + 4);
    setInt(value, myBytes, offset);
    mySize = Math.max(mySize, offset + 4);
  }

  public void setByte(int offset, byte value) {
    ensureCapacity(offset + 1);
    myBytes[offset] = value;
    mySize = Math.max(mySize, offset + 1);
  }

  public static void setInt(int value, byte[] bytes, int offset) {
    int mask = 0xFF;
    for (int i = 0; i < 4; i++) {
      bytes[offset + i] = (byte) ((value & mask) >> i * 8);
      mask = mask << 8;
    }
  }

  public void setLong(int offset, long value) {
    ensureCapacity(offset + 8);
    byte[] bytes = myBytes;
    setLong(value, bytes, offset);
    mySize = Math.max(mySize, offset + 8);
  }

  public static void setLong(long value, byte[] bytes, int offset) {
    long mask = 0xFF;
    for (int i = 0; i < 8; i++) {
      bytes[offset + i] = (byte) ((value & mask) >> i * 8);
      mask = mask << 8;
    }
  }

  public void setBit(int bitIndex, boolean set) {
    int byteIndex = bitIndex / 8;
    ensureCapacity(byteIndex + 1);
    byte bitMask = (byte) (1 << (bitIndex % 8));
    if (set)
      myBytes[byteIndex] |= bitMask;
    else
      myBytes[byteIndex] &= (bitMask ^ 0xFF);
    mySize = Math.max(mySize, byteIndex + 1);
  }

  public boolean isBitSet(int bitIndex) {
    assert checkIndex(bitIndex / 8);
    return isBitSet(myBytes, bitIndex);
  }

  public byte getByte(int index) {
    assert checkIndex(index) : index;
    return myBytes[index];
  }

  private boolean checkIndex(int index) {
    return index >= 0 && index < mySize;
  }

  public static boolean isBitSet(byte[] bytes, int bitIndex) {
    byte aByte = bytes[bitIndex / 8];
    return (aByte & (byte)(1 << (bitIndex %8))) != 0;
  }

  public void moveBytes(int from, int to, int shift) {
    if (shift == 0 || to <= from) 
      return;
    System.arraycopy(myBytes, from, myBytes, from + shift, to - from);
  }

  public void addUTF8(String string) {
    if (string == null) addInt(-1);
    else if (string.length() == 0) addInt(0);
    else {
      try {
        byte[] bytes = string.getBytes("UTF-8");
        addInt(bytes.length);
        add(bytes);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void addSubarray(byte[] array) {
    if(array == null) addInt(-1);
    else if (array.length == 0) addInt(0);
    else {
      addInt(array.length);
      add(array);
    }
  }

  @NotNull
  public static Stream createStream(@Nullable byte[] bytes) {
    if (bytes == null) bytes = Const.EMPTY_BYTES;
    return new Stream(bytes);
  }

  /**
   * Supports secure content - to avoid logging of secure data. When client code needs to log stream it should use {@link #toString()} to respect {@link #mySecure secure flag}.
   */
  public static class Stream {
    private final byte[] myBytes;
    private final int myLimit;
    /**
     * Secure flag. If set toString hides value - to avoid logging secure data
     */
    private final boolean mySecure;
    private int myPosition;
    private boolean myErrorOccurred = false;

    public Stream(byte[] bytes) {
      this(bytes, 0, bytes.length);
    }

    public Stream(byte[] bytes, int position, int limit) {
      this(bytes, position, limit, false);
    }

    public Stream(byte[] bytes, int position, int limit, boolean secure) {
      myBytes = bytes;
      myPosition = position;
      myLimit = limit;
      mySecure = secure;
    }

    /**
     * @return Secure stream.
     * @see #mySecure
     */
    public static Stream secure(byte[] bytes) {
      return new Stream(bytes, 0, bytes.length, true);
    }

    public long nextLong() {
      if (myLimit - myPosition < 8) {
        errorOccurred();
        LogHelper.error(this);
        return 0;
      }
      long result = getLong(myBytes, myPosition);
      myPosition += 8;
      return result;
    }

    public int nextInt() {
      if (myLimit - myPosition < 4) {
        errorOccurred();
        LogHelper.error(this);
        return 0;
      }
      int result = getInt(myBytes, myPosition);
      myPosition += 4;
      return result;
    }

    public byte nextByte() {
      if (myLimit - myPosition < 1) {
        errorOccurred();
        LogHelper.error(this);
        return 0;
      }
      byte result = myBytes[myPosition];
      myPosition++;
      return result;
    }

    public boolean nextBoolean() {
      byte value = nextByte();
      if (value == 0) return false;
      if (value == 1) return true;
      LogHelper.error("Wrong boolean value", value, this);
      return false;
    }

    public boolean nextBytes(byte[] bytes) {
      if (myLimit - myPosition < bytes.length) {
        errorOccurred();
        LogHelper.error(bytes.length, this);
        return false;
      }
      System.arraycopy(myBytes, myPosition, bytes, 0, bytes.length);
      myPosition += bytes.length;
      return true;
    }

    @Nullable
    public byte[] nextBytes(int length) {
      if (length == 0) return Const.EMPTY_BYTES;
      if (length > myLimit - myPosition) return null;
      byte[] result = new byte[length];
      boolean success = nextBytes(result);
      return success ? result : null;
    }

    public String nextUTF8() {
      if (myLimit - myPosition < 4) {
        errorOccurred();
        LogHelper.error(this);
        return null;
      }
      int length = nextInt();
      if (length < 0) return null;
      if (length == 0) return "";
      byte[] bytes = new byte[length];
      if (!nextBytes(bytes)) return null;
      try {
        return new String(bytes, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        Log.error(e);
        return null;
      }
    }

    public Stream nextSubstream() {
      if(myLimit - myPosition < 4) {
        errorOccurred();
        LogHelper.error(this);
        return null;
      }
      int length = nextInt();
      if(length < 0) return null;
      return subStream(length);
    }

    public Stream subStream(int length) {
      int position = myPosition;
      if (myPosition + length > myLimit) {
        LogHelper.error(length, this);
        length = Math.max(0, myLimit - myPosition);
        errorOccurred();
        assert myPosition == myLimit;
      } else myPosition += length;
      assert position + length <= myLimit;
      return new Stream(myBytes, position, position + length, mySecure);
    }

    public Stream tailSubStream() {
      return subStream(myLimit - myPosition);
    }

    public boolean isErrorOccurred() {
      return myErrorOccurred;
    }

    private void errorOccurred() {
      myErrorOccurred = true;
      myPosition = myLimit;
    }

    public boolean isSuccessfullyAtEnd() {
      return isAtEnd() && !myErrorOccurred;
    }

    public boolean isAtEnd() {
      return myPosition == myLimit;
    }

    public int getPosition() {
      return myPosition;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("Pos:").append(myPosition);
      builder.append(" Limit:").append(myLimit);
      builder.append(" [");
      if (mySecure) builder.append("***");
      else
        for (int i = 0, myBytesLength = myBytes.length; i < myBytesLength; i++) {
          byte aByte = myBytes[i];
          if (i > 0) builder.append(",");
          if (i == myPosition) builder.append("|");
          builder.append(Integer.toHexString(aByte));
        }
      builder.append("]");
      return builder.toString();
    }
  }
}
