package com.almworks.jira.provider3.app.connection;

import com.almworks.items.api.DBResult;
import com.almworks.items.gui.meta.GuiFeaturesManager;
import com.almworks.items.sync.DBDrain;
import com.almworks.jira.provider3.gui.MetaSchema;
import com.almworks.jira.provider3.permissions.IssuePermissions;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.util.LogHelper;
import com.almworks.util.properties.Role;

public class JiraSchemaInit implements GuiFeaturesManager.DBInit {
  public static final Role<JiraSchemaInit> ROLE = Role.role(JiraSchemaInit.class);

  private long myProviderItem = 0;
  private boolean myFinished = false;

  @Override
  public void initialize(DBDrain drain) {
    myProviderItem = drain.materialize(Jira.JIRA_PROVIDER_ID);
    MetaSchema.materializeObjects(drain);
    IssuePermissions.materialize(drain);
  }

  @Override
  public void onFinished(DBResult<?> result) {
    myFinished = true;
  }

  public long getProviderItem() {
    if (!myFinished) LogHelper.error("Init is not finished yet");
    return myProviderItem;
  }
}
