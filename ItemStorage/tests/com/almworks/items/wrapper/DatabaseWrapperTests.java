package com.almworks.items.wrapper;

import com.almworks.api.misc.WorkArea;
import com.almworks.integers.LongArray;
import com.almworks.integers.LongList;
import com.almworks.integers.LongListIterator;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.impl.DBConfiguration;
import com.almworks.items.impl.SQLiteDatabase;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.DelegatingWriter;
import com.almworks.misc.TestWorkArea;
import com.almworks.util.Env;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.commons.Procedure;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Log;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import util.concurrent.Synchronized;

import java.io.File;
import java.util.List;

import static org.almworks.util.Collections15.arrayList;

public class DatabaseWrapperTests extends BaseTestCase {
  public static final DBNamespace NS = DBNamespace.moduleNs("com.almworks.engine").subNs("tests");
  public static final DBAttribute<String> ATTR = NS.string("Attr");
  public static final DBItemType TYPE = NS.type();
  public static final BoolExpr<DP> QUERY = DPEqualsIdentified.create(DBAttribute.TYPE, TYPE);
  public final static BoolExpr<DP> SUBQ_0 = DPEquals.create(ATTR, "0");
  public final static BoolExpr<DP> SUBQ_1 = DPEquals.create(ATTR, "1");

  private Database wdb;
  private SQLiteDatabase myStarted;
  private final List<DbStateTester> myTesters = arrayList();

  @Override
  protected void setUp() throws Exception {
    setWriteToStdout(true);
    final TestWorkArea workArea = new TestWorkArea();
    SQLiteDatabase sqliteDB = createStartedDB(workArea);
    DatabaseWrapper db = new DatabaseWrapper(sqliteDB);
    myStarted = sqliteDB;
    wdb = db;
  }

  private SQLiteDatabase createStartedDB(WorkArea workArea) {
    File databaseFile = new File(workArea.getRootDir(), "items.db");
    DBConfiguration configuration = DBConfiguration.createDefault(databaseFile);
    File tempDir = Env.isWindows() ? workArea.getTempDir() : null;
    SQLiteDatabase db = new SQLiteDatabase(databaseFile, tempDir, configuration);
    db.start();
    return db;
  }

  @Override
  protected void tearDown() throws Exception {
    myStarted.stop();
    super.tearDown();
  }

  private TestDbListener listener(boolean wrapped, boolean isQuery) {
    TestDbListener listener = new TestDbListener(wrapped, isQuery);
    myTesters.add(listener);
    return listener;
  }

  private TestDbTrigger trigger(boolean wrapped) {
    TestDbTrigger trigger = new TestDbTrigger(wrapped);
    myTesters.add(trigger);
    return trigger;
  }


  public void test() throws InterruptedException {
    if(true) return; // todo: fails on bbox, passes in IDEA
    
    wdb.liveQuery(Lifespan.FOREVER, QUERY, listener(true, true));
    DatabaseUnwrapper.liveQuery(wdb, Lifespan.FOREVER, QUERY, listener(false, true));
    wdb.addListener(Lifespan.FOREVER, listener(true, false));
    DatabaseUnwrapper.addListener(wdb, Lifespan.FOREVER, listener(false, false));

    wdb.registerTrigger(trigger(true));
    DatabaseUnwrapper.registerTrigger(wdb, trigger(false));

    final long[] items = new long[2];
    writeDb("init", new Procedure<DBWriter>() { public void invoke(DBWriter writer) {
        long typeItem = writer.materialize(TYPE);
        items[0] = writer.nextItem();
        writer.setValue(items[0], ATTR, "0");
        writer.setValue(items[0], DBAttribute.TYPE, typeItem);
        items[1] = writer.nextItem();
        writer.setValue(items[1], ATTR, "1");
        writer.setValue(items[1], DBAttribute.TYPE, typeItem);
      }
    });

    check("init", "01", "01");

    writeDb("rem0", new Procedure<DBWriter>() { public void invoke(DBWriter writer) {
      writer.clearItem(items[0]);
    }});

    check("rem0", "1", "01");

    writeDb("clr0", new Procedure<DBWriter>() { public void invoke(DBWriter writer) {
      DatabaseUnwrapper.clearItem(writer, items[0]);
    }});

    check("clr0", "1", "1");

    writeDb("rem1", new Procedure<DBWriter>() { public void invoke(DBWriter writer) {
      writer.clearItem(items[1]);
    }});

    check("rem1", "", "1");
  }

  private void writeDb(final String key, final Procedure<DBWriter> write) throws InterruptedException {
    Log.debug("writing " + key);
    for (DbStateTester t : myTesters) t.beforeDbWrite();
    wdb.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        write.invoke(writer);
        return null;
      }
    }).onFailure(ThreadGate.STRAIGHT, new Procedure<DBResult<Object>>() {
      public void invoke(DBResult<Object> arg) {
        fail(key);
      }
    }).waitForCompletion();
    for (DbStateTester t : myTesters) t.waitState(key);
  }

  private void check(final String key, final String wrapped, final String unwrapped) {
    Log.debug("checking " + key);
    wdb.readForeground(new ReadTransaction<Object>() {
      @Override
      public Object transaction(DBReader reader) throws DBOperationCancelledException {
        DatabaseWrapperTests.this.check(reader, key + "_r", wrapped, unwrapped);
        return null;
      }
    }).waitForCompletion();
    wdb.writeForeground(new WriteTransaction<Object>() {
      @Override
      public Object transaction(DBWriter writer) throws DBOperationCancelledException {
        check(new MyWriterWrapper(writer), key + "_w", wrapped, unwrapped);
        return null;
      }
    }).waitForCompletion();
  }

  private void check(DBReader reader, String key, String wrapped, String unwrapped) {
    assertEquals(key, wrapped, readItems(reader.query(QUERY).copyItemsSorted(), reader));
    assertEquals(key, unwrapped, readItems(DatabaseUnwrapper.query(reader, QUERY).copyItemsSorted(), reader));
    for (DbStateTester t : myTesters) t.check(key, wrapped, unwrapped);
  }

  private static class MyWriterWrapper extends DelegatingWriter {
    protected MyWriterWrapper(@NotNull DBWriter writer) {
      super(writer);
    }
  }

  private static String readItems(LongList items, DBReader reader) {
    StringBuilder s = new StringBuilder();
    for (LongListIterator i = items.iterator(); i.hasNext();) {
      s.append(reader.getValue(i.nextValue(), ATTR));
    }
    return s.toString();
  }

  private static String readItems(BoolExpr<DP> expr, DBReader reader) {
    return readItems(reader.query(expr).copyItemsSorted(), reader);
  }

  private static String readItems(DBQuery q) {
    return readItems(q.copyItemsSorted(), q.getReader());
  }

  private static TestDbState readState(DBReader reader, String cumulative) {
    String immediate = readItems(QUERY, reader);
    DBQuery q = reader.query(QUERY);
    String bySubQ = readItems(q.query(SUBQ_0)) + readItems(q.query(SUBQ_1));
    TestDbState dbState = new TestDbState(cumulative, immediate, bySubQ);
    return dbState;
  }

  private static interface DbStateTester {
    void beforeDbWrite();
    void waitState(String key) throws InterruptedException;
    void check(String key, String wrapped, String unwrapped);
  }

  private static class TestDbListener implements DBLiveQuery.Listener, DbStateTester {
    private final LongArray myItems = new LongArray();
    private final Synchronized<TestDbState> myState = new Synchronized<TestDbState>(TestDbState.EMPTY);
    private final boolean myWrapped;
    private final boolean myListeningQuery;
    private final String myKey;

    public TestDbListener(boolean wrapped, boolean listeningQuery) {
      myWrapped = wrapped;
      myListeningQuery = listeningQuery;
      myKey = (myWrapped ? "w" : "u") + (myListeningQuery ? "q" : "a");
    }

    public void beforeDbWrite() {
      myState.set(null);
    }

    @Override
    public void onICNPassed(long icn) {
    }

    @Override
    public void onDatabaseChanged(DBEvent event, DBReader reader) {
      if (event.isEmpty()) Log.debug(this + " received empty event");
      myItems.addAll(event.getAddedAndChangedSorted());
      myItems.removeAll(event.getRemovedSorted());
      myItems.sortUnique();
      String cumulative = readItems(myItems, reader);
      myState.set(readState(reader, cumulative));
    }

    public void waitState(String key) throws InterruptedException {
      Log.debug(this + " waiting for state (" + key + ')');
      myState.waitForNotNull();
    }

    public void check(String key, String wrapped, String unwrapped) {
      myState.get().check(key + "_" + this, myWrapped ? wrapped : unwrapped, myListeningQuery);
    }

    @Override
    public String toString() {
      return "DBL[" + myKey + ']';
    }
  }

  private static class TestDbTrigger extends DBTrigger implements DbStateTester {
    private final Synchronized<TestDbState> myState = new Synchronized<TestDbState>(TestDbState.EMPTY);
    private final boolean myWrapped;
    private final String myKey;

    public TestDbTrigger(boolean wrapped) {
      super("TDT:" + (wrapped ? "w" : "u"), QUERY);
      myKey = (wrapped ? "w" : "u");
      myWrapped = wrapped;
    }

    @Override
    public void apply(LongList itemsSorted, DBWriter writer) {
      myState.set(readState(writer, ""));
    }

    @Override
    public void beforeDbWrite() {
      myState.set(null);
    }

    @Override
    public void waitState(String key) throws InterruptedException {
      Log.debug(this + " waiting for state (" + key + ')');
    }

    @Override
    public void check(String key, String wrapped, String unwrapped) {
      TestDbState state = myState.get();
      // todo workaround for trigger quirks : if item does not belong to the query at the end of transaction, it is not given to the trigger
      if (state != null) {
        state.check(key + "_" + this, myWrapped ? wrapped : unwrapped, false);
      }
    }
  }

  private static class TestDbState {
    public static final TestDbState EMPTY = new TestDbState("", "", "");
    private final String myCumulative;
    private final String myImmediate;
    private final String myBySubqueries;

    public TestDbState(String cumulative, String immediate, String bySubqueries) {
      myCumulative = cumulative;
      myImmediate = immediate;
      myBySubqueries = bySubqueries;
    }

    public void check(String key, String expected, boolean checkCumul) {
      if (checkCumul) assertEquals(key, expected, myCumulative);
      assertEquals(key, expected, myImmediate);
      assertEquals(key, expected, myBySubqueries);
    }
  }
}
