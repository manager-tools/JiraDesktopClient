package com.almworks.util.components;

import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.components.speedsearch.ListSpeedSearch;
import com.almworks.util.debug.DebugFrame;

public class AListSpeedSearch {
  public static void main(String[] args) {
    final AList aList = new AList(FixedListModel.create("aa", "bb", "abc", "cbcb", "acaca"));
    aList.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    ListSpeedSearch.install(aList);
    DebugFrame.show(aList);
  }
}
