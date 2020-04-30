package com.almworks.util.collections;

import com.almworks.util.tests.BaseTestCase;
import junit.framework.ComparisonFailure;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Dyoma
 */
@SuppressWarnings({"NonBooleanMethodNameMayNotStartWithQuestion"})
public class ByteArrayTests extends BaseTestCase {
  public void testReadFromStream() throws IOException {
    ByteArray byteArray = new ByteArray(2);
    ByteArrayInputStream stream = new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
    // Read in current byte buffer
    byteArray.readFrom(stream, 1);
    checkAllEqual(new byte[]{1}, byteArray.toNativeArray());
    // Reallocation
    byteArray.readFrom(stream, 2);
    checkAllEqual(new byte[]{1, 2, 3}, byteArray.toNativeArray());
    // Read after stream end
    byteArray.readFrom(stream, 2);
    checkAllEqual(new byte[]{1, 2, 3, 4}, byteArray.toNativeArray());
  }

  public void testReadAllFromStream() throws IOException {
    ByteArray byteArray = new ByteArray(4);
    byte[] bytes = new byte[]{1, 2, 3, 4};
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    byteArray.readAllFromStream(stream);
    checkAllEqual(bytes, byteArray.toNativeArray());
  }

  @SuppressWarnings({"MagicNumber"})
  public void testBitOperations() {
    ByteArray array = new ByteArray();
    int value = 1;
    for (int i = 0; i < 31; i++) {
      array.setBit(i, true);
      int byteIndex = i >> 3;
      assertEquals(String.valueOf(i), (byte) ((value >> 8 * byteIndex) & 0xFF), array.getByte(byteIndex));
      assertTrue(String.valueOf(i), array.isBitSet(i));
      for (int j = 0; j < byteIndex; j++)
        assertEquals(0, array.getByte(j));
      array.setBit(i, false);
      assertFalse(String.valueOf(i), array.isBitSet(i));
      assertEquals(0, array.getByte(0));
      value <<= 1;
    }
  }

  private static void checkAllEqual(byte[] expectedBytes, byte[] actualBytes) {
    assertEquals("Sizes aren't equal", expectedBytes.length, actualBytes.length);
    for (int i = 0; i < expectedBytes.length; i++) {
      byte expectedByte = expectedBytes[i];
      if (expectedByte != actualBytes[i])
        throw new ComparisonFailure("Bytes aren't equal", toTextLines(expectedBytes), toTextLines(actualBytes));
    }
  }

  private static String toTextLines(byte[] bytes) {
    StringBuffer buffer = new StringBuffer();
    String separator = "";
    for (int i = 0; i < bytes.length; i++) {
      byte aByte = bytes[i];
      buffer.append(separator);
      separator = "\n";
      buffer.append(aByte);
    }
    return buffer.toString();
  }
}
