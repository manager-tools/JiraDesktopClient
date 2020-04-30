package com.almworks.util.bmp;

import java.io.IOException;
public class BMPColorMap {
  int N;		// 1 << bitsPerPixel
  byte r[];				// red values
  byte g[];				// green values
  byte b[];				// blue values

  public BMPColorMap(LEDataInputStream in, BMPFileHeader h) throws IOException {
    N = h.actualColorsUsed;

    r = new byte[N];
    g = new byte[N];
    b = new byte[N];

    if (N > 0) {
      for (int i = 0; i < N; i++) {
        b[i] = in.readByte();
        g[i] = in.readByte();
        r[i] = in.readByte();

        if (h.bmpVersion == 3 || h.bmpVersion == 4)
          in.readByte();	// read ignore dummy byte
      }
    }
  }
}
