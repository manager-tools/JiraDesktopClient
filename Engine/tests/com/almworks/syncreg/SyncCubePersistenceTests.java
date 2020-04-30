package com.almworks.syncreg;

import com.almworks.items.api.*;

import java.io.IOException;

public class SyncCubePersistenceTests extends MemoryDatabaseFixture {

  public void testPersistance() throws IOException {
    checkPersistence("");
    checkPersistence("1 +1");
    checkPersistence("1 +1; 2 +2; 3 +3; 4 +4; 5 +5; 6 +6; 7 +7; 8 +8");
    checkPersistence("1 -1; 2 -2; 3 -3; 4 -4; 5 -5; 6 -6; 7 -7; 8 -8");
    checkPersistence("1 +1", "1 +2", "1 +3", "1 +4", "2 -1", "2 -2", "2 -3", "2 -4");
    checkPersistence("1 +100000000001,100000000002,100000000003 -1,2,3");
    checkPersistence("1 +100000000001,100000000002,100000000003 -1,2,3", "2 +2; 3 -3");
    checkPersistence("1 +1", "2 +2; 3 +3", "2 +2,3; 3 +3,4", "5 +6,7,8; 6 -100,101,102; 7 -1");
  }

  private void checkPersistence(String ... cubes) throws IOException {
    final SyncCubeRegistryImpl r1 = new SyncCubeRegistryImpl();
    for (String cube : cubes) {
      r1.setSynced(SyncCubeRegistryImplTests.cube(cube));
    }
    db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        r1.save(writer);
        return null;
      }
    }).waitForCompletion();

    final SyncCubeRegistryImpl r2 = new SyncCubeRegistryImpl();
    db.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        r2.load(reader);
        return null;
      }
    }).waitForCompletion();

    assertTrue(r1.equalRegistry(r2));
  }
}
