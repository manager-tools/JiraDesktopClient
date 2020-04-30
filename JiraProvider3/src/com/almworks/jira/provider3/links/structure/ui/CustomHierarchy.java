package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.api.application.ItemsTreeLayout;
import com.almworks.api.application.LoadedItem;
import com.almworks.explorer.TableTreeStructure;
import com.almworks.util.LogHelper;
import com.almworks.util.components.TreeBuilder;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import org.almworks.util.Collections15;

import java.util.*;

class CustomHierarchy extends TableTreeStructure {
  private static final String SUBSET = "tree";
  private static final String S_ID = "id";
  private static final String S_NAME = "name";
  private static final String S_CHILDREN = "child";
  public static final String LAYOUT_ID_PREFIX = "-custom-.";

  private final ItemsTreeLayout[] myLayouts;
  private final String myDisplayName;
  private final String myLayoutId;

  private CustomHierarchy(ItemsTreeLayout[] layouts, String displayName, String layoutId) {
    myLayouts = layouts;
    myDisplayName = displayName;
    myLayoutId = layoutId;
  }

  public static ArrayList<HierarchyEditState> loadForEdit(ReadonlyConfiguration config) {
    ArrayList<HierarchyEditState> result = Collections15.arrayList();
    for (ReadonlyConfiguration cfg : config.getAllSubsets(SUBSET)) {
      String name = cfg.getSetting(S_NAME, "");
      String id = cfg.getSetting(S_ID, "");
      if (name.isEmpty() || id.isEmpty()) LogHelper.warning("Bad hierarchy config", name, id);
      else result.add(new HierarchyEditState(name, cfg.getAllSettings(S_CHILDREN), id));
    }
    return result;
  }

  public static ArrayList<CustomHierarchy> load(ReadonlyConfiguration config, Map<String, ItemsTreeLayout> layouts) {
    ArrayList<CustomHierarchy> result = Collections15.arrayList();
    for (ReadonlyConfiguration cfg : config.getAllSubsets(SUBSET)) {
      ArrayList<ItemsTreeLayout> children = Collections15.arrayList();
      for (String childId : cfg.getAllSettings(S_CHILDREN)) {
        ItemsTreeLayout layout = layouts.get(childId);
        if (layout != null) children.add(layout);
      }
      if (!children.isEmpty()) result.add(new CustomHierarchy(children.toArray(new ItemsTreeLayout[children.size()]), cfg.getSetting(S_NAME, ""), getLayoutId(
        cfg.getSetting(S_ID, ""))));
    }
    return result;
  }

  public static String getLayoutId(String id) {
    return LAYOUT_ID_PREFIX + id;
  }

  public static List<HierarchyEditState> saveToConfig(Configuration config, Collection<HierarchyEditState> states) {
    int maxId = 0;
    List<HierarchyEditState> newStates = Collections15.arrayList();
    config.removeSubsets(SUBSET);
    ArrayList<HierarchyEditState> result = Collections15.arrayList();
    for (HierarchyEditState state : states) {
      String id = state.getId();
      try {
        int intId = Integer.parseInt(id);
        maxId = Math.max(maxId, intId);
      } catch (NumberFormatException e) {
        LogHelper.warning("Wrong hierarchy id", id);
        id = null;
      }
      if (id == null) {
        newStates.add(state);
        continue;
      }
      Configuration cfg = config.createSubset(SUBSET);
      cfg.setSetting(S_ID, id);
      cfg.setSetting(S_NAME, state.getDisplayName());
      cfg.setSettings(S_CHILDREN, Collections15.arrayList(state.getLayoutIds()));
      result.add(state);
    }
    for (HierarchyEditState state : newStates) {
      maxId++;
      Configuration cfg = config.createSubset(SUBSET);
      String displayName = state.getDisplayName();
      String id = String.valueOf(maxId);
      ArrayList<String> layoutIds = Collections15.arrayList(state.getLayoutIds());
      cfg.setSetting(S_ID, id);
      cfg.setSetting(S_NAME, displayName);
      cfg.setSettings(S_CHILDREN, layoutIds);
      result.add(new HierarchyEditState(displayName, layoutIds, id));
    }
    return result;
  }

  @Override
  public Set<Long> getNodeParentKeys(LoadedItem element) {
    HashSet<Long> keys = Collections15.hashSet();
    for (ItemsTreeLayout layout : myLayouts) keys.addAll(TreeBuilder.getParentKeys(element, layout.getTreeStructure()));
    return keys;
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public String getLayoutId() {
    return myLayoutId;
  }

  public static String getCustomHierarchyId(String layoutId) {
    if (layoutId == null) return null;
    return layoutId.startsWith(LAYOUT_ID_PREFIX) ? layoutId.substring(LAYOUT_ID_PREFIX.length()) : null;
  }
}
