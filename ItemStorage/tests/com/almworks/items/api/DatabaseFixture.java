package com.almworks.items.api;

import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.util.exec.LongEventQueue;
import com.almworks.util.files.FileUtil;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.almworks.util.Collections15.arrayList;

public abstract class DatabaseFixture extends BaseTestCase {
  protected final List<SQLiteDatabase> databases = arrayList();
  protected final List<File> tempFiles = arrayList();
  protected final List<File> tempDirs = arrayList();

  protected DatabaseFixture() {
    super();
  }

  protected DatabaseFixture(int timeout) {
    super(timeout);
  }

  protected SQLiteDatabase createSQLiteMemoryDatabase() {
    SQLiteDatabase db = new SQLiteDatabase(null, null);
    db.start();
    databases.add(db);
    return db;
  }

  protected void enableSqlite4JavaLogging(boolean fine) {
    Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.FINE);
    setWriteToStdout(true, fine ? Level.FINE : Level.INFO);
  }

  protected SQLiteDatabase createFileDatabase() {
    try {
      File file = createFileName();
      tempFiles.add(file);
      tempFiles.add(new File(file.getPath() + "-journal"));
      File tempDir = new File(file.getParent(), file.getName() + "-tempdir");
      assertTrue(tempDir.mkdir());
      assertTrue(tempDir.isDirectory());
      tempDirs.add(tempDir);
      SQLiteDatabase db = new SQLiteDatabase(file, tempDir);
      db.start();
      databases.add(db);
      return db;
    } catch (IOException e) {
      throw new Error(e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    if ("true".equals(System.getProperty("sqlite4java.debug"))) {
      enableSqlite4JavaLogging(true);
    }
    super.setUp();
    LongEventQueue.installToContext();
  }

  @Override
  protected void tearDown() throws Exception {
    for (SQLiteDatabase database : databases) {
      try {
        database.stop();
      } catch (Exception e) {
        Log.error(e);
      }
    }
    for (File tempFile : tempFiles) {
      FileUtil.deleteFile(tempFile, true);
    }
    for (File tempDir : tempDirs) {
      FileUtil.deleteDirectoryWithContents(tempDir);
    }
    LongEventQueue.removeFromContext();
    super.tearDown();
  }

  protected void flushWriteQueue(Database db) {
    db.writeBackground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        return null;
      }
    }).waitForCompletion();
  }

  public static DBAttribute copyAttribute(DBAttribute attribute) {
    return DBAttribute.create(attribute.getId(), attribute.getName(), attribute.getScalarClass(),
      attribute.getComposition(), attribute.isPropagatingChange());
  }
}
