package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
class SDFFormatV1 extends SDFFormatBase {
  public static final int DATAFILE_SIGNATURE = 0x5708EF11;
  private static final int DATA_BLOCK_SIGNATURE = 0xFFEEFFEE;

  public int getFormatVersion() {
    return 1;
  }

  public void writeFileSignature(BufferedDataOutput output) throws IOException {
    output.writeInt(DATAFILE_SIGNATURE);
  }

  public void readFileSignature(BufferedDataInput input) throws IOException, FileFormatException {
    int signature = input.readInt();
    if (signature != DATAFILE_SIGNATURE)
      throw new FileFormatException("invalid signature");
  }

  public void readBlockSignature(BufferedDataInput input) throws IOException, FileFormatException {
    int signature = input.readInt();
    if (signature != DATA_BLOCK_SIGNATURE)
      throw new FileFormatException("block signature not found");
  }

  public StoreFeature[] readBlockFeatures(BufferedDataInput input) throws IOException {
    return StoreFeature.EMPTY_FEATURES_ARRAY;
  }

  public void writeBlockSignature(BufferedDataOutput output) throws IOException {
    output.writeInt(DATA_BLOCK_SIGNATURE);
  }

  public void writeBlockFeatures(BufferedDataOutput output, StoreFeature[] features) {
    checkFeatures(features, StoreFeature.EMPTY_FEATURES_ARRAY);
  }

  public int getRequiredBlockSize(String storePath, byte[] payload, StoreFeature[] features) {
    int dataLength = (payload == null ? 0 : payload.length);
    return dataLength + 4 + CompactChar.countBytes(storePath) + CompactInt.countBytes(dataLength) + 5;
  }

  public void writeFormatVersion(BufferedDataOutput output) {
    // do nothing
  }

  public byte[] transformAfterReading(byte[] data, StoreFeature[] blockFeatures) {
    checkFeatures(blockFeatures, StoreFeature.EMPTY_FEATURES_ARRAY);
    return data;
  }

  public byte[] transformBeforeWriting(byte[] data, StoreFeature[] blockFeatures) {
    checkFeatures(blockFeatures, StoreFeature.EMPTY_FEATURES_ARRAY);
    return data;
  }
}
