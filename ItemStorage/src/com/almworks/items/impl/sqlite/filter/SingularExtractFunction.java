package com.almworks.items.impl.sqlite.filter;


import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;

public class SingularExtractFunction extends ExtractionFunction {
  public static final SingularExtractFunction EXTRACT_ALL = new SingularExtractFunction(true);
  public static final SingularExtractFunction EXTRACT_NONE = new SingularExtractFunction(false);

  private final boolean myAll;

  private SingularExtractFunction(boolean all) {
    myAll = all;
  }

  public void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    visitor.visitStarted(context);
    if (myAll) {
      SQLItemSelectBuilder sql = new SQLItemSelectBuilder();
      visitor.visitSQL(context, sql);
    }
    visitor.visitFinished(context);
  }
}
