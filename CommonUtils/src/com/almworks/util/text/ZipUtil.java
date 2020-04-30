package com.almworks.util.text;

import org.almworks.util.ArrayUtil;
import org.almworks.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author dyoma
 */
public class ZipUtil {
  private static final ThreadLocal<byte[]> ourBuffer = new ThreadLocal<byte[]>();
  private static final ThreadLocal<Inflater> ourInflater = new ThreadLocal<Inflater>();
  private static final ThreadLocal<Deflater> ourDeflater = new ThreadLocal<Deflater>();


  @Nullable
  public static byte[] encode(String str) {
    return encode(str, "UTF-8");
  }

  public static byte[] encode(String str, String charsetName) {
    byte[] bytes;
    try {
      bytes = str.getBytes(charsetName);
    } catch (UnsupportedEncodingException e) {
      Log.error(e);
      return null;
    }
    byte[] result = getBuffer(bytes.length);
    int offset = 0;
    Deflater compresser = getDeflater();
    compresser.setInput(bytes);
    compresser.finish();
    while (true) {
      int tail = result.length - offset;
      int compressed = compresser.deflate(result, offset, tail);
      if (compressed == 0) {
        Log.error(str + " (" + offset + ", " + tail + ")");
        return null;
      }
      offset += compressed;
      if (compressed < tail) break;
      result = copyBuffer(result);
    }
    releaseDeflater(compresser);
    byte[] copy = new byte[offset];
    System.arraycopy(result, 0, copy, 0, copy.length);
    releaseBytes(result);
    return copy;
  }

  @Nullable
  public static String decode(byte[] zipped) {
    return decode(zipped, "UTF-8");
  }

  public static String decode(byte[] zipped, String charsetName) {
    Inflater decompresser = getInflater();
    decompresser.setInput(zipped);
    byte[] result = getBuffer(zipped.length * 3);
    int offset = 0;
    while (true) {
      int tailLength = result.length - offset;
      int resultLength = 0;
      try {
        resultLength = decompresser.inflate(result, offset, tailLength);
      } catch (DataFormatException e) {
        Log.error("Wrong compressed data " + ArrayUtil.toString(zipped), e);
        return null;
      }
      if (resultLength == 0) {
        Log.error("Wrong compressed data " + ArrayUtil.toString(zipped));
        return null;
      }
      offset += resultLength;
      if (resultLength < tailLength) break;
      result = copyBuffer(result);
    }
    releaseInflater(decompresser);
    String str;
    try {
      str = new String(result, 0, offset, charsetName);
    } catch (UnsupportedEncodingException e) {
      Log.error(e);
      return null;
    }
    releaseBytes(result);
    return str;
  }

  @NotNull
  private static byte[] getBuffer(int length) {
    byte[] bytes = ourBuffer.get();
    if (bytes == null || bytes.length < length) bytes = new byte[length];
    else ourBuffer.set(null);
    return bytes;
  }

  private static void releaseBytes(byte[] bytes) {
    if (bytes == null) return;
    byte[] current = ourBuffer.get();
    if (current == null || current.length < bytes.length) ourBuffer.set(bytes);
  }

  private static byte[] copyBuffer(byte[] result) {
    byte[] newBuffer = getBuffer(result.length * 2);
    System.arraycopy(result, 0, newBuffer, 0, result.length);
    result = newBuffer;
    return result;
  }

  @NotNull
  private static Inflater getInflater() {
    Inflater existing = ourInflater.get();
    if (existing == null) existing = new Inflater();
    else ourInflater.set(null);
    return existing;
  }

  private static void releaseInflater(Inflater inflater) {
    if (inflater == null) return;
    if (ourInflater.get() != null) inflater.end();
    else {
      inflater.reset();
      ourInflater.set(inflater);
    }
  }

  private static Deflater getDeflater() {
    Deflater existing = ourDeflater.get();
    if (existing == null) existing = new Deflater();
    else ourDeflater.set(null);
    return existing;
  }

  private static void releaseDeflater(Deflater deflater) {
    if (deflater == null) return;
    if (ourDeflater.get() != null) deflater.end();
    else {
      deflater.reset();
      ourDeflater.set(deflater);
    }
  }
}
