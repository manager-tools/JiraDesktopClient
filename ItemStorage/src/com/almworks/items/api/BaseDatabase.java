package com.almworks.items.api;

import com.almworks.util.bool.BoolExpr;
import com.almworks.util.io.IOUtils;
import org.almworks.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public abstract class BaseDatabase implements Database {
  public DBFilter filter(BoolExpr<DP> expr) {
    return new DBFilter(this, expr);
  }

  public <T> DBResult<T> readForeground(ReadTransaction<T> transaction) {
    return read(DBPriority.FOREGROUND, transaction);
  }

  public <T> DBResult<T> readBackground(ReadTransaction<T> transaction) {
    return read(DBPriority.BACKGROUND, transaction);
  }

  public <T> DBResult<T> writeBackground(WriteTransaction<T> transaction) {
    return write(DBPriority.BACKGROUND, transaction);
  }

  public <T> DBResult<T> writeForeground(WriteTransaction<T> transaction) {
    return write(DBPriority.FOREGROUND, transaction);
  }

  public void dump(String filename) {
    PrintStream writer = null;
    try {
      writer = new PrintStream(filename, "UTF-8");
      dump(writer);
    } catch (IOException e) {
      Log.warn(e);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(writer);
    }
  }

  public String dumpString() {
    PrintStream writer = null;
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      writer = new PrintStream(out, false, "UTF-8");
      dump(writer);
      writer.close();
      return out.toString("UTF-8");
    } catch (IOException e) {
      Log.warn(e);
    } finally {
      IOUtils.closeStreamIgnoreExceptions(writer);
    }
    return "";
  }
}
