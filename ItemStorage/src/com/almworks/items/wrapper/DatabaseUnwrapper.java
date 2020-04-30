package com.almworks.items.wrapper;

import com.almworks.items.api.*;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.Log;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;

/**
 * Use methods of this class only if you are sure that you need unwrapped queries.
 * @see ItemStorageAdaptor#wrapExpr(com.almworks.util.bool.BoolExpr Query wrapping
 * */
public class DatabaseUnwrapper {
  public static DBQuery query(DBReader reader, BoolExpr<DP> expr) {
    Object unwrappedReaderObj = reader.getTransactionCache().get(DatabaseWrapperPrivateUtil.UNWRAPPED_READER);
    // if null, it is not wrapped by this package
    DBReader unwrappedReader = unwrappedReaderObj instanceof DBReader ? (DBReader) unwrappedReaderObj : reader;
    return unwrappedReader.query(expr);
  }

  public static DBQuery query(DBFilter filter, DBReader reader) {
    return query(reader, filter.getExpr());
  }

  public static DBLiveQuery liveQuery(Database db, Lifespan lifespan, BoolExpr<DP> expr, DBLiveQuery.Listener listener) {
    if (db instanceof DatabaseWrapper) {
      return ((DatabaseWrapper) db).liveQueryUnwrapped(lifespan, expr, listener);
    } else {
      Log.error("Unwrapped database");
      return db.liveQuery(lifespan, expr, listener);
    }
  }

  public static void addListener(Database db, Lifespan lifespan, DBListener listener) {
    if (db instanceof DatabaseWrapper) {
      ((DatabaseWrapper)db).addListenerUnwrapped(lifespan, listener);
    } else {
      Log.error("Unwrapped database");
      db.addListener(lifespan, listener);
    }
  }

  public static void registerTrigger(Database db, DBTrigger trigger) {
    if (db instanceof DatabaseWrapper) {
      ((DatabaseWrapper)db).registerTriggerUnwrapped(trigger);
    } else {
      Log.error("Unwrapped database");
      db.registerTrigger(trigger);
    }
  }

  public static void clearItem(DBWriter writer, long item) {
    Object unwrappedWriterObj = writer.getTransactionCache().get(DatabaseWrapperPrivateUtil.UNWRAPPED_WRITER);
    // if null, it is not wrapped by this package
    DBWriter unwrappedWriter = unwrappedWriterObj instanceof DBWriter ? (DBWriter) unwrappedWriterObj : writer;
    unwrappedWriter.clearItem(item);
  }
}

class DatabaseWrapperPrivateUtil {
  static final TypedKey<DBReader> UNWRAPPED_READER = TypedKey.create(DBReader.class);
  static final TypedKey<DBWriter> UNWRAPPED_WRITER = TypedKey.create(DBWriter.class);

  static DBReaderWrapper wrapReader(DBReader reader) {
    UNWRAPPED_READER.putTo(reader.getTransactionCache(), reader);
    return new DBReaderWrapper(reader);
  }

  static DBWriterWrapper wrapWriter(DBWriter writer) {
    UNWRAPPED_READER.putTo(writer.getTransactionCache(), writer);
    UNWRAPPED_WRITER.putTo(writer.getTransactionCache(), writer);
    return new DBWriterWrapper(writer);
  }
}