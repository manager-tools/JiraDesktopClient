package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.WeakEncryption;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import util.external.CompactChar;
import util.external.CompactInt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SDFFormatV2 extends SDFFormatBase {
  public static final int DATAFILE_SIGNATURE = 0x5708EF12;
  private static final int DATA_BLOCK_SIGNATURE = 0xFFEEFFEF;
  private static final StoreFeature[] SUPPORTED_FEATURES = {StoreFeature.ENCRYPTED};

  public int getFormatVersion() {
    return 2;
  }

  public void writeFileSignature(BufferedDataOutput output) throws IOException {
    output.writeInt(DATAFILE_SIGNATURE);
  }

  public void readFileSignature(BufferedDataInput input) throws IOException, FileFormatException {
    int signature = input.readInt();
    if (signature != DATAFILE_SIGNATURE)
      throw new FileFormatException("invalid signature");
  }

  public int readFormatVersion(BufferedDataInput input) throws IOException, FileFormatException {
    int version = CompactInt.readInt(input);
    if (version != getFormatVersion())
      throw new FileFormatException("wrong format version");
    return version;
  }

  public void readBlockSignature(BufferedDataInput input) throws IOException, FileFormatException {
    int signature = input.readInt();
    if (signature != DATA_BLOCK_SIGNATURE)
      throw new FileFormatException("block signature not found");
  }

  public StoreFeature[] readBlockFeatures(BufferedDataInput input) throws IOException {
    String feature = CompactChar.readString(input);
    if (feature == null)
      return StoreFeature.EMPTY_FEATURES_ARRAY;
    List<StoreFeature> features = Collections15.arrayList();
    while (feature != null) {
      StoreFeature storeFeature = StoreFeature.findByName(feature);
      if (storeFeature == null) {
        Log.warn("unknown block feature " + feature + ", ignoring");
      } else {
        features.add(storeFeature);
      }
      feature = CompactChar.readString(input);
    }
    return features.toArray(new StoreFeature[features.size()]);
  }

  public void writeBlockSignature(BufferedDataOutput output) throws IOException {
    output.writeInt(DATA_BLOCK_SIGNATURE);
  }

  public void writeBlockFeatures(BufferedDataOutput output, StoreFeature[] features) throws IOException {
    for (int i = 0; i < features.length; i++) {
      StoreFeature feature = features[i];
      CompactChar.writeString(output, feature.getName());
    }
    CompactChar.writeString(output, null);
  }

  public int getRequiredBlockSize(String storePath, byte[] payload, StoreFeature[] features) {
    checkFeatures(features, SUPPORTED_FEATURES);
    if (isCrypting(features)) {
      payload = WeakEncryption.encrypt(payload);
    }
    int dataLength = (payload == null ? 0 : payload.length);
    int size = dataLength + 4 + CompactChar.countBytes(storePath) + CompactInt.countBytes(dataLength) + 5;
    for (int i = 0; i < features.length; i++) {
      size += CompactChar.countBytes(features[i].getName());
    }
    size += CompactChar.countBytes(null);
    return size;
  }

  public void writeFormatVersion(BufferedDataOutput output) throws IOException {
    CompactInt.writeInt(output, getFormatVersion());
  }

  public byte[] transformAfterReading(byte[] data, StoreFeature[] blockFeatures) throws FileFormatException {
    checkFeatures(blockFeatures, SUPPORTED_FEATURES);
    if (isCrypting(blockFeatures)) {
      try {
        data = WeakEncryption.decrypt(data);
      } catch (GeneralSecurityException e) {
        throw new FileFormatException(e);
      }
    }
    return data;
  }

  private boolean isCrypting(StoreFeature[] blockFeatures) {
    for (int i = 0; i < blockFeatures.length; i++) {
      if (blockFeatures[i] == StoreFeature.ENCRYPTED)
        return true;
    }
    return false;
  }

  public byte[] transformBeforeWriting(byte[] data, StoreFeature[] blockFeatures) {
    checkFeatures(blockFeatures, SUPPORTED_FEATURES);
    if (isCrypting(blockFeatures)) {
      data = WeakEncryption.encrypt(data);
    }
    return data;
  }
}
