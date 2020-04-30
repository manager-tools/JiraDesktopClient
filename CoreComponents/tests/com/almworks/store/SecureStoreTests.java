package com.almworks.store;

import com.almworks.api.store.StoreFeature;

import java.io.IOException;
import java.util.Arrays;

/**
 * :todoc:
 *
 * @author sereda
 */
public class SecureStoreTests extends StorerFixture {
  public void testSecureStorer() throws IOException, InterruptedException {
    byte[] data1 = createTestData(100, 1);
    byte[] data2 = createTestData(1000, 1);
    byte[] data3 = createTestData(10000, 1);
    myFile.store("haba.1", data1, StoreFeature.SECURE_STORE);
    myFile.store("haba.2", data2, StoreFeature.SECURE_STORE);
    myFile.store("haba.3", data3, StoreFeature.SECURE_STORE);
    myFile.store("haba", "haba".getBytes());

    myFile = new FileStorer(myWorkArea);
    byte[] ndata1 = myFile.load("haba.1");
    byte[] ndata2 = myFile.load("haba.2");
    byte[] ndata3 = myFile.load("haba.3");
    assertTrue(Arrays.equals(data1, ndata1));
    assertTrue(Arrays.equals(data2, ndata2));
    assertTrue(Arrays.equals(data3, ndata3));

    assertEquals("haba", new String(myFile.load("haba")));
  }


  public void testOlderFormatDontAcceptSecureWrites() throws IOException {
    myFile = new FileStorer(myWorkArea, new SDFFormatV1());
    try {
      myFile.store("haba", "haba".getBytes(), StoreFeature.SECURE_STORE);
      fail("successfully stored secure data in a file that doesn't support security");
    } catch (UnsupportedOperationException e) {
      // normal
    }
  }
}
