package com.almworks.api.explorer;

import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.MetaInfo;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.collections.MultiMap;
import org.almworks.util.Collections15;

import java.util.Collection;
import java.util.List;

/**
 * @author dyoma
 */
public class MetaInfoCollector {
  // todo instead of holding LoadedArtifacts, just count
  private final MultiMap<MetaInfo, LoadedItem> myMetaInfos = MultiMap.create();
  private final OrderListModel<MetaInfo> myExistingMI = OrderListModel.create();

  public void onNewArtifacts(List<? extends LoadedItem> items) {
    for (LoadedItem item : items) {
      MetaInfo metaInfo = item.getMetaInfo();
      boolean addNew = !myMetaInfos.containsKey(metaInfo);
      myMetaInfos.add(metaInfo, item);
      if (addNew)
        myExistingMI.addElement(metaInfo);
    }
  }

  public void onArtifactsRemoved(List<LoadedItem> items) {
    for (LoadedItem item : items) {
      MetaInfo metaInfo = item.getMetaInfo();
      myMetaInfos.remove(metaInfo, item);
      if (!myMetaInfos.containsKey(metaInfo))
        myExistingMI.remove(metaInfo);
    }
  }


  public AListModel<MetaInfo> getMetaInfoModel() {
    return myExistingMI;
  }

  public Collection<MetaInfo> getAllMetaInfos() {
    return Collections15.arrayList(myExistingMI.toList());
  }
}
