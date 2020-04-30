package com.almworks.store;

import com.almworks.api.store.StoreFeature;
import com.almworks.util.Pair;
import com.almworks.util.fileformats.FileFormatException;
import com.almworks.util.tests.BaseTestCase;
import util.external.UID;

import java.io.IOException;
import java.util.Arrays;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreDataFileTests extends BaseTestCase {
  private StoreDataFile myFile;
  private static final int BLOCK_SIZE = 300;

  protected void setUp() throws Exception {
    super.setUp();
    myFile = new StoreDataFile(createFileName());
  }

  protected void tearDown() throws Exception {
    if (myFile != null) {
      try {
        myFile.drop();
      } catch (Throwable t) {
      }
      myFile = null;
    }
    super.tearDown();
  }

  public void testCreation() throws IOException, FileFormatException {
    myFile.create(BLOCK_SIZE);
    UID uid = myFile.getUID();
    myFile.close();

    myFile = new StoreDataFile(myFile.getPath());
    myFile.open();
    assertEquals(uid, myFile.getUID());
  }

  public void testWriteAndRead() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full,
    FileFormatException, StoreDataFile.BlockIsFree {

    String PATH = "haba.haba";
    myFile.create(BLOCK_SIZE);
    byte[] data = createTestData(BLOCK_SIZE / 2, 1);
    int block = myFile.write(PATH, data);
    assertFalse(myFile.isBlockFree(block));
    Pair<String, byte[]> pair = myFile.read(block);
    assertEquals(PATH, pair.getFirst());
    assertTrue(Arrays.equals(data, pair.getSecond()));
  }

  public void testTwoBlocks() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full, FileFormatException,
    StoreDataFile.BlockIsFree {
    byte[] data1 = {99};
    byte[] data2 = {100};
    myFile.create(BLOCK_SIZE);
    int b1 = myFile.write("a", data1);
    int b2 = myFile.write("b", data2);
    myFile.close();

    myFile = new StoreDataFile(myFile.getPath());
    myFile.open();
    Pair<String, byte[]> pair1 = myFile.read(b1);
    Pair<String, byte[]> pair2 = myFile.read(b2);
    assertEquals("a", pair1.getFirst());
    assertEquals("b", pair2.getFirst());
    assertTrue(Arrays.equals(data1, pair1.getSecond()));
    assertTrue(Arrays.equals(data2, pair2.getSecond()));
  }

  public void testFullFile() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full {
    myFile.create(BLOCK_SIZE, 2, new SDFFormatV1());
    myFile.write("x", createTestData(100, 1));
    myFile.write("y", createTestData(100, 2));
    try {
      myFile.write("z", createTestData(100, 3));
      fail("successfully written extra block");
    } catch (StoreDataFile.Full e) {
      // ok!
    }
  }

  public void testBlockOverflow() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full {
    myFile.create(BLOCK_SIZE);
    try {
      myFile.write("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", createTestData(BLOCK_SIZE - 12, 1));
      fail("successfully written too much data");
    } catch (StoreDataFile.BlockOverflow blockOverflow) {
      // normal
    }
    try {
      myFile.write("aaaaaaaaaaaaaaaaaaaaaaaaaaaaa", createTestData(BLOCK_SIZE * 2, 2));
      fail("successfully written too much data");
    } catch (StoreDataFile.BlockOverflow blockOverflow) {
      // normal
    }
  }

  public void testBlockRemoval() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full,
    StoreDataFile.BlockIsFree, FileFormatException {

    myFile.create(BLOCK_SIZE);
    int b1 = myFile.write("x.y", createTestData(100, 1));
    int b2 = myFile.write("x.y.z", createTestData(100, 2));
    int b3 = myFile.write("x", createTestData(100, 3));
    myFile.free(b2);
    try {
      myFile.free(b2);
      fail("freed empty block");
    } catch (StoreDataFile.BlockIsFree e) {
      // ok!
    }

    try {
      myFile.read(b2);
      fail("read empty block");
    } catch (StoreDataFile.BlockIsFree e) {
      // ok!
    }

    int b4 = myFile.write("d.e.f", createTestData(50, 4));
    assertEquals(b2, b4);

    assertEquals("x.y", myFile.read(b1).getFirst());
    assertEquals("d.e.f", myFile.read(b2).getFirst());
    assertEquals("x", myFile.read(b3).getFirst());
  }

  public void testMassiveOperations() throws IOException, StoreDataFile.BlockOverflow, StoreDataFile.Full,
    FileFormatException, StoreDataFile.BlockIsFree {
    int BLOCK_COUNT = 50;
    myFile.create(BLOCK_SIZE, BLOCK_COUNT, new SDFFormatV1());
    int[] order = new int[BLOCK_COUNT];
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < BLOCK_COUNT; i++) {
      String storePath = ":" + i;
      order[i] =
        myFile.write(storePath, createTestData(BLOCK_SIZE - 12 - storePath.length(), i), StoreFeature.PLAIN_STORE);
    }
    long writeTime = System.currentTimeMillis() - startTime;
    // System.out.println("write time " + writeTime + "ms, or " + (writeTime / BLOCK_COUNT) + "ms/block");
    int count = BLOCK_COUNT;
    startTime = System.currentTimeMillis();
    myFile.forceSync();
    long lastLength = myFile.getPath().length();
    while (count > 0) {
      for (int i = count / 2; i < count; i++) {
        myFile.read(i);
        myFile.free(i);
      }
      count = count / 2;
      myFile.forceSync();
      long length = myFile.getPath().length();
      assertTrue("length = " + length + "; lastLength = " + lastLength, length < lastLength);
      lastLength = length;
    }
    long readClearTime = System.currentTimeMillis() - startTime;
    // System.out.println("read-clear time " + readClearTime + "ms, or " + (readClearTime / BLOCK_COUNT) + "ms/block");
  }
}
