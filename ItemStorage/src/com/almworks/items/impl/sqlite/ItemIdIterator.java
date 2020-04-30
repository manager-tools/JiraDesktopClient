package com.almworks.items.impl.sqlite;

import com.almworks.integers.LongIterator;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import org.almworks.util.Collections15;

import java.util.List;

public class ItemIdIterator {
  private final List<String> myStatements = Collections15.arrayList();
  private final int myCount;

  public ItemIdIterator(int count) {
    myCount = count;
  }

  public int addStatement(String sql) {
    myStatements.add(sql);
    return myStatements.size() - 1;
  }

  public void iterate(TransactionContext context, LongIterator ids, long[] indexBuffer, Acceptor acceptor)
    throws SQLiteException
  {
    context.checkCancelled();
    if (indexBuffer != null && indexBuffer.length != myCount) {
      assert false : myCount + " " + indexBuffer.length;
      indexBuffer = null;
    }
    StringBuilder q = new StringBuilder();
    String prefix = "";
    for (int i = 0; i < myCount; i++) {
      q.append(prefix).append('?');
      prefix = ",";
    }
    String qqq = q.toString();
    SQLiteStatement[] stmts = new SQLiteStatement[myStatements.size()];
    try {
      for (int i = 0; i < myStatements.size(); i++) {
        String s = myStatements.get(i);
        s = s.replaceFirst("\\?\\?\\?", qqq);
        stmts[i] = context.prepare(new SQLParts(s));
      }
      while (ids.hasNext()) {
        int idCount = 0;
        for (int j = 0; j < myCount; j++) {  
          long id;
          if (ids.hasNext()) {
            id = ids.nextValue();
            idCount++;
          } else {
            id = -1;
          }
          if (indexBuffer != null) {
            indexBuffer[j] = id;
          }
          for (SQLiteStatement stmt : stmts) {
            stmt.bind(j + 1, (long) id);
          }
        }
        context.checkCancelled();
        acceptor.processNext(stmts, indexBuffer, idCount);
        for (SQLiteStatement stmt : stmts) {
          stmt.reset(false);
        }
      }
    } finally {
      for (SQLiteStatement stmt : stmts) {
        stmt.dispose();
      }
    }
  }

  public void iterate(TransactionContext context, LongIterator ids) throws SQLiteException {
    iterate(context, ids, null, ExecutingAcceptor.INSTANCE);
  }

  public interface Acceptor {
    void processNext(SQLiteStatement[] boundStatements, long[] indexBuffer, int idCount) throws SQLiteException;
  }


  private static class ExecutingAcceptor implements Acceptor {
    public static final ExecutingAcceptor INSTANCE = new ExecutingAcceptor();

    public void processNext(SQLiteStatement[] boundStatements, long[] indexBuffer, int idCount)
      throws SQLiteException
    {
      for (SQLiteStatement stmt : boundStatements) {
        stmt.stepThrough();
      }
    }
  }
}
