package com.almworks.jira.provider3.sync.download2.rest;

import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.EntityKey;
import com.almworks.items.entities.api.collector.transaction.EntityHolder;
import com.almworks.jira.provider3.sync.download2.details.fields.ValueSupplement;
import com.almworks.jira.provider3.sync.schema.ServerIssue;
import com.almworks.util.LogHelper;

public class SupplyReference implements ValueSupplement<Entity> {
  private final EntityKey<Entity> myIssueSource;
  private final EntityKey<Entity> myEnumTarget;

  public SupplyReference(EntityKey<Entity> issueSource, EntityKey<Entity> enumTarget) {
    myIssueSource = issueSource;
    myEnumTarget = enumTarget;
  }

  /**
   * Obtains value for projectKey of enum from issue {@link ServerIssue#PROJECT}
   * @param projectKey enum key for project reference
   */
  public static ValueSupplement<Entity> supplyProject(EntityKey<Entity> projectKey) {
    return new SupplyReference(ServerIssue.PROJECT, projectKey);
  }

  @Override
  public boolean supply(EntityHolder issue, Entity value) {
    if (value.isFixed()) {
      LogHelper.assertError(value.hasValue(myEnumTarget), value);
      return true;
    }
    if (!issue.hasValue(myIssueSource)) return false;
    EntityHolder project = issue.getReference(myIssueSource);
    if (project == null) return false;
    value.put(myEnumTarget, project.getPlace().restoreEntity());
    value.fix();
    return true;
  }
}
