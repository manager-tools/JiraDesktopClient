package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.Pair;
import com.almworks.util.fileformats.AlmworksFormatsUtil;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.fileformats.FormattedFile;
import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;
import com.almworks.util.io.InputPump;
import com.almworks.util.io.OutputPump;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import util.external.CompactChar;
import util.external.CompactInt;
import util.external.UID;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class StoreDataFile extends FormattedFile {
  public static final int MIN_BLOCK_SIZE = 64;
  public static final int TARGET_MAX_BLOCK_SIZE = 100000000;
  public static final int POSSIBLE_MAX_BLOCK_SIZE = TARGET_MAX_BLOCK_SIZE * 10;

  private static final int DEFAULT_BLOCKS_COUNT = 4096 * 8;

  private static final int BITS[] = {128, 64, 32, 16, 8, 4, 2, 1};

  private UID myUID = null;
  private int myBlockSize;
  private int myBlockCount;
  private int myZeroBlockPosition;
  private byte[] myBitMask;
  private int myBitMaskPosition;

  private SDFFormat myFormat = null;

  public StoreDataFile(File path) {
    super(path, false);
  }

  public synchronized void open() throws IOException, FileFormatException {
    super.open();
  }

  public synchronized void close() {
    super.close();
  }

  public synchronized void drop() throws InterruptedException, IOException {
    super.drop();
  }

  public synchronized void create(int blockSize, int blockCount, SDFFormat format) throws IOException {
    checkNotOpen();
    if (!isValidBlockSize(blockSize))
      throw new IllegalArgumentException("invalid block size " + blockSize);
    if (!isValidBlockCount(blockCount))
      throw new IllegalArgumentException("invalid block count " + blockCount);
    if (format == null)
      format = SDFFormatUtils.getDefaultFormat();
    assert format != null;
    myBlockSize = blockSize;
    myBlockCount = blockCount;
    myFormat = format;
    create();
  }

  public synchronized void create(int blockSize) throws IOException {
    create(blockSize, DEFAULT_BLOCKS_COUNT, null);
  }

  public synchronized void create(int blockSize, SDFFormat format) throws IOException {
    create(blockSize, DEFAULT_BLOCKS_COUNT, format);
  }

  public synchronized int write(String storePath, byte[] data, StoreFeature[] features) throws IOException, Full,
    BlockOverflow {
    checkOpen();
    if (storePath == null)
      throw new NullPointerException("storePath");
    if (data.length > myBlockSize)
      throw new BlockOverflow();
    int block = findEmptyBlock();
    if (block == -1)
      throw new Full();
    writeBlock(block, storePath, data, features);
    setBlockBit(block, true);
    if (myForceDiskWriteEachTime)
      myChannel.force(false);
    return block;
  }

  public int write(String storePath, byte[] data) throws BlockOverflow, Full, IOException {
    return write(storePath, data, StoreFeature.EMPTY_FEATURES_ARRAY);
  }

  public synchronized Pair<String, byte[]> read(int block) throws BlockIsFree, IOException, FileFormatException {
    checkOpen();
    if (!isValidBlockIndex(block))
      throw new IllegalArgumentException("block " + block);
    if (isBlockFree(block))
      throw new BlockIsFree();
    InputPump input = new InputPump(myChannel, myBlockSize, getBlockPosition(block));
    int startPosition = input.getPosition();
    myFormat.readBlockSignature(input);
    StoreFeature[] blockFeatures = myFormat.readBlockFeatures(input);
    String storePath = CompactChar.readString(input);
    int length = CompactInt.readInt(input);
    if (length < 0 || length > myBlockSize)
      throw new FileFormatException("data length " + length);
    byte[] data = new byte[length];
    input.readFully(data);
    AlmworksFormatsUtil.checkCRC(input, startPosition);
    input.discard();
    data = myFormat.transformAfterReading(data, blockFeatures);
    return Pair.<String, byte[]>create(storePath, data);
  }

  public synchronized StoreFeature[] getFeatures(int block) throws BlockIsFree, FileFormatException, IOException {
    checkOpen();
    if (!isValidBlockIndex(block))
      throw new IllegalArgumentException("block " + block);
    if (isBlockFree(block))
      throw new BlockIsFree();
    InputPump input = new InputPump(myChannel, myBlockSize, getBlockPosition(block));
    int startPosition = input.getPosition();
    myFormat.readBlockSignature(input);
    StoreFeature[] blockFeatures = myFormat.readBlockFeatures(input);
    input.discard();
    return blockFeatures;
  }

  public synchronized void free(int block) throws BlockIsFree, IOException {
    checkOpen();
    if (!isValidBlockIndex(block))
      throw new IllegalArgumentException("block " + block);
    if (isBlockFree(block))
      throw new BlockIsFree();
    setBlockBit(block, false);
    trim();
    if (myForceDiskWriteEachTime)
      myChannel.force(false);
    return;
  }

  public synchronized int getBlockSize() {
    checkOpen();
    return myBlockSize;
  }

  public synchronized UID getUID() {
    checkOpen();
    return myUID;
  }

  public synchronized List<Pair<Integer, String>> scanContent() throws FileFormatException, IOException {
    checkOpen();
    List<Pair<Integer, String>> result = Collections15.arrayList();
    for (int i = 0; i < myBitMask.length; i++) {
      if (myBitMask[i] != 0) {
        int mask = myBitMask[i] & 0xFF;
        for (int j = 0; j < 8; j++) {
          if ((mask & BITS[j]) > 0) {
            int block = i * 8 + j;
            InputPump input = new InputPump(myChannel, 1024, getBlockPosition(block));
            myFormat.readBlockSignature(input);
            StoreFeature[] blockFeatures = myFormat.readBlockFeatures(input);
            String storePath = CompactChar.readString(input);
            input.discard();
            result.add(Pair.create(block, storePath));
          }
        }
      }
    }
    return result;
  }

  public StoreFeature[] getFileFeatures() {
    return StoreFeature.PLAIN_STORE;
  }

  protected void doCreate() throws IOException {
    setupNew();

    OutputPump output = new OutputPump(myChannel);
    writeHeader(output);
    myBitMaskPosition = output.getPosition();
    writeBitMask(output);
    myZeroBlockPosition = output.getPosition();
    output.flush();
  }

  protected void doOpen() throws FileFormatException, IOException {
    assert myFormat == null;
    myFormat = SDFFormatUtils.guessFormat(myChannel);
    assert myFormat != null;
    InputPump input = new InputPump(myChannel, 2048, 0);
    readHeader(input);
    myBitMaskPosition = input.getPosition();
    readBitMask(input);
    myZeroBlockPosition = input.getPosition();
    input.discard();
  }

  protected void doClose() {
  }

  protected void doDrop() {
  }

  protected void cleanUp() {
    super.cleanUp();
    myZeroBlockPosition = 0;
    myBitMaskPosition = 0;
    myBitMask = null;
    myBlockCount = 0;
    myBlockSize = 0;
    myUID = null;
    myFormat = null;
  }

  protected void checkOpen() {
    super.checkOpen();
    assert myZeroBlockPosition > 0;
    assert myBitMaskPosition > 0;
    assert myBitMask != null;
    assert myBlockCount > 0;
    assert myBlockSize >= MIN_BLOCK_SIZE;
    assert myUID != null;
    assert myFormat != null;
  }

  void readBitMask(BufferedDataInput input) throws IOException, FileFormatException {
    int length = input.readShort();
    if (!isValidBitMaskLength(length, myBlockCount))
      throw new FileFormatException("invalid bitmask length");
    myBitMask = new byte[length];
    input.readFully(myBitMask);
  }

  int getBlockPosition(int block) {
    assert myZeroBlockPosition > 0;
    assert block >= 0;
    assert block < myBlockCount;
    assert isValidBlockSize(myBlockSize);
    return myZeroBlockPosition + block * myBlockSize;
  }

  boolean isBlockFree(int block) {
    if (!isValidBlockIndex(block))
      throw new IllegalArgumentException("block " + block);
    int index = block / 8;
    assert index >= 0 && index < myBitMask.length;
    int mask = myBitMask[index] & 0xFF;
    return (mask & BITS[block & 0x07]) == 0;
  }

  boolean isValidBlockIndex(int block) {
    return block >= 0 && block < myBlockCount;
  }

  int findEmptyBlock() {
    int i = 0;
    for (; i < myBitMask.length; i++)
      if (myBitMask[i] != (byte) -1)
        break;
    if (i == myBitMask.length)
      return -1;
    int position = -1;
    int mask = (int) myBitMask[i] & 0xFF;
    assert BITS.length == 8;
    assert mask != 0xFF;
    for (int j = 0; j < 8; j++) {
      if ((mask & BITS[j]) == 0) {
        position = j;
        break;
      }
    }
    assert position != -1;
    position += i * 8;
    return position < myBlockCount ? position : -1;
  }

  private void writeBitMask(BufferedDataOutput output) throws IOException {
    assert myBitMask != null;
    assert isValidBitMaskLength(myBitMask.length, myBlockCount);
    output.writeShort(myBitMask.length);
    output.write(myBitMask);
  }

  private void writeHeader(BufferedDataOutput output) throws IOException {
    int startPosition = output.getPosition();
    myFormat.writeFileSignature(output);
    assert myUID != null;
    myUID.write(output);

    // skip length for now
    int lengthPosition = output.getPosition();
    output.writeShort(0);

    assert isValidBlockSize(myBlockSize);
    assert isValidBlockCount(myBlockCount);
    output.writeInt(myBlockSize);
    output.writeInt(myBlockCount);
    myFormat.writeFormatVersion(output);
    int endPosition = output.getPosition();

    output.setPosition(lengthPosition);
    output.writeShort(endPosition - startPosition);
    output.setPosition(endPosition);

    AlmworksFormatsUtil.putCRC(output, startPosition, endPosition - startPosition);
  }

  private void readHeader(BufferedDataInput input) throws IOException, FileFormatException {
    int startPosition = input.getPosition();
    myFormat.readFileSignature(input);
    myUID = UID.read(input);
    short headerLength = input.readShort();
    myBlockSize = input.readInt();
    if (!isValidBlockSize(myBlockSize))
      throw new FileFormatException("invalid block size " + myBlockSize);
    myBlockCount = input.readInt();
    if (!isValidBlockCount(myBlockCount))
      throw new FileFormatException("invalid block count " + myBlockCount);
    myFormat.readFormatVersion(input);
    // todo read features
    AlmworksFormatsUtil.checkCRC(input, startPosition);
  }

  private void setBlockBit(int block, boolean setOrClear) throws IOException {
    int bitIndex = block / 8;
    if (setOrClear) {
      myBitMask[bitIndex] = (byte) (myBitMask[bitIndex] | BITS[block & 0x07]);
    } else {
      myBitMask[bitIndex] = (byte) (myBitMask[bitIndex] & ~BITS[block & 0x07]);
    }
    OutputPump output = new OutputPump(myChannel, myBitMask.length + 20, myBitMaskPosition);
    writeBitMask(output);
    output.flush();
  }

  private void writeBlock(int block, String storePath, byte[] data, StoreFeature[] features) throws IOException,
    BlockOverflow {
    OutputPump output = new OutputPump(myChannel, myBlockSize * 2, getBlockPosition(block));
    int startPos = output.getPosition();
    myFormat.writeBlockSignature(output);
    myFormat.writeBlockFeatures(output, features);
    CompactChar.writeString(output, storePath);
    data = myFormat.transformBeforeWriting(data, features);
    CompactInt.writeInt(output, data.length);
    output.write(data);
    AlmworksFormatsUtil.putCRC(output, startPos);
    int written = output.getPosition() - startPos;
    if (written > myBlockSize) {
      output.unwrite(written);
      throw new BlockOverflow();
    }
    output.flush();
  }

  private void trim() throws IOException {
    // find last used block
    int i;
    for (i = myBitMask.length - 1; i >= 0; i--) {
      if (myBitMask[i] != 0)
        break;
    }
    int trimBlock = 0;
    if (i >= 0) {
      trimBlock = 8 * (i + 1);
      int mask = myBitMask[i] & 0xFF;
      for (int j = 0; j < 8; j++) {
        if ((mask & BITS[7 - j]) > 0) {
          trimBlock -= j;
          break;
        }
      }
    }
    if (trimBlock >= myBlockCount)
      return;
    int trimPosition = getBlockPosition(trimBlock);
    if (trimPosition < myChannel.size()) {
      // due to the bug in JDK 1.4, we have to avoid certain IOException here
      // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
      // see http://bugs.deskzilla.com/show_bug.cgi?id=125
      try {
        myChannel.truncate(trimPosition);
      } catch (IOException e) {
        Log.warn("exception in truncating store file, ignoring...", e);
        
      }
    }
  }

  private void setupNew() {
    myUID = new UID();
    myBitMask = initBitMask(myBlockCount);
  }

  private static boolean isValidBlockCount(int blockCount) {
    return blockCount > 0 && blockCount < 100000;
  }

  private static boolean isValidBlockSize(int blockSize) {
    return blockSize >= MIN_BLOCK_SIZE && blockSize <= POSSIBLE_MAX_BLOCK_SIZE;
  }

  private static boolean isValidBitMaskLength(int length, int blockCount) {
    return length == (blockCount + 7) / 8;
  }

  private static byte[] initBitMask(int blocksCount) {
    int bitmaskLength = (blocksCount + 7) / 8;
    byte[] array = new byte[bitmaskLength];
    Arrays.fill(array, (byte) 0);
    return array;
  }

  public int getFormatVersion() {
    checkOpen();
    return myFormat.getFormatVersion();
  }

  public static class Full extends StoreDataFileException {
  }

  public static class BlockOverflow extends StoreDataFileException {
  }

  public static class BlockIsFree extends StoreDataFileException {
  }
}

