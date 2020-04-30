package com.almworks.engine.items;

import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ProductInformation;
import com.almworks.items.api.*;
import com.almworks.items.impl.DBConfiguration;
import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.items.util.delegate.DelegatingDatabase;
import com.almworks.items.wrapper.DatabaseWrapper;
import com.almworks.util.Env;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.threads.Computable;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.util.List;

class DatabaseComponent extends DatabaseWrapper<DelegatingDatabase> implements Startable {
  private static final String DB_NAME = "items";
  private static final String DB_EXTENSION = "db";
  public static final String DATABASE_FILE_NAME = DB_NAME + "." + DB_EXTENSION;

  private final WorkArea myWorkArea;
  private final DialogManager myDialogs;
  private final ApplicationManager myApplication;
  private final List<DatabaseCheck> myChecks;
  private final ProductInformation myProductInfo;

  DatabaseComponent(WorkArea workArea, DialogManager dialogs, DatabaseCheck[] checkers, ApplicationManager application, ProductInformation productInfo) {
    super(new DelegatingDatabase() {
      @Override
      protected void onWaitingDBCall() {
        LogHelper.error("Not initialized DB used");
      }
    });
    myWorkArea = workArea;
    myDialogs = dialogs;
    myApplication = application;
    myChecks = Collections15.unmodifiableListCopy(checkers);
    myProductInfo = productInfo;
  }

  private SQLiteDatabase createStartedDB() {
    File databaseFile = getDBFile();
    DBConfiguration configuration = DBConfiguration.createDefault(databaseFile);
    File tempDir = Env.isWindows() ? myWorkArea.getTempDir() : null;
    SQLiteDatabase db = new SQLiteDatabase(databaseFile, tempDir, configuration);
    db.start();
    return db;
  }

  private File getDBFile() {
    return new File(myWorkArea.getRootDir(), DATABASE_FILE_NAME);
  }

  private SQLiteDatabase createDB() {
    boolean needsCheck = getDBFile().exists();
    SQLiteDatabase db = createStartedDB();
    if (!needsCheck) return db;
    DatabaseCheck.DBProblems problem = checkDatabase(db);
    if (problem == null) {
      LogHelper.error("Check DB failed to run");
      return db;
    }
    String fatal = problem.getFatalProblem();
    if (fatal != null) {
      LogHelper.debug("DB-startup: clean DB required", fatal);
      db.stop();
      SQLiteDatabase cleanDB = createCleanDB(fatal);
      if (cleanDB == null) {
        LogHelper.error("Failed to clean DB");
        throw new Failure("Failed to clean DB");
      }
      return cleanDB;
    }
    final List<Procedure<DBWriter>> fixes = problem.getFixes();
    if (!fixes.isEmpty()) {
      LogHelper.debug("DB-startup: fixes require", fixes);
      DBResult<Object> result = db.writeForeground(new WriteTransaction<Object>() {
        @Override
        public Object transaction(DBWriter writer) throws DBOperationCancelledException {
          for (Procedure<DBWriter> fix : fixes) {
            LogHelper.debug("DB-startup: running fix", fix);
            fix.invoke(writer);
          }
          return null;
        }
      });
      result.waitForCompletion();
      if (!result.isSuccessful()) LogHelper.error("Failed to fix DB", result.getError());
    } else LogHelper.debug("DB-startup: no fixes required. Startup done.");
    return db;
  }

  private DatabaseCheck.DBProblems checkDatabase(SQLiteDatabase db) {
    return db.readForeground(new ReadTransaction<DatabaseCheck.DBProblems>() {
        @Override
        public DatabaseCheck.DBProblems transaction(DBReader reader) throws DBOperationCancelledException {
          DatabaseCheck.DBProblems problems = new DatabaseCheck.DBProblems();
          for (DatabaseCheck check : myChecks) {
            try {
              check.check(reader, problems);
            } catch (DBOperationCancelledException e) {
              if (problems.getFatalProblem() != null)
                return problems;
              LogHelper.error(check, e);
            }
          }
          return problems;
        }
      }).waitForCompletion();
  }

  private SQLiteDatabase createCleanDB(final String fatalMessage) {
    final ExportTags exportTags = createExportTags();
    CleanDBForm form = ThreadGate.AWT_IMMEDIATE.compute(new Computable<CleanDBForm>() {
      @Override
      public CleanDBForm compute() {
        return CleanDBForm.showDialog(myDialogs, fatalMessage);
      }
    });
    if (!form.isErase()) {
      LogHelper.debug("Clear DB cancelled");
      myApplication.forceExit();
      return null;
    }
    if (exportTags != null) exportTags.runSafe();
    final File backup;
    if (form.isBackup()) {
      backup = createBackup();
      if (backup == null) {
        LogHelper.error("Backup failed");
        ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
          @Override
          public void run() {
            myDialogs.showErrorMessage("Database Backup", "Failed to back due to unknown reason. Application is going to terminate.");
            myApplication.forceExit();
          }
        });
        return null;
      }
    } else backup = null;
    final boolean deleted;
    File dbFile = getDBFile();
    //noinspection SimplifiableIfStatement
    if (dbFile.exists()) deleted = dbFile.delete();
    else deleted = true;
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      @Override
      public void run() {
        if (!deleted) {
          myDialogs.showErrorMessage("Clear Database", "Failed to delete database file. Application is going to terminate.");
          myApplication.forceExit();
        } else 
          BackupMessage.showMessage(myDialogs, backup, exportTags != null ? exportTags.getTargetFileName() : "");
      }
    });
    if (deleted) return createStartedDB();
    LogHelper.error("Failed to delete DB file");
    return null;
  }

  @Nullable
  private ExportTags createExportTags() {
    File tagFile = pickTagFile(myWorkArea.getRootDir(), "tags." + myProductInfo.getBuildNumber().toDisplayableString(), ".txt");
    return tagFile == null ? null : ExportTags.create(tagFile, myWorkArea);
  }

  private static File pickTagFile(File rootDir, String stem, String suffix) {
    IOException lastException = null;
    for (int i = 0; i < 1000; ++i) {
      File file = new File(rootDir, stem + (i == 0 ? "" : "-" + i) + suffix);
      if (!file.exists() || file.canWrite()) {
        try {
          file = file.getCanonicalFile();
        } catch (IOException e) {
          lastException = e;
          continue;
        }
        return file;
      }
    }
    LogHelper.warning("Cannot create export tag file", lastException == null ? "" : lastException);
    return null;
  }

  private File createBackup() {
    File dbFile = getDBFile();
    int num = 0;
    int failures = 0;
    while (true) {
      num++;
      String backupName = DB_NAME + num + "." + DB_EXTENSION;
      File file = new File(myWorkArea.getRootDir(), backupName);
      if (file.exists()) continue;
      boolean success = dbFile.renameTo(file);
      if (success) return file;
      failures++;
      if (failures > 3) return null;
    }
  }

  @Override
  public void start() {
    SQLiteDatabase db = createDB();
    if (db == null) throw new Failure();
    db.setLongHousekeepingAllowed(true);
    initDatabase(db);
    Database result = getDelegator().replaceWaitingState(db);
    if (result != db) db.stop();
  }

  private void initDatabase(SQLiteDatabase db) {
    DBResult<Object> result = db.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        for (DatabaseCheck check : myChecks) {
          check.init(writer);
        }
        return null;
      }
    });
    result.waitForCompletion();
    LogHelper.assertError(result.isSuccessful(), "DB init failed", result.getError());
    result.waitForCompletion();
  }

  @Override
  public void stop() {
    Database db = getDelegator().setNullDB();
    if (db instanceof SQLiteDatabase) ((SQLiteDatabase) db).stop();
  }
}
