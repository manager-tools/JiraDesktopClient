package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.tests.BaseTestCase;
import com.almworks.util.tests.CollectionsCompare;

import java.io.IOException;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreIndexFileTests extends BaseTestCase {
  private StoreIndexFile myFile;
  private final CollectionsCompare compare = new CollectionsCompare();

  protected void setUp() throws Exception {
    super.setUp();
    myFile = new StoreIndexFile(createFileName());
  }

  protected void tearDown() throws Exception {
    myFile = null;
    super.tearDown();
  }

  public void testBasicContract() throws IOException, FileFormatException {
    StoreIndex index = new StoreIndex();
    index.addFileInfo(new StoreIndex.FileInfo("a1", 10000000000L, 2000000, 1000, StoreFeature.SECURE_STORE, 1));
    index.addFileInfo(new StoreIndex.FileInfo("b1", 1, 2, 3, StoreFeature.PLAIN_STORE, 2));
    index.addEntryInfo(new StoreIndex.EntryInfo("x.y.z", 0, 1));
    index.addEntryInfo(new StoreIndex.EntryInfo("v.w", 1, 2));
    index.addEntryInfo(new StoreIndex.EntryInfo("v.w.x", 1, 2));
    myFile.write(index);

    myFile = new StoreIndexFile(myFile.getPath());
    StoreIndex index2 = myFile.read();

    compare.order(index.getFiles(), index2.getFiles());
    compare.order(index.getEntries().keySet().toArray(), index2.getEntries().keySet().iterator());
    compare.order(index.getEntries().values().toArray(), index2.getEntries().values().iterator());
  }
}
