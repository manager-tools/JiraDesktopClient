package com.almworks.items.entities.api.collector.transaction.write;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.items.entities.api.collector.typetable.EntityPlace;
import com.almworks.items.sync.ItemVersionCreator;
import com.almworks.util.bool.BoolExpr;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

interface KeyCounterpart {
  @Nullable
  BoolExpr<DP> query(WriteState state, Object value);

  @Nullable
  BoolExpr<DP> queryOneOf(WriteState state, Collection<Object> entityValues);

  @Nullable
  DBAttribute<?> getAttribute();

  void update(WriteState state, ItemVersionCreator item, EntityPlace place);

  void update(WriteState state, ItemVersionCreator item, Object value);

  boolean equalValue(WriteState state, Object dbValue, Object entityValue);

  Object convertToDB(WriteState state, Object value);
}
