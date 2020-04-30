package com.almworks.items.gui.meta;

import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.engine.DBCommons;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.cache.DBImage;
import com.almworks.items.cache.DataLoader;
import com.almworks.items.cache.ImageSlice;
import com.almworks.items.cache.QueryImageSlice;
import com.almworks.items.cache.util.AttributeReference;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.gui.meta.commons.SerializedObjectAttribute;
import com.almworks.items.gui.meta.schema.constraints.ConstraintKind;
import com.almworks.items.gui.meta.schema.constraints.Descriptors;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.model.ValueModel;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;

public class DescriptorsCollector extends BaseCollector<ConstraintDescriptor, DescriptorsCollector.Holder> {
  private static final AttributeReference<?> ATTRIBUTE = AttributeReference.create(Descriptors.ATTRIBUTE);
  private static final SerializedObjectAttribute<Icon> ICON = SerializedObjectAttribute.create(Icon.class, Descriptors.CONSTRAINT_ICON);
  private final EnumTypesCollector myTypes;

  private DescriptorsCollector(EnumTypesCollector types, QueryImageSlice slice) {
    super(slice);
    myTypes = types;
  }

  public static DescriptorsCollector create(DBImage image, EnumTypesCollector types) {
    QueryImageSlice slice = image.createQuerySlice(DPEqualsIdentified.create(DBAttribute.TYPE, Descriptors.DB_TYPE));
    return new DescriptorsCollector(types, slice);
  }

  @Override
  protected void initSlice(ImageSlice slice) {
    slice.addAttributes(Descriptors.XML_NAME, Descriptors.ENUM_TYPE);
    slice.addData(ConstraintKind.LOADER, ATTRIBUTE, DBCommons.DISPLAY_NAME, ICON);
  }

  public AListModel<ConstraintDescriptor> filterModel(final Lifespan life, final ValueModel<? extends ScopeFilter> scope) {
    return ModelScopeFilter.filterAndConvert(life, myModel, scope, toValue);
  }

  private EnumTypesCollector.Loaded getType(long item) {
    return myTypes.getType(item);
  }

  @Override
  protected Holder createHolder(long item) {
    return new Holder(item);
  }

  class Holder extends BaseCollector.BaseHolder<ConstraintDescriptor> {
    public Holder(long item) {
      super(item);
    }

    private <T> T getValue(DBAttribute<T> attribute) {
      return DescriptorsCollector.this.getValue(myItem, attribute);
    }

    private <T> T getValue(DataLoader<T> loader) {
      return DescriptorsCollector.this.getValue(myItem, loader);
    }

    @Override
    protected ConstraintDescriptor createValue() {
      DBAttribute<?> attribute = getValue(ATTRIBUTE);
      String displayName = getValue(DBCommons.DISPLAY_NAME);
      String id = getValue(Descriptors.XML_NAME);
      ConstraintKind kind = getValue(ConstraintKind.LOADER);
      Long enumTypeItem = getValue(Descriptors.ENUM_TYPE);
      if(attribute == null || displayName == null || id == null || kind == null) {
        LogHelper.error("Missing constrain data", myItem, attribute, displayName, id, kind, enumTypeItem);
        return null;
      }
      EnumTypesCollector.Loaded type = null;
      if(enumTypeItem != null && enumTypeItem > 0L) {
        type = getType(enumTypeItem);
        if(type == null) {
          LogHelper.error("Unknown enum type", enumTypeItem, attribute);
          return null;
        }
      }
      Icon icon = getValue(ICON);
      return kind.createDescriptor(attribute, displayName, id, type, icon);
    }
  }
}
