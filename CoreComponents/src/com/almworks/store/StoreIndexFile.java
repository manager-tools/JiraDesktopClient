package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.fileformats.AlmworksFormatsUtil;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.fileformats.FormattedFile;
import com.almworks.util.io.BufferedDataInput;
import com.almworks.util.io.BufferedDataOutput;
import com.almworks.util.io.InputPump;
import com.almworks.util.io.OutputPump;
import org.almworks.util.Collections15;
import org.almworks.util.RuntimeInterruptedException;
import util.external.CompactChar;
import util.external.CompactInt;
import util.external.UID;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreIndexFile extends FormattedFile {
  private static final int INDEXFILE_SIGNATURE = 0x5708E1EF;
  private static final int FILE_ENTRY_MARKER = 'F';
  private static final int FILE_FEATURE_MARKER = '+';
  private static final int NOMORE_FEATURES_MARKER = '=';
  private static final int INDEX_ENTRY_MARKER = 'I';

  public StoreIndexFile(File path) {
    super(path, true);
  }


  public synchronized void write(StoreIndex index) throws IOException {
    boolean success = false;
    try {
      drop();
      create();
      writeIndex(index);
      close();
      success = true;
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    } finally {
      if (!success) {
        cleanUp();
        try {
          drop();
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
      }
    }
  }

  public synchronized StoreIndex read() throws IOException, FileFormatException {
    try {
      open();
      StoreIndex result = readIndex();
      close();
      return result;
    } finally {
      cleanUp();
    }
  }


  private StoreIndex readIndex() throws FileFormatException, IOException {
    InputPump input = new InputPump(myChannel, 1024 * 16);
    StoreIndex result = new StoreIndex();
    int startPosition = input.getPosition();
    readHeader(input);
    while (true) {
      int nextRecord = input.readByte() & 0xFF;
      if (nextRecord == FILE_ENTRY_MARKER) {
        result.addFileInfo(readFileInfo(input));
      } else if (nextRecord == INDEX_ENTRY_MARKER) {
        result.addEntryInfo(readEntryInfo(input, result.getFiles().size()));
      } else if (nextRecord == AlmworksFormatsUtil.CRC_MARK) {
        input.unread(1);
        AlmworksFormatsUtil.checkCRC(input, startPosition);
        break;
      } else {
        throw new FileFormatException("bad record " + nextRecord);
      }
    }
    return result;
  }

  private StoreIndex.EntryInfo readEntryInfo(BufferedDataInput input, int fileCount) throws IOException,
    FileFormatException {
    String storePath = CompactChar.readString(input);
    int fileIndex = CompactInt.readInt(input);
    if (fileIndex < 0 || fileIndex >= fileCount)
      throw new FileFormatException("incorrect file index " + fileIndex);
    int blockID = CompactInt.readInt(input);
    return new StoreIndex.EntryInfo(storePath, fileIndex, blockID);
  }


  private StoreIndex.FileInfo readFileInfo(BufferedDataInput input) throws IOException, FileFormatException {
    String fileName = CompactChar.readString(input);
    long lastSize = CompactInt.readLong(input);
    long lastModificationTime = CompactInt.readLong(input);
    int blockSize = CompactInt.readInt(input);
    int formatVersion = CompactInt.readInt(input);
    List<StoreFeature> features = null;
    while (true) {
      int marker = input.readByte() & 0xFF;
      if (marker == FILE_FEATURE_MARKER) {
        if (features == null)
          features = Collections15.arrayList();
        String featureName = CompactChar.readString(input);
        StoreFeature feature = StoreFeature.findByName(featureName);
        if (feature == null)
          throw new FileFormatException("unknown feature " + featureName);
        features.add(feature);
      } else if (marker == NOMORE_FEATURES_MARKER) {
        break;
      } else {
        throw new FileFormatException("unknown marker " + marker);
      }
    }
    StoreFeature[] featuresArray = features == null ? StoreFeature.PLAIN_STORE :
      features.toArray(new StoreFeature[features.size()]);
    return new StoreIndex.FileInfo(fileName, lastSize, lastModificationTime, blockSize, featuresArray, formatVersion);
  }

  private void readHeader(InputPump input) throws IOException, FileFormatException {
    int signature = input.readInt();
    if (signature != INDEXFILE_SIGNATURE)
      throw new FileFormatException("bad index file - signature not found");
    UID.read(input); // whatever
    int filesCount = CompactInt.readInt(input);
    int entriesCount = CompactInt.readInt(input);
  }


  private void writeIndex(StoreIndex index) throws IOException {
    OutputPump output = new OutputPump(myChannel);
    int startPosition = output.getPosition();
    List<StoreIndex.FileInfo> files = index.getFiles();
    SortedMap<String, StoreIndex.EntryInfo> entries = index.getEntries();
    writeHeader(output, files.size(), entries.size());
    for (int i = 0; i < files.size(); i++)
      writeFileInfo(output, files.get(i));
    for (Iterator<StoreIndex.EntryInfo> iterator = entries.values().iterator(); iterator.hasNext();)
      writeEntryInfo(output, iterator.next());
    AlmworksFormatsUtil.putCRC(output, startPosition);
    output.flush();
  }

  private void writeEntryInfo(BufferedDataOutput output, StoreIndex.EntryInfo entryInfo) throws IOException {
    output.writeByte(INDEX_ENTRY_MARKER);
    CompactChar.writeString(output, entryInfo.getStorePath());
    CompactInt.writeInt(output, entryInfo.getFileIndex());
    CompactInt.writeInt(output, entryInfo.getBlockID());
  }

  private void writeFileInfo(BufferedDataOutput output, StoreIndex.FileInfo fileInfo) throws IOException {
    output.writeByte(FILE_ENTRY_MARKER);
    CompactChar.writeString(output, fileInfo.getFileName());
    CompactInt.writeLong(output, fileInfo.getLastSize());
    CompactInt.writeLong(output, fileInfo.getLastModificationTime());
    CompactInt.writeInt(output, fileInfo.getBlockSize());
    CompactInt.writeInt(output, fileInfo.getFormatVersion());
    StoreFeature[] features = fileInfo.getFeatures();
    for (int i = 0; i < features.length; i++) {
      StoreFeature feature = features[i];
      output.writeByte(FILE_FEATURE_MARKER);
      CompactChar.writeString(output, feature.getName());
    }
    output.writeByte(NOMORE_FEATURES_MARKER);
  }

  private void writeHeader(BufferedDataOutput output, int fileCount, int entryCount) throws IOException {
    output.writeInt(INDEXFILE_SIGNATURE);
    new UID().write(output);
    CompactInt.writeInt(output, fileCount);
    CompactInt.writeInt(output, entryCount);
  }

  public String toString() {
    return "storeIndex:" + myPath;
  }
}
