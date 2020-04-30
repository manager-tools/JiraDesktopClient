package com.almworks.store;

import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.io.InputPump;
import org.almworks.util.Log;

import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SDFFormatUtils {
  private static final SDFFormat[] formats = {new SDFFormatV1(), new SDFFormatV2()};

  public static SDFFormat guessFormat(FileChannel channel) throws IOException, FileFormatException {
    InputPump input = new InputPump(channel, 1024, 0);
    try {
      int signature = input.readInt();
      if (signature == SDFFormatV1.DATAFILE_SIGNATURE)
        return new SDFFormatV1();
      if (signature == SDFFormatV2.DATAFILE_SIGNATURE)
        return new SDFFormatV2();
      throw new FileFormatException("invalid signature " + signature);
    } finally {
      input.discard();
    }
  }

  public static SDFFormat getDefaultFormat() {
    return new SDFFormatV2();
  }

  public static SDFFormat getFormatByVersion(int formatVersion) {
    for (int i = 0; i < formats.length; i++) {
      SDFFormat format = formats[i];
      if (format.getFormatVersion() == formatVersion)
        return format;
    }
    Log.warn("cannot find format version " + formatVersion);
    return null;
  }
}
