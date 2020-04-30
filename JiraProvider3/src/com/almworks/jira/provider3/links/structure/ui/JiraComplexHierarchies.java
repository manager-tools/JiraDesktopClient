package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.engine.Connection;
import com.almworks.api.explorer.AdditionalHierarchies;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.advmodel.OrderListModel;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.Collections15;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JiraComplexHierarchies implements AdditionalHierarchies {
  private static final String S_HIERARCHIES = "hierarchies";
  private final Configuration myConfig;
  private final GuiFeaturesManager myFeatures;
  private final Connection myConnection;
  private final OrderListModel<ItemsTreeLayout> myModel = OrderListModel.create();

  public JiraComplexHierarchies(Configuration config, GuiFeaturesManager features, Connection connection) {
    myConfig = config;
    myFeatures = features;
    myConnection = connection;
  }

  @Override
  public AListModel<ItemsTreeLayout> getAdditionalLayouts() {
    return myModel;
  }

  public void start() {
    reloadFromConfig();
  }

  private void reloadFromConfig() {
    ItemsTreeLayout.OwnerFilter filter = new ItemsTreeLayout.OwnerFilter(myConnection);
    Map<String, ItemsTreeLayout> layouts = Collections15.hashMap();
    for (ItemsTreeLayout layout : myFeatures.getTreeLayouts().toList()) {
      if (!filter.isAccepted(layout)) continue;
      layouts.put(layout.getId(), layout);
    }
    final ArrayList<CustomHierarchy> hierarchies = CustomHierarchy.load(myConfig.getOrCreateSubset(S_HIERARCHIES), layouts);
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        updateModel(hierarchies);
      }
    });
  }

  private void updateModel(ArrayList<CustomHierarchy> hierarchies) {
    ArrayList<ItemsTreeLayout> layouts = Collections15.arrayList();
    for (CustomHierarchy hierarchy : hierarchies) {
      layouts.add(ItemsTreeLayout.create(hierarchy, hierarchy.getDisplayName(), hierarchy.getLayoutId(), myConnection.getConnectionItem(), Integer.MAX_VALUE));
    }
    myModel.setElements(layouts);
  }

  public ArrayList<HierarchyEditState> getEditStates() {
    return CustomHierarchy.loadForEdit(myConfig.getOrCreateSubset(S_HIERARCHIES));
  }

  public Connection getConnection() {
    return myConnection;
  }

  public List<HierarchyEditState> setNewHierarchies(List<HierarchyEditState> states) {
    List<HierarchyEditState> updated = CustomHierarchy.saveToConfig(myConfig.getOrCreateSubset(S_HIERARCHIES), states);
    reloadFromConfig();
    return updated;
  }
}
