package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface SDFFormat {
  void writeFileSignature(BufferedDataOutput output) throws IOException;

  void readFileSignature(BufferedDataInput input) throws IOException, FileFormatException;

  int readFormatVersion(BufferedDataInput input) throws IOException, FileFormatException;

  void readBlockSignature(BufferedDataInput input) throws IOException, FileFormatException;

  StoreFeature[] readBlockFeatures(BufferedDataInput input) throws IOException;

  void writeBlockSignature(BufferedDataOutput output) throws IOException;

  void writeBlockFeatures(BufferedDataOutput output, StoreFeature[] features) throws IOException;

  int getRequiredBlockSize(String storePath, byte[] payload, StoreFeature[] features);

  void writeFormatVersion(BufferedDataOutput output) throws IOException;

  int getFormatVersion();

  byte[] transformAfterReading(byte[] data, StoreFeature[] blockFeatures) throws FileFormatException;

  byte[] transformBeforeWriting(byte[] data, StoreFeature[] blockFeatures);
}

