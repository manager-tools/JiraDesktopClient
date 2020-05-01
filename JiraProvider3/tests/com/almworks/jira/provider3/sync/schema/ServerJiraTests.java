package com.almworks.jira.provider3.sync.schema;

import com.almworks.items.api.DBItemType;
import com.almworks.items.entities.api.Entity;
import com.almworks.util.tests.BaseTestCase;

/**
 * @author dyoma
 */
public class ServerJiraTests extends BaseTestCase {
  public void testReverseEntityType() {
    Entity type = Entity.buildType("my.type");
    DBItemType dbType = ServerJira.toItemType(type);
    Entity reversedType = ServerJira.dbTypeToEntity(dbType);
    assertEquals(type.getTypeId(), reversedType.getTypeId());
  }
}
