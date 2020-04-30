package com.almworks.sumtable;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.application.qb.ConstraintDescriptor;
import com.almworks.api.application.qb.ConstraintType;
import com.almworks.api.application.qb.EnumConstraintType;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.explorer.qbuilder.filter.BaseEnumConstraintDescriptor;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DP;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringConvertingListDecorator;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.Convertor;
import com.almworks.util.commons.Condition;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.properties.PropertyMap;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class EnumAxisDefinition extends AxisDefinition {
  private final ConstraintDescriptor myDescriptor;

  public EnumAxisDefinition(@NotNull ConstraintDescriptor descriptor) {
    myDescriptor = descriptor;
  }

  public void renderOn(Canvas canvas, CellState state) {
    myDescriptor.getPresentation().renderOn(canvas, state);
  }

  public String getName() {
    return myDescriptor.getDisplayName();
  }

  public ConstraintDescriptor getDescriptor() {
    return myDescriptor;
  }

  @NotNull
  public AListModel<? extends STFilter> createOptionsModel(Lifespan lifespan, final ItemHypercube hypercube) {
    final ConstraintType type = myDescriptor.getType();
    if(!(type instanceof EnumConstraintType)) {
      assert false : this;
      return AListModel.EMPTY;
    }

    final EnumConstraintType enumType = (EnumConstraintType)type;
    final AListModel<ItemKey> keys = enumType.getEnumModel(lifespan, hypercube);
    final BaseEnumConstraintDescriptor descriptor = myDescriptor.cast(BaseEnumConstraintDescriptor.class);
    if (descriptor == null) {
      LogHelper.error("Wrong descriptor", myDescriptor);
      return AListModel.EMPTY;
    }
    final DBAttribute attr = descriptor.getAttribute();

    final Convertor<ItemKey, STFilter> filterProducer = new Convertor<ItemKey, STFilter>() {
      public STFilter convert(ItemKey value) {
        assert value != null;
        final Icon icon = getCommonIcon(value, enumType, hypercube);
        Set<Long> longs = null;
        if(attr != null) {
          longs = descriptor.resolveSubtree(value, hypercube);
        }
        PropertyMap map = BaseEnumConstraintDescriptor.createValues(Collections.singletonList(value));
        BoolExpr<DP> filter = descriptor.createFilter(map, hypercube);
        return new STFilter(filter, value.getDisplayName(), icon, value.getId(), attr, longs);
      }
    };

    return FilteringConvertingListDecorator.create(lifespan, keys, Condition.<ItemKey>always(), filterProducer);
  }

  private Icon getCommonIcon(ItemKey value, EnumConstraintType enumType, ItemHypercube hypercube) {
    Icon icon = null;
    final List<ResolvedItem> resolved = enumType.resolveKey(value.getId(), hypercube);
    for(final ResolvedItem artifact : resolved) {
      final Icon aIcon = artifact.getIcon();
      if(icon == null) {
        icon = aIcon;
      } else if (!icon.equals(aIcon)) {
        return null;
      }
    }
    return icon;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof EnumAxisDefinition))
      return false;
    return myDescriptor.equals(((EnumAxisDefinition) obj).myDescriptor);
  }

  public int hashCode() {
    return myDescriptor.hashCode();
  }
}
