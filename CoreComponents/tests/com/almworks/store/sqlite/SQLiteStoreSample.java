package com.almworks.store.sqlite;

import com.almworks.api.store.StoreUtils;
import com.almworks.store.StoreImpl;
import com.almworks.util.io.persist.PersistableString;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: dyoma
 * Date: Apr 30, 2010
 * Time: 2:23:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class SQLiteStoreSample {
  public static void main(String[] args) throws IOException {
    Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.FINE);
    File file = File.createTempFile("sqlite", "test");
    file.delete();
    file.deleteOnExit();
    SQLiteStorer storer = new SQLiteStorer(file, null);
    StoreImpl store = new StoreImpl(storer, "a");
    System.out.println("Should be null " + store.access("a").load());
    storeValue(store, "str");
    printContent(store, "str");
    storeValue(store, "abc");
    printContent(store, "abc");
    storeValue(store, null);
    printContent(store, null);
    storer.stop();
  }

  private static void storeValue(StoreImpl store, String value) {
    PersistableString out = new PersistableString(value);
    StoreUtils.storePersistable(store, "a", out);
  }

  private static void printContent(StoreImpl store, String expected) {
    PersistableString in = new PersistableString(null);
    StoreUtils.restorePersistable(store, "a", in);
    System.out.println("Should be " + expected + ": " + in.copy());
  }
}
