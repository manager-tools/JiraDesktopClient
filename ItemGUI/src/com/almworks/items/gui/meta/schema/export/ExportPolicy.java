package com.almworks.items.gui.meta.schema.export;

import com.almworks.api.application.ExportValueType;
import com.almworks.api.application.util.ExportContext;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.util.Pair;
import com.almworks.util.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

public interface ExportPolicy {
  @Nullable
  Pair<String,ExportValueType> export(PropertyMap values, ExportContext context, GuiFeaturesManager features);
}
