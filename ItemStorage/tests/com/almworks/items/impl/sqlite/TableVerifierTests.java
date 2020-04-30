package com.almworks.items.impl.sqlite;

import com.almworks.items.impl.dbadapter.*;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.almworks.util.tests.BaseTestCase;
import junit.framework.Assert;

public class TableVerifierTests extends BaseTestCase {
  protected SQLiteConnection myDb;

  protected void setUp() throws Exception {
    super.setUp();
    myDb = new SQLiteConnection();
    myDb.open();
  }

  protected void tearDown() throws Exception {
    myDb.dispose();
    myDb = null;
    super.tearDown();
  }

  public void testTableCreated() throws TableValidator.Failed, SQLiteException {
    DBTable def = new DBTableBuilder("test:x")
      .cols(new DBIntColumn("id"), new DBAnyTypeColumn("x"), new DBStringColumn("y"))
      .column(new DBStringColumn("z"), true).
      pk(new DBIntColumn("id")).create();
    validateOrFail("x", def, true, false);
    myDb.exec("insert into x (id, x, y, z) values (null, 1, 'abc', 'def')");
    validateOrFail("x", def, true, false);
    myDb.exec("insert into x (id, x, y, z) values (null, 'x', 'abc', 'def')");
  }

  public void testAddColumn() throws TableValidator.Failed, SQLiteException {
    DBTableBuilder builder = new DBTableBuilder("test:x").cols(new DBAnyTypeColumn("x"));
    DBTable def = builder.create();
    validateOrFail("x", def, true, false);
    myDb.exec("insert into x (x) values (1)");
    def = builder.column(new DBAnyTypeColumn("y"), false).create();
    try {
      validateOrFail("x", def, true, false);
      Assert.fail("successfully altered?");
    } catch (TableValidator.Failed e) {
      // normal
    }
    validateOrFail("x", def, true, true);
    myDb.exec("insert into x (x, y) values (1, 2)");
  }

  public void testLeaveColumn() throws TableValidator.Failed, SQLiteException {
    validateOrFail("x", new DBTableBuilder("test:x").column(new DBAnyTypeColumn("a"), false).create(), true,
      false);
    myDb.exec("insert into x (a) values (1)");
    validateOrFail("x", new DBTableBuilder("test:x").column(new DBIntColumn("b"), false).create(), true,
      true);
    myDb.exec("insert into x (b) values (1)");
  }

  public void testRecreate() throws TableValidator.Failed, SQLiteException {
    validateOrFail("x", new DBTableBuilder("test:x").column(new DBAnyTypeColumn("a"), true).create(), true,
      false);
    myDb.exec("insert into x (a) values (1)");
    DBTable def =
      new DBTableBuilder("test:x").cols(new DBIntColumn("id")).pk(new DBIntColumn("id")).column(new DBAnyTypeColumn("a"), true).create();
    try {
      validateOrFail("x", def, true, true);
      Assert.fail("?");
    } catch (TableValidator.Failed e) {
      // good
    }
    TableValidator.validate(myDb, "x", def);
    SQLiteStatement st = myDb.prepare(new SQLParts("select * from x"));
    Assert.assertFalse(st.step());
    // recreated
    st.dispose();
    myDb.exec("insert into x (id, a) values (1, 2)");
  }

  public void validateOrFail(String tableName, DBTable definition, boolean autoCreate, boolean autoAlter)
    throws TableValidator.Failed, SQLiteException
  {
    TableValidator.validateOrFail(myDb, tableName, definition, autoCreate, autoAlter);
  }
}
