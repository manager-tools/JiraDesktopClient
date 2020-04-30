package com.almworks.items.gui.meta.schema.applicability;

import com.almworks.api.application.ItemWrapper;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.sync.ItemVersion;

public interface Applicability {
  Applicability[] EMPTY_ARRAY = new Applicability[0];
  SerializedObjectAttribute<Applicability> ATTRIBUTE = SerializedObjectAttribute.create(Applicability.class, Applicabilities.NS.bytes("applicability"));

  boolean isApplicable(ItemWrapper item);

  boolean isApplicable(EditModelState model);

  boolean isApplicable(ItemVersion item);
}
