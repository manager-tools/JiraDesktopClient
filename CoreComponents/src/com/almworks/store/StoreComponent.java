package com.almworks.store;

import com.almworks.api.container.ActorSelector;
import com.almworks.api.container.ContainerPath;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.store.Store;
import com.almworks.sqlite4java.SQLite;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.store.sqlite.SQLiteStorer;
import org.almworks.util.Failure;
import org.almworks.util.Log;

import java.io.File;

/**
 * :todoc:
 *
 * @author sereda
 */
public class StoreComponent implements ActorSelector<Store> {
  private final Storer myStorer;

  public StoreComponent(WorkArea workArea) {
    myStorer = createStorer(workArea);
  }

  private Storer createStorer(WorkArea workArea) {
    if(checkSQLite()) {
//      adjustSQLiteLoggingLevel();
      return new SQLiteStorer(getSQLiteStoreFile(workArea), null);
    } else {
      throw new Failure("Cannot start SQLite");
    }
  }

  private void adjustSQLiteLoggingLevel() {
    // THIS IS NOT THE RIGHT PLACE TO DO THIS

//    if(!Env.getBoolean("sqlite4java.verbose")) {
//      Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.WARNING);
//    }
  }

  private File getSQLiteStoreFile(WorkArea workArea) {
    return new File(new File(workArea.getStorerDir(), "store2"), "store2.db");
  }

  private static boolean checkSQLite() {
    try {
      SQLite.loadLibrary();
      return true;
    } catch(SQLiteException e) {
      Log.warn(e);
      return false;
    }
  }

  @Override
  public Store selectImplementation(ContainerPath selectionKey) {
    ContainerPath parent = selectionKey.getParentPath();
    String componentId = selectionKey.getName();
    return parent == null ?
      new StoreImpl(myStorer, componentId) :
      selectImplementation(parent).getSubStore(componentId);
  }
}
