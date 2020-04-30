package com.almworks.store;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StorerTests extends StorerFixture {

  public void testStorer() throws IOException, InterruptedException {
    timing();
    byte[] data1 = createTestData(100, 1);
    byte[] data2 = createTestData(1000, 1);
    byte[] data3 = createTestData(10000, 1);
    timing();
    myFile.store("haba.1", data1);
    timing();
    myFile.store("haba.2", data2);
    timing();
    myFile.store("haba.3", data3);
    timing();
    myFile.store("haba", "haba".getBytes());

    myFile = new FileStorer(myWorkArea);
    byte[] ndata1 = myFile.load("haba.1");
    timing();
    byte[] ndata2 = myFile.load("haba.2");
    timing();
    byte[] ndata3 = myFile.load("haba.3");
    timing();
    assertTrue(Arrays.equals(data1, ndata1));
    timing();
    assertTrue(Arrays.equals(data2, ndata2));
    timing();
    assertTrue(Arrays.equals(data3, ndata3));
    timing();

    assertEquals("haba", new String(myFile.load("haba")));
    timing();

    assertEquals(null, myFile.load("inexistent"));
  }


  public void testRebuildIndex() throws IOException, InterruptedException {
    int COUNT = 50;
    byte[] data1 = createTestData(100, 1);
    byte[] data2 = createTestData(1000, 1);
    byte[] data3 = createTestData(10000, 1);
    myFile.store("haba.1", data1);
    myFile.store("haba.2", data2);
    myFile.store("haba.3", data3);
    for (int i = 0; i < COUNT; i++)
      myFile.store("h." + i, ("h." + i).getBytes());
    File indexFile = new File(myWorkArea.getStorerDir(), FileStorer.INDEX_FILE_NAME);
    assertTrue(indexFile.exists());
    assertTrue(indexFile.delete());
    myFile = new FileStorer(myWorkArea);
    byte[] ndata1 = myFile.load("haba.1");
    byte[] ndata2 = myFile.load("haba.2");
    byte[] ndata3 = myFile.load("haba.3");
    assertTrue(Arrays.equals(data1, ndata1));
    assertTrue(Arrays.equals(data2, ndata2));
    assertTrue(Arrays.equals(data3, ndata3));
    for (int i = 0; i < COUNT; i++) {
      String s = "h." + i;
      assertEquals(s, new String(myFile.load(s)));
    }
  }

  public void testClear() throws IOException, InterruptedException {
    myFile.store("haba", "haba".getBytes());
    assertEquals("haba", new String(myFile.load("haba")));
    myFile.clear("haba");
    assertEquals(null, myFile.load("haba"));
  }


/*
  public void testPerformance() throws IOException {
    for (int i = 0; i < 100; i++) {
      TestWorkArea workArea = new TestWorkArea();
      myStorer = new FileStorer(workArea);
      myStorer.store("haba", "haba".getBytes());
      workArea.cleanUp();
    }
  }
*/

}
