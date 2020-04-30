package com.almworks.items.gui.meta.schema.enums;

import com.almworks.api.application.ItemKey;
import com.almworks.api.application.ResolvedItem;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.explorer.qbuilder.filter.EnumNarrower;
import com.almworks.items.api.DBAttribute;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.util.advmodel.AListModel;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import java.util.Collection;
import java.util.List;

public interface LoadedEnumNarrower extends EnumNarrower {
  boolean isAccepted(ItemHypercube cube, LoadedItemKey value);

  /**
   * Adds enum attributes required by this narrower. This method allows to narrower depend on enum item attribute values
   * @param target attributes collector
   */
  void collectValueAttributes(Collection<? super DBAttribute<?>> target);

  void collectIssueAttributes(Collection<? super DBAttribute<Long>> target);
  
  boolean isAllowedValue(ItemVersion issue, ItemVersion value);

  LoadedEnumNarrower DEFAULT = new LoadedEnumNarrower() {
    @Override
    public <T extends ItemKey> AListModel<T>  narrowModel(Lifespan life, AListModel<T> original,
      ItemHypercube cube)
    {
      return EnumNarrower.DEFAULT.narrowModel(life, original, cube);
    }

    @Override
    public <I extends ResolvedItem> List<I> narrowList(List<I> values, ItemHypercube cube) {
      return EnumNarrower.DEFAULT.narrowList(values, cube);
    }

    @Override
    public void collectValueAttributes(Collection<? super DBAttribute<?>> target) {
      target.add(SyncAttributes.CONNECTION);
    }

    @Override
    public void collectIssueAttributes(Collection<? super DBAttribute<Long>> target) {
      target.add(SyncAttributes.CONNECTION);
    }

    @Override
    public boolean isAccepted(ItemHypercube cube, LoadedItemKey value) {
      if (value == null) return true;
      Collection<Long> connections = ItemHypercubeUtils.getIncludedConnections(cube);
      return connections.isEmpty() || connections.contains(value.getConnectionItem());
    }

    @Override
    public boolean isAllowedValue(ItemVersion issue, ItemVersion value) {
      long issueConnection = Util.NN(issue.getValue(SyncAttributes.CONNECTION), 0l);
      long valueConnection = Util.NN(value.getValue(SyncAttributes.CONNECTION), 0l);
      return issueConnection > 0 && valueConnection > 0 && issueConnection == valueConnection;
    }
  };

  LoadedEnumNarrower IDENTITY = new LoadedEnumNarrower() {
    @Override
    public boolean isAccepted(ItemHypercube cube, LoadedItemKey value) {
      return true;
    }

    @Override
    public void collectValueAttributes(Collection<? super DBAttribute<?>> target) {
    }

    @Override
    public void collectIssueAttributes(Collection<? super DBAttribute<Long>> target) {
    }

    @Override
    public boolean isAllowedValue(ItemVersion issue, ItemVersion value) {
      return true;
    }

    @Override
    public <T extends ItemKey> AListModel<T> narrowModel(Lifespan life, AListModel<T> original, ItemHypercube cube) {
      return original;
    }

    @Override
    public <I extends ResolvedItem> List<I> narrowList(List<I> values, ItemHypercube cube) {
      return values;
    }
  };
}
