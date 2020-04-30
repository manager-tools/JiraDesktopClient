package com.almworks.items.impl.sqlite.filter;

import com.almworks.integers.LongIterable;
import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLiteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtractFromPhysicalTableFunction extends ExtractionFunction {
  private final String myTable;
  private final String myItemColumn;

  @Nullable
  private final LongIterable myTargetItems;

  public ExtractFromPhysicalTableFunction(@NotNull String table, @NotNull String itemColumn, @Nullable LongIterable targetItems) {
    myTable = table;
    myItemColumn = itemColumn;
    myTargetItems = targetItems;
  }

  public void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    visitor.visitStarted(context);
    SQLItemSelectBuilder sql = new SQLItemSelectBuilder();
    sql.joinPrimaryTable(myTable, myItemColumn, false);
    if (myTargetItems != null)
      sql.whereItemInArray(myTargetItems);
    visitor.visitSQL(context, sql);
    visitor.visitFinished(context);
  }
}
