package com.almworks.util.io;

import java.io.DataInput;
import java.nio.ByteBuffer;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface BufferedDataInput extends DataInput {
  int getPosition();

  ByteBuffer getBuffer();

  void discard();

  void unread(int count);

  long getCarrierFilePosition();
}
