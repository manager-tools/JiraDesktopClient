package com.almworks.items.impl.sqlite;

import com.almworks.items.api.DBException;
import com.almworks.items.impl.DBConfiguration;
import com.almworks.items.impl.dbadapter.DBAnyTypeColumn;
import com.almworks.items.impl.dbadapter.DBLongColumn;
import com.almworks.items.impl.dbadapter.DBTable;
import com.almworks.items.impl.dbadapter.DBTableBuilder;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.Util;

import java.util.HashMap;

public class TableManagerTests extends BaseTestCase {
  private SQLiteConnection myConnection;
  private TableManager myTableManager;
  private TransactionContext myContext;
  private SessionContext mySessionContext;

//  static {
//    BaseDBTestCase.setupLogging(Level.FINE);
//  }

  protected void setUp() throws Exception {
    super.setUp();
    myConnection = new SQLiteConnection();
    myConnection.open();
    beginImmediate();
    Schema.validate(myContext);
    commit();
    mySessionContext = new SessionContext(new DatabaseContext(new DBConfiguration()));
    myTableManager = TableManager.getFromContext(new HashMap());
    beginImmediate();
  }

  private void commit() throws SQLiteException {
    myConnection.exec("COMMIT");
    myContext.dispose();
    myContext = null;
  }

  private void beginImmediate() throws SQLiteException {
    myConnection.exec("BEGIN IMMEDIATE");
    myContext = new TransactionContext(myConnection, mySessionContext, true);
  }

  protected void tearDown() throws Exception {
    if (!myConnection.getAutoCommit())
      commit();
    myTableManager = null;
    mySessionContext = null;
    myConnection.dispose();
    super.tearDown();
  }

  public void testBasic() throws SQLiteException, DBException {
    DBTable table = table("a").ipk(new DBLongColumn("id")).create();
    assertNull(dbt(table, false));
    assertEquals("test_a", dbt(table, true));
    assertEquals("test_a", dbt(table, false));
    roll();
    int changeNumber = 1;

    assertChangeNumber(changeNumber);
    assertEquals("test_a", dbt(table, false));
    assertEquals("test_a", dbt(table, true));
  }

  private void assertChangeNumber(int changeNumber) throws SQLiteException {
    long tcid = Util.NN(Schema.getProperty(myConnection, Schema.TCID), 0L);
    assertEquals(changeNumber, ((int) (tcid >> 32) & 0x7FFFFFFFL));
  }

  public void testRemoteChange() throws SQLiteException, DBException {
    DBTable t1 = table("a").ipk(new DBLongColumn("id")).create();
    DBTable t2 = table("b").ipk(new DBLongColumn("id")).create();
    assertEquals("test_a", dbt(t1, true));
    assertEquals("test_b", dbt(t2, true));
    roll();
    assertChangeNumber(2);
    setChangeNumber(3);
    roll();
    assertEquals("test_a", dbt(t1, false));
    assertEquals("test_b", dbt(t2, false));
  }

  private void setChangeNumber(int cn) throws SQLiteException {
    Schema.setProperty(myConnection, Schema.TCID, ((long) cn) << 32);
  }

  public void testRemoveConflictingChange() throws SQLiteException, DBException {
    DBTable t1 = table("a").ipk(new DBLongColumn("id")).create();
    assertEquals("test_a", dbt(t1, true));
    roll();
    assertChangeNumber(1);
    setChangeNumber(2);
    myConnection.exec("DROP TABLE test_a");
    myConnection.exec("CREATE TABLE test_a (idid integer primary key)");
    roll();
    assertNull(dbt(t1, false));
    assertEquals("test_a_v0", dbt(t1, true));
  }

  public void testAutoAlter() throws SQLiteException, DBException {
    DBTable t1 = table("a").ipk(new DBLongColumn("id")).create();
    assertEquals("test_a", dbt(t1, true));
    roll();
    assertChangeNumber(1);
    DBTable t2 = table("a").ipk(new DBLongColumn("id")).cols(new DBAnyTypeColumn("x")).create();
    assertNull(dbt(t2, false));
    assertEquals("test_a", dbt(t2, true));
  }

  public void testMultipleConflict() throws SQLiteException, DBException {
    DBTable t1 = table("a").ipk(new DBLongColumn("id1")).create();
    DBTable t2 = table("a").ipk(new DBLongColumn("id2")).create();
    DBTable t3 = table("a", 1).ipk(new DBLongColumn("id3")).create();
    DBTable t4 = table("a", 1).ipk(new DBLongColumn("id4")).create();
    assertEquals("test_a", dbt(t1, true));
    assertEquals("test_a_v0", dbt(t2, true));
    assertEquals("test_a_v1", dbt(t3, true));
    String t4name = dbt(t4, true);
    assertTrue(t4name.startsWith("test_a_"));
    assertEquals(8, t4name.length());
    System.out.println(t4name);
  }

  public void testReloadAfterRollback() throws SQLiteException, DBException {
    assertEquals("test_a", dbt(table("a").ipk(new DBLongColumn("id1")).create(), true));
    roll();
    assertChangeNumber(1);
    assertEquals("test_b", dbt(table("b").ipk(new DBLongColumn("id1")).create(), true));
    assertChangeNumber(2);
    rollback();
    beginImmediate();
    assertChangeNumber(1);
    assertNull(dbt(table("b").ipk(new DBLongColumn("id1")).create(), false));
  }

  private void rollback() throws SQLiteException {
    myConnection.exec("ROLLBACK");
    myContext.dispose();
    myContext = null;
  }

  private void roll() throws SQLiteException {
    commit();
    beginImmediate();
  }

  private DBTableBuilder table(String name) {
    return new DBTableBuilder("test_" + name);
  }

  private DBTableBuilder table(String name, int version) {
    return new DBTableBuilder("test_" + name, version);
  }

  private String dbt(DBTable table, boolean write) throws SQLiteException, DBException {
    TableInfo tableInfo = myTableManager.getTableInfo(myContext, table, write);
    return tableInfo == null ? null : tableInfo.getPhysicalTable();
  }
}
