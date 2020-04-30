package com.almworks.jira.provider3.sync.download2.details.fields;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;

public interface ValueSupplement<T> {
  boolean supply(EntityHolder entity, T value);
}
