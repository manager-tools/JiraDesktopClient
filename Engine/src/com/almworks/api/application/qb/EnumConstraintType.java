package com.almworks.api.application.qb;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Convertor;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.TypedKey;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * @author dyoma
 */
public interface EnumConstraintType extends ConstraintType {
  Comparator<EnumConstraintType> ORDER_BY_DISPLAY_NAME = new Comparator<EnumConstraintType>() {
    public int compare(EnumConstraintType o1, EnumConstraintType o2) {
      return ConstraintDescriptor.ORDER_BY_DISPLAY_NAME.compare(o1.getDescriptor(), o2.getDescriptor());
    }
  };

  TypedKey<List<ItemKey>> SUBSET = TypedKey.create("subset");

  boolean isNotEmpty();

  /**
   * Returns sorted unique (set) model of <b>unresolved</b> item keys, narrowed down to the hypercube
   */
  AListModel<ItemKey> getEnumModel(Lifespan life, ItemHypercube hypercube);

  /** Returns unsorted set of resolved items. Before showing it to the user, it <b>should</b> be deresolved and uniquely sorted. */
  List<ResolvedItem> getEnumList(ItemHypercube hypercube);

  /**
   * Returns sorted unique (set) model of <b>unresolved</b> item keys, full
   */
  AListModel<ItemKey> getEnumFullModel();

  ConstraintDescriptor getDescriptor();

  @ThreadSafe
  List<ResolvedItem> resolveKey(String itemId, ItemHypercube cube);

  boolean isNotSetItem(ItemKey itemKey);

//  boolean isReady();

  @Nullable
  List<EnumGrouping> getAvailableGroupings();

  @Nullable
  Convertor<ItemKey, String> getFilterConvertor();

  AListModel<? extends ResolvedItem> getResolvedEnumModel(Lifespan life, ItemHypercube hypercube);

  @Nullable
  ItemKey getMissingItem();
}
