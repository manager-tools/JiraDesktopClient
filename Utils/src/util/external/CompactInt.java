package util.external;

/* Imported from com.devexperts.qd.util */

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The <code>CompactInt</code> utility class provides algorithms for compact
 * serialization of integer numbers. It uses encoding scheme with variable-length
 * two's complement big-endian format capable to encode 64-bits signed numbers.
 * <p/>
 * The following table defines used serial format (the first byte is given in bits
 * with 'x' representing payload bit; the remaining bytes are given in bit count):
 * <pre>
 * 0xxxxxxx     - for -64 &lt;= N &lt; 64
 * 10xxxxxx  8x - for -8192 &lt;= N &lt; 8192
 * 110xxxxx 16x - for -1048576 &lt;= N &lt; 1048576
 * 1110xxxx 24x - for -134217728 &lt;= N &lt; 134217728
 * 11110xxx 32x - for -17179869184 &lt;= N &lt; 17179869184 (includes whole range of signed int)
 * 111110xx 40x - for -2199023255552 &lt;= N &lt; 2199023255552
 * 1111110x 48x - for -281474976710656 &lt;= N &lt; 281474976710656
 * 11111110 56x - for -36028797018963968 &lt;= N &lt; 36028797018963968
 * 11111111 64x - for -9223372036854775808 &lt;= N &lt; 9223372036854775808 (the range of signed long)
 * </pre>
 *
 * @author Denis Kislovsky
 */
public final class CompactInt {

  /**
   * Writes specified integer to the specified data output in a compact form.
   *
   * @throws IOException as specified data output does.
   */
  public static final void writeInt(DataOutput out, int n) throws IOException {
    if (n >= 0) {
      if (n < 0x40) {
        out.writeByte(n);
      } else if (n < 0x2000) {
        out.writeShort(0x8000 | n);
      } else if (n < 0x100000) {
        out.writeByte(0xC0 | (n >> 16));
        out.writeShort(n);
      } else if (n < 0x08000000) {
        out.writeInt(0xE0000000 | n);
      } else {
        out.writeByte(0xF0);
        out.writeInt(n);
      }
    } else {
      if (n >= -0x40) {
        out.writeByte(0x7F & n);
      } else if (n >= -0x2000) {
        out.writeShort(0xBFFF & n);
      } else if (n >= -0x100000) {
        out.writeByte(0xDF & (n >> 16));
        out.writeShort(n);
      } else if (n >= -0x08000000) {
        out.writeInt(0xEFFFFFFF & n);
      } else {
        out.writeByte(0xF7);
        out.writeInt(n);
      }
    }
  }

  /**
   * Reads integer from specified data input in a compact form.
   * If encoded number does not fit into <code>int</code> data type,
   * then loss of precision occurs as it is type casted into
   * <code>int</code>; the number is read entirely in this case.
   *
   * @throws IOException as specified data input does.
   */
  public static final int readInt(DataInput in) throws IOException {
    // The ((n << k) >> k) expression performs two's complement.
    int n = in.readUnsignedByte();
    if (n < 0x80)
      return (n << 25) >> 25;
    if (n < 0xC0)
      return (((n << 8) + in.readUnsignedByte()) << 18) >> 18;
    if (n < 0xE0)
      return (((n << 16) + in.readUnsignedShort()) << 11) >> 11;
    if (n < 0xF0)
      return (((n << 24) + (in.readUnsignedByte() << 16) + in.readUnsignedShort()) << 4) >> 4;
    // The encoded number is possibly out of range, some bytes have to be skipped.
    // The skipBytes(...) does the strange thing, thus readUnsignedByte() is used.
    while (((n <<= 1) & 0x10) != 0)
      in.readUnsignedByte();
    return in.readInt();
  }

  /**
   * Writes specified long to the specified data output in a compact form.
   *
   * @throws IOException as specified data output does.
   */
  public static final void writeLong(DataOutput out, long l) throws IOException {
    if (l == (long) (int) l) {
      writeInt(out, (int) l);
      return;
    }
    int n = (int) (l >>> 32);
    if (n >= 0) {
      if (n < 0x04) {
        out.writeByte(0xF0 | n);
      } else if (n < 0x0200) {
        out.writeShort(0xF800 | n);
      } else if (n < 0x010000) {
        out.writeByte(0xFC);
        out.writeShort(n);
      } else if (n < 0x800000) {
        out.writeInt(0xFE000000 | n);
      } else {
        out.writeByte(0xFF);
        out.writeInt(n);
      }
    } else {
      if (n >= -0x04) {
        out.writeByte(0xF7 & n);
      } else if (n >= -0x0200) {
        out.writeShort(0xFBFF & n);
      } else if (n >= -0x010000) {
        out.writeByte(0xFD);
        out.writeShort(n);
      } else if (n >= -0x800000) {
        out.writeInt(0xFEFFFFFF & n);
      } else {
        out.writeByte(0xFF);
        out.writeInt(n);
      }
    }
    out.writeInt((int) l);
  }

  /**
   * Reads long from specified data input in a compact form.
   *
   * @throws IOException as specified data input does.
   */
  public static final long readLong(DataInput in) throws IOException {
    // The ((n << k) >> k) expression performs two's complement.
    int n = in.readUnsignedByte();
    if (n < 0x80)
      return (n << 25) >> 25;
    if (n < 0xC0)
      return (((n << 8) + in.readUnsignedByte()) << 18) >> 18;
    if (n < 0xE0)
      return (((n << 16) + in.readUnsignedShort()) << 11) >> 11;
    if (n < 0xF0)
      return (((n << 24) + (in.readUnsignedByte() << 16) + in.readUnsignedShort()) << 4) >> 4;
    if (n < 0xF8) {
      n = (n << 29) >> 29;
    } else if (n < 0xFC) {
      n = (((n << 8) + in.readUnsignedByte()) << 22) >> 22;
    } else if (n < 0xFE) {
      n = (((n << 16) + in.readUnsignedShort()) << 15) >> 15;
    } else if (n < 0xFF) {
      n = (in.readByte() << 16) + in.readUnsignedShort();
    } else {
      n = in.readInt();
    }
    return ((long) n << 32) + (in.readInt() & 0xFFFFFFFFL);
  }

  /**
   * Prevents unintentional instantiation.
   */
  private CompactInt() {
  }

  public static int countBytes(int n) {
    if (n >= 0) {
      if (n < 0x40) {
        return 1;
      } else if (n < 0x2000) {
        return 2;
      } else if (n < 0x100000) {
        return 3;
      } else if (n < 0x08000000) {
        return 4;
      } else {
        return 5;
      }
    } else {
      if (n >= -0x40) {
        return 1;
      } else if (n >= -0x2000) {
        return 2;
      } else if (n >= -0x100000) {
        return 3;
      } else if (n >= -0x08000000) {
        return 4;
      } else {
        return 5;
      }
    }
  }
}
