package com.almworks.util.io.persist;

import util.external.CompactChar;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

public class PersistableFile extends LeafPersistable<File> {
  private File myFile;

  protected void doClear() {
    myFile = null;
  }

  protected File doAccess() {
    return myFile;
  }

  protected File doCopy() {
    return myFile;
  }

  protected void doRestore(DataInput in) throws IOException, FormatException {
    String path = CompactChar.readString(in);
    myFile = path == null ? null : new File(path);
  }

  protected void doSet(File file) {
    myFile = file;
  }

  protected void doStore(DataOutput out) throws IOException {
    String path = myFile == null ? null : myFile.getAbsolutePath();
    CompactChar.writeString(out, path);
  }
}
