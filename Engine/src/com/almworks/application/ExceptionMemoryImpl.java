package com.almworks.application;

import com.almworks.api.engine.Engine;
import com.almworks.api.exec.ExceptionHash;
import com.almworks.api.exec.ExceptionMemory;
import com.almworks.api.platform.ProductInformation;
import com.almworks.integers.LongIterator;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.wrapper.DatabaseUnwrapper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.concurrent.DetachLatch;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.NotNull;
import org.picocontainer.Startable;

import java.util.Collection;
import java.util.List;

public class ExceptionMemoryImpl implements ExceptionMemory, Startable {
  private static final DBNamespace NS = Engine.NS.subModule("exceptionMemory").subNs("ReportedException");

  private static final DBItemType TYPE_RECORD = NS.type();
  private static final DBAttribute<String> ATTR_HASH = NS.string("traceHash", "Trace Hash", false);
  private static final DBAttribute<String> ATTR_BUILD = NS.string("buildNo", "Build Number", false);

  private final Database myDatabase;
  private final String myCurrentBuild;
  private final List<ExceptionHash> myHashes = Collections15.arrayList();
  private final DetachLatch myLoadedLatch = new DetachLatch();

  public ExceptionMemoryImpl(Database database, ProductInformation productInfo) {
    myDatabase = database;
    myCurrentBuild = productInfo.getBuildNumber().toDisplayableString();
  }

  @Override
  public void start() {
    myDatabase.writeForeground(new WriteTransaction<List<ExceptionHash>>() {
      @Override
      public List<ExceptionHash> transaction(DBWriter w) throws DBOperationCancelledException {
        final List<ExceptionHash> records = Collections15.arrayList();
        final DBQuery query = DatabaseUnwrapper.query(w, DPEqualsIdentified.create(DBAttribute.TYPE, TYPE_RECORD));
        for(final LongIterator it = query.copyItemsSorted().iterator(); it.hasNext();) {
          final long item = it.nextValue();
          final String build = w.getValue(item, ATTR_BUILD);
          if(myCurrentBuild.equals(build)) {
            records.add(ExceptionHash.createHash(w.getValue(item, ATTR_HASH)));
          } else {
            DatabaseUnwrapper.clearItem(w, item);
          }
        }
        return records;
      }
    }).finallyDo(ThreadGate.LONG(this), new Procedure<List<ExceptionHash>>() {
      @Override
      public void invoke(List<ExceptionHash> arg) {
        if (arg != null) {
          synchronized (myHashes) {
            myHashes.addAll(arg);
          }
        }
        myLoadedLatch.detach();
      }
    });
  }

  @Override
  public void stop() {}

  @Override
  public boolean remembers(@NotNull ExceptionHash hash) {
    if(!myLoadedLatch.awaitOrInterrupt()) {
      return false;
    }
    synchronized(myHashes) {
      for(final ExceptionHash old : myHashes) {
        if(ExceptionHash.sameException(old, hash)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void remember(@NotNull final Collection<ExceptionHash> hashes) {
    if(!myLoadedLatch.awaitOrInterrupt()) {
      return;
    }
    myDatabase.writeForeground(new WriteTransaction<Void>() {
      @Override
      public Void transaction(DBWriter w) throws DBOperationCancelledException {
        for(final ExceptionHash r : hashes) {
          final long item = w.nextItem();
          w.setValue(item, DBAttribute.TYPE, w.materialize(TYPE_RECORD));
          w.setValue(item, ATTR_HASH, r.getTraceHash());
          w.setValue(item, ATTR_BUILD, myCurrentBuild);
        }
        return null;
      }
    }).onSuccess(ThreadGate.LONG(this), new Procedure<Void>() {
      @Override
      public void invoke(Void arg) {
        synchronized (myHashes) {
          myHashes.addAll(hashes);
        }
      }
    });
  }
}
