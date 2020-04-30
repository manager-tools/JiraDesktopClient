package com.almworks.jira.provider3.gui;

import com.almworks.api.application.ItemWrapper;
import com.almworks.api.engine.Connection;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.EngineUtils;
import com.almworks.items.gui.edit.EditModelState;
import com.almworks.items.gui.edit.engineactions.EngineConsts;
import com.almworks.items.gui.meta.commons.SerializableFeature;
import com.almworks.items.gui.meta.schema.applicability.Applicability;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import org.almworks.util.Util;

class JiraEditApplicability implements Applicability {
  public static final SerializableFeature<Applicability> FEATURE = new SerializableFeature.NoParameters<Applicability>(new JiraEditApplicability(), Applicability.class);

  private JiraEditApplicability() {
  }

  private boolean isApplicable(Connection c) {
    JiraConnection3 connection = Util.castNullable(JiraConnection3.class, c);
    return connection != null && connection.isUploadAllowed();
  }

  @Override
  public boolean isApplicable(ItemWrapper item) {
    return isApplicable(item.services().getConnection());
  }

  @Override
  public boolean isApplicable(EditModelState model) {
    JiraConnection3 connection = EngineConsts.getConnection(JiraConnection3.class, model);
    return isApplicable(connection);
  }

  @Override
  public boolean isApplicable(ItemVersion item) {
    Long connectionItem = item.getValue(SyncAttributes.CONNECTION);
    if (connectionItem == null || connectionItem <= 0) return false;
    Engine engine = EngineUtils.getEngine(item.getReader());
    return isApplicable(engine.getConnectionManager().findByItem(connectionItem));
  }

  @Override
  public String toString() {
    return "JiraEditApplicability()";
  }
}
