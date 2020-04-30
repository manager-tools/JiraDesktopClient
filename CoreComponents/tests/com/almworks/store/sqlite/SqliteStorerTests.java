package com.almworks.store.sqlite;

import com.almworks.api.store.StoreFeature;
import com.almworks.store.StorerFixture;

import java.io.IOException;
import java.util.Arrays;

public class SqliteStorerTests extends StorerFixture {
  private SQLiteStorer mySQL;

  @Override
  protected void tearDown() throws Exception {
    if (mySQL != null) mySQL.stop();
    super.tearDown();
  }

  public void testSecure() throws IOException, InterruptedException {
    mySQL = new SQLiteStorer(null, null);
    byte[] bytes = {1, 2, 4};
    mySQL.store("a", bytes, StoreFeature.SECURE_STORE);
    mySQL.store("b", bytes, StoreFeature.EMPTY_FEATURES_ARRAY);
    assertNull(mySQL.load("xxx", StoreFeature.EMPTY_FEATURES_ARRAY));
    assertEquals(0, mySQL.getPendingCount());
    assertTrue(Arrays.equals(mySQL.load("a", StoreFeature.SECURE_STORE), bytes));
    assertTrue(Arrays.equals(mySQL.load("b", StoreFeature.EMPTY_FEATURES_ARRAY), bytes));

    assertEquals(0, mySQL.getPendingCount());
    StoreJob.Read aRead = mySQL.performRead("a");
    StoreJob.Read bRead = mySQL.performRead("b");
    assertEquals(0, bRead.getFeatures().length);
    assertEncripted(true, aRead);
    assertEncripted(false, bRead);
    assertTrue(Arrays.equals(bytes, bRead.getData()));
    assertFalse(Arrays.equals(bytes, aRead.getData()));
  }

  private void assertEncripted(boolean expectedEncripted, StoreJob.Read read) {
    assertTrue(expectedEncripted == Arrays.equals(read.getFeatures(), new StoreFeature[]{StoreFeature.ENCRYPTED}));
  }

  public void testMigration() throws IOException, InterruptedException {
    byte[] bytes = {1, 2, 4};
    myFile.store("a", bytes, StoreFeature.SECURE_STORE);
    myFile.store("b", bytes, StoreFeature.EMPTY_FEATURES_ARRAY);
    assertTrue(Arrays.equals(bytes, myFile.load("a", StoreFeature.SECURE_STORE)));
    assertTrue(Arrays.equals(StoreFeature.SECURE_STORE, myFile.getFeaures("a")));
    mySQL = new SQLiteStorer(null, myFile);
    mySQL.store("c", bytes, StoreFeature.SECURE_STORE);
    assertTrue(Arrays.equals(bytes, mySQL.load("b", StoreFeature.EMPTY_FEATURES_ARRAY)));
    assertTrue(Arrays.equals(bytes, mySQL.load("a", StoreFeature.SECURE_STORE)));
    assertEquals(0, mySQL.getPendingCount());
    byte[] secureBytes = mySQL.performRead("c").getData();
    StoreJob.Read aRead = mySQL.performRead("a");
    assertEncripted(false, mySQL.performRead("b"));
    assertEncripted(true, mySQL.performRead("c"));
    assertEncripted(true, aRead);
    assertTrue(Arrays.equals(secureBytes, aRead.getData()));
  }

  public void testOverwriteFeatures() throws IOException, InterruptedException {
    mySQL = new SQLiteStorer(null, null);
    byte[] bytes = {1, 2, 4};
    mySQL.store("a", bytes, StoreFeature.EMPTY_FEATURES_ARRAY);
    assertTrue(Arrays.equals(bytes, mySQL.load("a", StoreFeature.SECURE_STORE)));
    assertTrue(Arrays.equals(bytes, mySQL.performRead("a").getData()));
    mySQL.store("a", bytes, StoreFeature.SECURE_STORE);
    assertTrue(Arrays.equals(bytes, mySQL.load("a", StoreFeature.SECURE_STORE)));
    assertFalse(Arrays.equals(bytes, mySQL.performRead("a").getData()));
    assertTrue(Arrays.equals(bytes, mySQL.load("a", StoreFeature.EMPTY_FEATURES_ARRAY))); // Legacy behaviour
  }
}
