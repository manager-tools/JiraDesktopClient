package util.external;

/* Imported from com.devexperts.qd.util */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;

/**
 * The <code>CompactChar</code> utility class provides algorithms for compact
 * serialization of characters and strings. It uses <tt>CESU-8</tt> format
 * (the format close to <tt>UTF-8</tt> but with special handling of surrogate
 * characters). This is generally the same format as used by standard Java I/O
 * streams, though Java uses modified <tt>CESU-8</tt> (Java represents character
 * with code 0 using 2-byte encoding, not 1-byte as required).
 * <p/>
 * When encoding character sequences, the <code>CompactChar</code> uses
 * <code>CompactInt</code> to encode character sequence length first,
 * then serializes characters themselves. The value -1 for length is used
 * as a marker to distinguish 'null' sequence from empty sequence.
 * <p/>
 * See <A href="http://www.unicode.org/unicode/reports/tr26/tr26-2.html">CESU-8</A>
 * for format basics.
 *
 * @author Denis Kislovsky
 */
public final class CompactChar {
  private static final int DEFAULT_MAXIMUM_LENGTH;

  static {
    long big = Runtime.getRuntime().maxMemory() / 10;
    DEFAULT_MAXIMUM_LENGTH = big > 1000 && big < 1000000L ? (int)big : 1000000;
  }

  /**
   * Writes specified character to the specified data output in a compact form.
   *
   * @throws IOException as specified data output does.
   */
  public static final void writeChar(DataOutput out, char c) throws IOException {
    if (c <= 0x007F) {
      out.writeByte(c);
    } else if (c <= 0x07FF) {
      out.writeShort(0xC080 + ((c << 2) & 0x1F00) + (c & 0x3F));
    } else {
      out.writeByte(0xE0 + (c >> 12));
      out.writeShort(0x8080 + ((c << 2) & 0x3F00) + (c & 0x3F));
    }
  }

  /**
   * Reads character from specified data input in a compact form.
   *
   * @throws UTFDataFormatException if the bytes do not represent
   *                                a valid CESU-8 encoding of a Unicode character.
   * @throws IOException            as specified data input does.
   */
  public static final char readChar(DataInput in) throws IOException {
    int c = in.readByte();
    if (c < 0) { // multi-byte case
      if ((c & 0xE0) == 0xC0) { // 2-byte case
        c = c & 0x1F;
        if (c < 0x02) // Check range 0x00..0x7F
          throw new UTFDataFormatException();
      } else if ((c & 0xF0) == 0xE0) { // 3-byte case
        int cc = in.readByte();
        if ((cc & 0xC0) != 0x80)
          throw new UTFDataFormatException();
        c = ((c & 0x0F) << 6) + (cc & 0x3F);
        if (c < 0x20) // Check range 0x0000..0x07FF
          throw new UTFDataFormatException();
      } else
        throw new UTFDataFormatException();
      int cc = in.readByte();
      if ((cc & 0xC0) != 0x80)
        throw new UTFDataFormatException();
      c = (c << 6) + (cc & 0x3F);
    }
    return (char) c;
  }

  /**
   * Writes specified characters to the specified data output in a compact form.
   * Accepts <code>null</code> array as a valid value.
   *
   * @throws IOException as specified data output does.
   */
  public static final void writeChars(DataOutput out, char[] c) throws IOException {
    int len = c == null ? -1 : c.length;
    CompactInt.writeInt(out, len);
    for (int i = 0; i < len; i++)
      //noinspection ConstantConditions
      writeChar(out, c[i]);
  }

  /**
   * Reads characters from specified data input in a compact form.
   * Returns <code>null</code> if such value was written to the stream.
   *
   * @throws UTFDataFormatException if the bytes do not represent
   *                                a valid CESU-8 encoding of a Unicode string.
   * @throws IOException            as specified data input does.
   */
  public static final char[] readChars(DataInput in) throws IOException {
    int len = CompactInt.readInt(in);
    if (len == -1)
      return null;
    if (len < 0)
      throw new UTFDataFormatException();
    char[] chars = new char[len];
    for (int i = 0; i < len; i++)
      chars[i] = readChar(in);
    return chars;
  }

  /**
   * Writes specified string to the specified data output in a compact form.
   * Accepts <code>null</code> string as a valid value.
   *
   * @throws IOException as specified data output does.
   */
  public static final void writeString(DataOutput out, String s) throws IOException {
    int len = s == null ? -1 : s.length();
    CompactInt.writeInt(out, len);
    for (int i = 0; i < len; i++)
      //noinspection ConstantConditions
      writeChar(out, s.charAt(i));
  }

  /**
   * Reads string from specified data input in a compact form.
   * Returns <code>null</code> if such value was written to the stream.
   *
   * @throws UTFDataFormatException if the bytes do not represent
   *                                a valid CESU-8 encoding of a Unicode string.
   * @throws IOException            as specified data input does.
   */
  public static final String readString(DataInput in) throws IOException {
    return readString(in, DEFAULT_MAXIMUM_LENGTH);
  }

  public static final String readString(DataInput in, int maximumLength) throws IOException {
    int len = CompactInt.readInt(in);
    if (len == -1)
      return null;
    if (len < 0)
      throw new UTFDataFormatException();
    if (len == 0)
      return "";
    if (maximumLength > 0 && len > maximumLength)
      throw new IOException("cannot read string[" + len + "]");
    char[] chars = new char[len];
    for (int i = 0; i < len; i++)
      chars[i] = readChar(in);
    return new String(chars);
  }

  /**
   * Prevents unintentional instantiation.
   */
  private CompactChar() {
  }

  public static int countBytes(String s) {
    int len = s == null ? -1 : s.length();
    int result = CompactInt.countBytes(len);
    for (int i = 0; i < len; i++)
      //noinspection ConstantConditions
      result += countBytes(s.charAt(i));
    return result;
  }

  private static int countBytes(char c) {
    if (c <= 0x007F) {
      return 1;
    } else if (c <= 0x07FF) {
      return 2;
    } else {
      return 3;
    }
  }
}
