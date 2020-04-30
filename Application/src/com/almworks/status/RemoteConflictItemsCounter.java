package com.almworks.status;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.GenericNode;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.Engine;
import com.almworks.items.api.DBFilter;
import com.almworks.items.api.DBReader;
import com.almworks.util.English;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.threads.CanBlock;

class RemoteConflictItemsCounter extends ItemStatCounter {
  public RemoteConflictItemsCounter(Engine engine, ExplorerComponent explorer) {
    super(Icons.ARTIFACT_STATE_HAS_SYNC_CONFLICT, engine, explorer,
      ThreadGate.LONG(RemoteConflictItemsCounter.class));
  }

  public static RemoteConflictItemsCounter create(ComponentContainer container) {
    return container.instantiate(RemoteConflictItemsCounter.class);
  }

  protected DBFilter createView() {
    return myEngine.getDatabase().filter(myEngine.getViews().getConflictsFilter());
  }

  protected String getTooltip(int count) {
    return L.tooltip(
      items(count) + " " + English.have(count) + " conflicting changes both locally and on remote server");
  }

  protected String getViewName() {
    return L.content(Local.parse(Terms.ref_Artifacts + " that have conflicting changes"));
  }

  protected int getConnectionCount(GenericNode node) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected boolean isCountNeedingDatabase() {
    return true;
  }

  @Override
  @CanBlock
  protected int count(DBReader reader) {
    return (int)createView().query(reader).count();
  }
}
