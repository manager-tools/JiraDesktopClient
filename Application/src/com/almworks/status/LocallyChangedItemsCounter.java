package com.almworks.status;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.explorer.tree.OutboxNode;
import com.almworks.items.api.DBFilter;
import com.almworks.util.English;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;

class LocallyChangedItemsCounter extends ItemStatCounter {
  public LocallyChangedItemsCounter(Engine engine, ExplorerComponent explorer) {
    super(Icons.ARTIFACT_STATE_HAS_UNSYNC_CHANGES, engine, explorer, ThreadGate.AWT);
  }

  public static LocallyChangedItemsCounter create(ComponentContainer container) {
    return container.instantiate(LocallyChangedItemsCounter.class);
  }

  protected DBFilter createView() {
    return createLocallyChangedView(myEngine);
  }

  static DBFilter createLocallyChangedView(Engine engine) {
    return engine.getDatabase().filter(engine.getViews().getLocalChangesFilter());
  }

  protected String getTooltip(int count) {
    return L.tooltip(items(count) + " " + English.verb("need", count) + " to be synchronized");
  }

  protected String getViewName() {
    return L.tooltip(Local.parse(Terms.ref_Artifacts + " with local changes"));
  }

  protected int getConnectionCount(GenericNode node) {
    int childCount = node.getChildrenCount();
    for (int i = childCount - 1; i >= 0; i--) {
      GenericNode child = node.getChildAt(i);
      if (child instanceof OutboxNode) {
        return child.getPreviewCount(true);
      }
    }
    return 0;
  }
}
