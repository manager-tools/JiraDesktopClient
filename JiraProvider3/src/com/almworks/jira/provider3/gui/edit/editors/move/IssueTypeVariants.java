package com.almworks.jira.provider3.gui.edit.editors.move;

import com.almworks.api.application.ItemKey;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.edit.CommitContext;
import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.editors.enums.ConnectionVariants;
import com.almworks.items.gui.edit.editors.enums.EnumModelConfigurator;
import com.almworks.items.gui.edit.editors.enums.EnumVariantsSource;
import com.almworks.items.gui.edit.editors.enums.VariantsAcceptor;
import com.almworks.items.gui.meta.EnumTypesCollector;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.gui.meta.schema.enums.LoadedEnumNarrower;
import com.almworks.items.sync.VersionSource;
import com.almworks.jira.provider3.schema.IssueType;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.FilteringListDecorator;
import com.almworks.util.advmodel.SortedListDecorator;
import com.almworks.util.collections.UserDataHolder;
import com.almworks.util.commons.Condition;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

class IssueTypeVariants implements EnumVariantsSource {
  public static final EnumVariantsSource INSTANCE = new IssueTypeVariants();

  private final ConnectionVariants myDelegate = ConnectionVariants.createStatic(IssueType.ENUM_TYPE, "issueType");

  @Override
  public void prepare(VersionSource source, EditModelState model) {
    MoveController.ensureLoaded(source, model);
    myDelegate.prepare(source, model);
  }

  @Override
  public void configure(Lifespan life, EditItemModel model, VariantsAcceptor<ItemKey> acceptor) {
    EnumTypesCollector.Loaded enumType = myDelegate.getEnumType(model);
    if (enumType == null) return;
    final LoadedEnumNarrower narrower = enumType.getNarrower();
    EnumModelConfigurator listener = new EnumModelConfigurator(model, acceptor) {
      private int myLastMode = -1;

      @Override
      protected void updateVariants(Lifespan life, VariantsAcceptor<ItemKey> acceptor, AListModel<LoadedItemKey> variants, EditItemModel model, UserDataHolder data) {
        MoveController controller = MoveController.getInstance(model);
        if (controller != null) {
          int mode = controller.getCurrentMode(model);
          Condition<? super LoadedItemKey> filter = null;
          if (mode == MoveController.MODE_SUBTASK) filter = MoveController.IS_SUBTASK;
          else if (mode == MoveController.MODE_GENERIC) filter = MoveController.IS_GENERIC;
          if (filter != null) variants = FilteringListDecorator.create(life, variants, filter);
          myLastMode = mode;
        }
        myDelegate.sendToAcceptor(life, acceptor, variants, model, data);
      }

      @Override
      protected boolean isModelChanged(EditItemModel model, UserDataHolder data) {
        MoveController controller = MoveController.getInstance(model);
        if (controller == null) return false;
        int mode = controller.getCurrentMode(model);
        return !Util.equals(myLastMode, mode);
      }

      @Override
      protected AListModel<LoadedItemKey> getSortedVariantsModel(Lifespan life, EditItemModel model, ItemHypercube cube) {
        EnumTypesCollector.Loaded enumType = myDelegate.getEnumType(model);
        AListModel<LoadedItemKey> unsortedModel = enumType != null ? enumType.getValueModel(life, cube) : (AListModel<LoadedItemKey>)AListModel.EMPTY;
        return SortedListDecorator.create(life, unsortedModel, ItemKey.COMPARATOR);
      }

      protected void collectCubeAttributes(HashSet<DBAttribute<Long>> attributes) {
        narrower.collectIssueAttributes(attributes);
      }
    };
    listener.start(life);
  }

  @Override
  @Nullable
  public LoadedItemKey getResolvedItem(EditModelState model, long item) {
    return myDelegate.getResolvedItem(model, item);
  }

  @NotNull
  @Override
  public List<ItemKey> selectInvalid(EditModelState model, Collection<? extends ItemKey> items) {
    ArrayList<ItemKey> invalids = Collections15.arrayList(myDelegate.selectInvalid(model, items));
    MoveController controller = MoveController.getInstance(model);
    if (controller == null) return invalids;
    int mode = controller.getCurrentMode(model);
    if (mode == MoveController.MODE_ALL || mode == MoveController.MODE_DISABLED) return invalids;
    for (ItemKey item : items) {
      if (invalids.contains(item)) continue;
      LoadedItemKey loaded = Util.castNullable(LoadedItemKey.class, item);
      if (loaded == null) {
        LogHelper.error("Wrong item class", item);
        continue;
      }
      if (!IssueType.isSubtask(loaded, mode == MoveController.MODE_SUBTASK)) invalids.add(item);
    }
    return invalids;
  }


  @Override
  public boolean isValidValueFor(CommitContext context, long itemValue) {
    return myDelegate.isValidValueFor(context, itemValue);
  }
}
