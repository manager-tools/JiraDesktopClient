package com.almworks.items.gui.edit.editors.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.UserDataHolder;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;

public abstract class BaseEnumVariantsSource extends EnumTypeProvider.Source {
  private final Comparator<ItemKey> myComparator;

  protected BaseEnumVariantsSource(EnumTypeProvider enumType) {
    super(enumType);
    myComparator = ItemKey.COMPARATOR;
  }

  public BaseEnumVariantsSource(EnumTypeProvider enumType, @Nullable Comparator<ItemKey> comparator) {
    super(enumType);
    myComparator = Util.NN(comparator, ItemKey.COMPARATOR);
  }

  @Override
  public void configure(Lifespan life, final EditItemModel model, final VariantsAcceptor<ItemKey> acceptor) {
    EnumTypesCollector.Loaded enumType = getEnumType(model);
    if (enumType == null) return;
    final LoadedEnumNarrower narrower = enumType.getNarrower();
    EnumModelConfigurator configurator = new EnumModelConfigurator(model, acceptor) {
      @Override
      protected void updateVariants(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants, EditItemModel model, UserDataHolder data) {
        sendToAcceptor(life, getAcceptor(), variants, model, data);
      }

      @Override
      protected boolean isModelChanged(EditItemModel model, UserDataHolder data) {
        return false;
      }

      @Override
      protected AListModel<LoadedItemKey> getSortedVariantsModel(Lifespan life, EditItemModel model, ItemHypercube cube) {
        AListModel<LoadedItemKey> unsortedModel = BaseEnumVariantsSource.this.getValueModel(life, model, cube);
        return SortedListDecorator.create(life, unsortedModel, myComparator);
      }

      protected void collectCubeAttributes(HashSet<DBAttribute<Long>> attributes) {
        narrower.collectIssueAttributes(attributes);
      }
    };
    configurator.start(life);
  }

  protected abstract void sendToAcceptor(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants,
    EditItemModel model, UserDataHolder data);
}
