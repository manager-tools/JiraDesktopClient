package com.almworks.items.util;

import com.almworks.items.api.DBReader;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import org.almworks.util.TypedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DPHolder {
  private final BoolExpr<DP> myFilter;
  private final TypedKey<BoolExpr<DP>> myResolvedKey = TypedKey.create("resolved");

  DPHolder(BoolExpr<DP> filter) {
    myFilter = filter;
  }

  public BoolExpr<DP> getFilter() {
    return myFilter;
  }

  public static DPHolder wrap(BoolExpr<DP> filter) {
    return filter != null ? new DPHolder(filter) : null;
  }

  public BoolExpr<DP> getResolved(@NotNull DBReader reader) {
    return getResolved(reader, null);
  }

  public BoolExpr<DP> getResolved(@NotNull DBReader reader, @Nullable DP.ResolutionSubscription subscription) {
    Map cache = reader.getTransactionCache();
    BoolExpr<DP> known = myResolvedKey.getFrom(cache);
    if (known == null) {
      known = DP.resolve(myFilter, reader, subscription);
      myResolvedKey.putTo(cache, known);
    }
    return known;
  }
}
