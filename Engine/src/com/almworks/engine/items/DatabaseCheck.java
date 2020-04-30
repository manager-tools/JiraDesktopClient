package com.almworks.engine.items;

import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.DBWriter;
import com.almworks.util.commons.Procedure;
import org.almworks.util.Collections15;

import java.util.Collections;
import java.util.List;

public interface DatabaseCheck {
  public void check(DBReader reader, DBProblems problems);

  void init(DBWriter writer);

  /**
   * Used to register at least one object of the type (picocontainer workaround)
   */
  DatabaseCheck DUMMY = new DatabaseCheck() {
    @Override
    public void check(DBReader reader, DBProblems problems) {
    }

    @Override
    public void init(DBWriter writer) {
    }
  };

  class DBProblems {
    private String myFatalProblem = null;
    private final List<Procedure<DBWriter>> myFixes = Collections15.arrayList();
    
    public DBOperationCancelledException addFatalProblem(String description) {
      myFatalProblem = description;
      throw new DBOperationCancelledException();
    }
    
    public void fixRequired(Procedure<DBWriter> fix) {
      myFixes.add(fix);
    }
    
    public String getFatalProblem() {
      return myFatalProblem;
    }

    public List<Procedure<DBWriter>> getFixes() {
      return Collections.unmodifiableList(myFixes);
    }
  }
}
