package com.almworks.util.io;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * BufferedDataOutput is an extension to data output that presents a way to read what was written
 * and to position writing pointer.
 *
 * @author sereda
 */
public interface BufferedDataOutput extends DataOutput {
  /**
   * Gets current position. Usually position starts from 0, but it is implementation-specific.
   * Position cannot be less than 0. After writing data, and after flushing position changes.
   *
   * @return current position that you could later setPosition() to.
   */
  int getPosition();

  /**
   * Sets current position for writing. Position should be previously got with getPosition() and
   * no flush() should happen in between.
   */
  void setPosition(int position);

  /**
   * Gets a byte buffer that is backed by the real buffer. Write in this buffer only at your own risk.
   */
  ByteBuffer getBuffer();

  /**
   * Flushes possible buffers into an underlying medium. After a flush, you may not be able
   * to go to a previously saved position.
   */
  void flush() throws IOException;

  void unwrite(int count);
}
