package com.almworks.items.impl.sqlite.filter;

import com.almworks.items.impl.sql.SQLItemSelectBuilder;
import com.almworks.items.impl.sqlite.Schema;
import com.almworks.items.impl.sqlite.TransactionContext;
import com.almworks.sqlite4java.SQLParts;
import com.almworks.sqlite4java.SQLiteException;

// caution: before using this, dump pending changes from DBWriter - see TransactionContext.getChangedItemsSorted
// todo should be affected by optimization of getChangedItemsSorted with the incremental change set
public class ChangedItemsExtractionFunction extends ExtractionFunction {
  private final long myFromIcn;
  private final long myToIcn;

  public ChangedItemsExtractionFunction(long fromIcn) {
    myFromIcn = fromIcn;
    myToIcn = -1;
  }

  public ChangedItemsExtractionFunction(long fromIcn, long toIcn) {
    myFromIcn = fromIcn;
    myToIcn = toIcn;
  }

  @Override
  public void execute(TransactionContext context, ExtractionVisitor visitor)
    throws SQLiteException
  {
    visitor.visitStarted(context);

    SQLItemSelectBuilder builder = new SQLItemSelectBuilder();
    SQLItemSelectBuilder.Join join = builder.joinPrimaryTable(Schema.ITEMS, Schema.ITEMS_ITEM.getName(), false);
    SQLPartsParameterized where = builder.addWhere();
    SQLParts parts = where.getParts();
    parts.append(join.getAlias()).append(".").append(Schema.ITEMS_LAST_ICN.getName());
    if (myToIcn < 0) {
      parts.append(" >= ?");
      where.addParameters(myFromIcn);
    } else {
      parts.append(" BETWEEN ? AND ?");
      where.addParameters(myFromIcn);
      where.addParameters(myToIcn);
    }

    visitor.visitSQL(context, builder);

    visitor.visitFinished(context);
  }
}
