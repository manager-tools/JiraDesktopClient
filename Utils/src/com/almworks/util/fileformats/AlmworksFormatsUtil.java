package com.almworks.util.fileformats;

import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class AlmworksFormatsUtil {
  public static final int CRC_MARK = 'C';

  public static int getCRC32(ByteBuffer buffer, int offset, int length) {
    CRC32 crc32 = new CRC32();
    if (buffer.hasArray()) {
      crc32.update(buffer.array(), buffer.position() + offset, length);
    } else {
      for (int i = 0; i < length; i++) {
        byte b = buffer.get(offset + i);
        crc32.update(b);
      }
    }
    return (int) crc32.getValue();
  }

  public static void putCRC(BufferedDataOutput output, int offset, int length) throws IOException {
    ByteBuffer buffer = output.getBuffer();
    int crcValue = getCRC32(buffer, offset, length);
    output.writeByte(CRC_MARK);
    output.writeInt(crcValue);
  }

  public static void checkCRC(BufferedDataInput input, int offset, int length) throws IOException, FileFormatException {
    ByteBuffer buffer = input.getBuffer();
    int crcValue = getCRC32(buffer, offset, length);
    int crcMark = input.readByte() & 0xFF;
    if (crcMark != CRC_MARK)
      throw new FileFormatException("crc not found");
    int writtenCRC = input.readInt();
    if (writtenCRC != crcValue)
      throw new FileFormatException("data file corrupt - bad crc");
  }

  public static void checkCRC(BufferedDataInput input, int startOffset) throws FileFormatException, IOException {
    checkCRC(input, startOffset, input.getPosition() - startOffset);
  }

  public static void putCRC(BufferedDataOutput output, int startPosition) throws IOException {
    putCRC(output, startPosition, output.getPosition() - startPosition);
  }
}
