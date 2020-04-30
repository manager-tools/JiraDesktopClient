package com.almworks.engine;

import com.almworks.api.engine.EngineViews;
import com.almworks.api.engine.ItemProvider;
import com.almworks.items.api.*;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.bool.Reductions;
import com.almworks.util.commons.Lazy;
import org.jetbrains.annotations.NotNull;

/**
 * :todoc:
 *
 * @author sereda
 */
public class EngineViewsImpl implements EngineViews {
  private final ItemProvider[] myProviders;
  private final Database myDb;

  private final Lazy<BoolExpr<DP>> myPrimaryItemsFilter = new Lazy<BoolExpr<DP>>() {
    @NotNull
    @Override
    protected BoolExpr<DP> instantiate() {
      return buildPrimaryItemsFilter();
    }
  };

  private final Lazy<BoolExpr<DP>> myLocalChangesFilter = new Lazy<BoolExpr<DP>>() {
    @NotNull
    @Override
    protected BoolExpr<DP> instantiate() {
      return buildLocalChangesFilter();
    }
  };

  private final Lazy<BoolExpr<DP>> myConflictsFilter = new Lazy<BoolExpr<DP>>() {
    @NotNull
    @Override
    protected BoolExpr<DP> instantiate() {
      return buildConflictsFilter();
    }
  };

  public EngineViewsImpl(ItemProvider[] providers, Database db) {
    myProviders = providers;
    myDb = db;
  }

  public Database getDatabase() {
    return myDb;
  }

  public DBFilter getItemsOfType(DBItemType type) {
    return myDb.filter(DPEqualsIdentified.create(DBAttribute.TYPE, type));
  }

  @Override
  public BoolExpr<DP> getPrimaryItemsFilter() {
    return myPrimaryItemsFilter.get();
  }

  private BoolExpr<DP> buildPrimaryItemsFilter() {
    BoolExpr<DP> r = BoolExpr.FALSE();
    for (ItemProvider provider : myProviders) {
      r = r.or(provider.getPrimaryStructure().getPrimaryItemsFilter());
    }
    return Reductions.simplify(r);
  }

  @Override
  public BoolExpr<DP> getLocalChangesFilter() {
    return myLocalChangesFilter.get();
  }

  private BoolExpr<DP> buildLocalChangesFilter() {
    BoolExpr<DP> r = BoolExpr.FALSE();
    for (ItemProvider provider : myProviders) {
      r = r.or(provider.getPrimaryStructure().getLocallyChangedFilter());
    }
    return Reductions.simplify(r);
  }

  @Override
  public BoolExpr<DP> getConflictsFilter() {
    return myConflictsFilter.get();
  }

  private BoolExpr<DP> buildConflictsFilter() {
    BoolExpr<DP> r = BoolExpr.FALSE();
    for (ItemProvider provider : myProviders) {
      r = r.or(provider.getPrimaryStructure().getConflictingItemsFilter());
    }
    return Reductions.simplify(r);
  }
}
