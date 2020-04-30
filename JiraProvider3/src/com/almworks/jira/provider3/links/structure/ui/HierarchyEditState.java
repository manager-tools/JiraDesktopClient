package com.almworks.jira.provider3.links.structure.ui;

import com.almworks.util.collections.Containers;
import com.almworks.util.collections.Convertor;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.Renderers;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.*;

class HierarchyEditState {
  public static final Convertor<HierarchyEditState, String> GET_NAME = new Convertor<HierarchyEditState, String>() {
    @Override
    public String convert(HierarchyEditState value) {
      return value.getDisplayName();
    }
  };
  public static final Comparator<? super HierarchyEditState> ORDER_BY_NAME = Containers.convertingComparator(GET_NAME, String.CASE_INSENSITIVE_ORDER);
  public static final CanvasRenderer<HierarchyEditState> RENDERER = Renderers.convertingCanvasRenderer(Renderers.canvasToString(), GET_NAME);

  private final String myInitialName;
  private final List<String> myInitialIds;
  private String myDisplayName;
  private final String myId;
  private final Set<String> myLayoutIds = Collections15.hashSet();

  HierarchyEditState(String displayName, List<String> initialIds, String id) {
    myInitialName = displayName;
    myDisplayName = displayName;
    myInitialIds = Collections15.arrayList(initialIds);
    Collections.sort(myInitialIds);
    myId = id;
    myLayoutIds.addAll(myInitialIds);
  }

  public String getDisplayName() {
    return myDisplayName;
  }

  public String getId() {
    return myId;
  }

  public void setDisplayName(String displayName) {
    myDisplayName = displayName;
  }

  public Set<String> getLayoutIds() {
    return myLayoutIds;
  }

  public void setLayoutIds(Collection<String> ids) {
    myLayoutIds.clear();
    myLayoutIds.addAll(ids);
  }

  public boolean isModified() {
    if (isNew()) return true;
    if (!Util.equals(myInitialName, myDisplayName)) return true;
    ArrayList<String> ids = Collections15.arrayList(myLayoutIds);
    Collections.sort(ids);
    return !ids.equals(myInitialIds);
  }

  public boolean isNew() {
    return myId == null;
  }

  public void reset() {
    myDisplayName = myInitialName;
    myLayoutIds.clear();
    myLayoutIds.addAll(myInitialIds);
  }

  public boolean isEmpty() {
    return myLayoutIds.isEmpty();
  }

  public HierarchyEditState duplicate(String name) {
    HierarchyEditState state = new HierarchyEditState(name, Collections.<String>emptyList(), null);
    state.myLayoutIds.clear();
    state.myLayoutIds.addAll(myLayoutIds);
    return state;
  }
}
