package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import org.almworks.util.TypedKey;

/**
 * Custom field {@link com.almworks.jira.provider3.custom.FieldKind kind} {@link com.almworks.jira.provider3.custom.FieldKind#getExtension(org.almworks.util.TypedKey) extension}.
 * Allows to perform additional field setup if enough date is available.
 */
public interface FieldSetup {
  public static final TypedKey<FieldSetup> SETUP_FIELD = TypedKey.create("additionalFieldSetup");

  void setupField(EntityHolder field);
}
