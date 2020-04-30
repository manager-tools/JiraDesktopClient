package com.almworks.jira.provider3.custom.fieldtypes.enums;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.meta.OptionsLoader;
import org.jetbrains.annotations.Nullable;

public class MetaOptionsLoader<T> extends OptionsLoader.Delegating<T> {
  private final EnumTypeKind myEnumInfo;
  private final String myPrefix;
  private final boolean myEditable;
  private final boolean mySingle;

  private MetaOptionsLoader(@Nullable OptionsLoader<T> delegate, EnumTypeKind enumInfo, String prefix, boolean editable, boolean single) {
    super(delegate);
    myEnumInfo = enumInfo;
    myPrefix = prefix;
    myEditable = editable;
    mySingle = single;
  }

  public static <T> OptionsLoader<T> wrapSingle(@Nullable OptionsLoader<T> loader, EnumTypeKind enumInfo, String prefix, boolean editable) {
    return new MetaOptionsLoader<T>(loader, enumInfo, prefix, editable, true);
  }

  public static <T> OptionsLoader<T> wrapMulti(@Nullable OptionsLoader<T> loader, EnumTypeKind enumInfo, String prefix, boolean editable) {
    return new MetaOptionsLoader<T>(loader, enumInfo, prefix, editable, false);
  }

  @Override
  public void postProcess(EntityHolder field, @Nullable T loadResult, boolean fullSet) {
    Entity type = myEnumInfo.createType(field);
    if (type != null) {
      if (mySingle) EnumFieldUtils.setupSingleEnumField(field, myPrefix, myEditable, type);
      else EnumFieldUtils.setupMultiEnumField(field, myPrefix, myEditable, type);
    }
    super.postProcess(field, loadResult, fullSet);
  }
}
