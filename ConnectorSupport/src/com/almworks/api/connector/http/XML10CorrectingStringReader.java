package com.almworks.api.connector.http;

import org.almworks.util.Log;
import org.apache.xerces.util.XMLChar;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is copied and modified StringReader. Copying was necessary to get access to the string.
 */
public class XML10CorrectingStringReader extends Reader {
  private static final char INVALID_REPLACEMENT = '?';
  private static final Set<Integer> myWarned = Collections.synchronizedSet(new HashSet<Integer>());

  private String str;
  private int length;
  private int next = 0;
  private int mark = 0;


  public XML10CorrectingStringReader(String s) {
    this.str = s;
    this.length = s.length();
  }

  public int read() throws IOException {
    int ch = superRead();
    if (!XMLChar.isValid(ch)) {
      maybeWarn(ch);
      ch = INVALID_REPLACEMENT;
    } 
    return ch;
  }

  public int read(char cbuf[], int off, int len) throws IOException {
    int result = superRead(cbuf, off, len);
    if (result > 0) {
      for (int i = off + result - 1; i >= off; i--) {
        if (!XMLChar.isValid(cbuf[i])) {
          maybeWarn(cbuf[i]);
          cbuf[i] = INVALID_REPLACEMENT;
        }
      }
    }
    return result;
  }

  private static void maybeWarn(int ch) {
    if (myWarned.add(ch)) {
      Log.warn("replacing invalid xml char &#x" + Integer.toHexString(ch) + " with '?'");
    }
  }

  private void ensureOpen() throws IOException {
    if (str == null)
      throw new IOException("Stream closed");
  }

  private int superRead() throws IOException {
    synchronized (lock) {
      ensureOpen();
      if (next >= length)
        return -1;
      return str.charAt(next++);
    }
  }

  public int superRead(char cbuf[], int off, int len) throws IOException {
    synchronized (lock) {
      ensureOpen();
      if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }
      if (next >= length)
        return -1;
      int n = Math.min(length - next, len);
      str.getChars(next, next + n, cbuf, off);
      next += n;
      return n;
    }
  }

  public long skip(long ns) throws IOException {
    synchronized (lock) {
      ensureOpen();
      if (next >= length)
        return 0;
      // Bound skip by beginning and end of the source
      long n = Math.min(length - next, ns);
      n = Math.max(-next, n);
      next += n;
      return n;
    }
  }

  public boolean ready() throws IOException {
    synchronized (lock) {
      ensureOpen();
      return true;
    }
  }

  public boolean markSupported() {
    return true;
  }

  public void mark(int readAheadLimit) throws IOException {
    if (readAheadLimit < 0) {
      throw new IllegalArgumentException("Read-ahead limit < 0");
    }
    synchronized (lock) {
      ensureOpen();
      mark = next;
    }
  }

  public void reset() throws IOException {
    synchronized (lock) {
      ensureOpen();
      next = mark;
    }
  }

  public void close() {
    str = null;
  }
}
