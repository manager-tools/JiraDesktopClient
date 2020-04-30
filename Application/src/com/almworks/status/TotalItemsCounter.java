package com.almworks.status;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBFilter;
import com.almworks.util.English;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;

class TotalItemsCounter extends ItemStatCounter {
  public TotalItemsCounter(ExplorerComponent explorer, Engine engine) {
    super(Icons.ARTIFACT_SET, engine, explorer, ThreadGate.AWT);
    setUnclickable();
  }

  public boolean isShowUnknownAndZero() {
    return true;
  }

  protected String getTooltip(int count) {
    return L.tooltip("There " + English.are(count) + " " + items(count) + " in the local database");
  }

  protected String getViewName() {
    assert false : this;
    return L.content(Local.parse("All " + Terms.ref_Artifacts));
  }

  protected DBFilter createView() {
    assert false : this;
    return null;
  }

  protected int getConnectionCount(GenericNode node) {
    return node.getPreviewCount(true);
  }

  public static TotalItemsCounter create(ComponentContainer container) {
    return container.instantiate(TotalItemsCounter.class);
  }
}
