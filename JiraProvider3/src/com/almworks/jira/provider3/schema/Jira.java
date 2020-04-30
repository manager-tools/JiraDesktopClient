package com.almworks.jira.provider3.schema;

import com.almworks.items.api.DBAttribute;
import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.api.DBItemType;
import com.almworks.items.api.DBReader;
import com.almworks.items.cache.util.BlobLongListAttribute;
import com.almworks.items.dp.DPEquals;
import com.almworks.items.dp.DPEqualsIdentified;
import com.almworks.items.sync.ItemVersion;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.items.util.DBNamespace;
import com.almworks.items.util.SyncAttributes;
import com.almworks.jira.provider3.sync.schema.ServerJira;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Set;

public class Jira {
  private static final DBNamespace NS = ServerJira.NS;
  public static final DBNamespace NS_FEATURE = NS.subNs("feature");
  private static final DBNamespace NS_CONNECTIONS = NS.subNs("connections");
  private static final DBNamespace NS_PROVIDER = NS.subNs("provider");
  private static final DBNamespace NS_HISTORY_STEPS = NS.subNs("historySteps");

  private static final DBIdentifiedObject JIRA_PROVIDER_OBJ = NS_PROVIDER.object("provider");
  public static final DBIdentity JIRA_PROVIDER_ID = DBIdentity.fromDBObject(JIRA_PROVIDER_OBJ);
  public static final DBAttribute<Set<Long>> PROJECT_FILTER = NS_PROVIDER.linkSet("prjFilter", "Project Filter", false);
  public static final BlobLongListAttribute HIDDEN_EDITORS = new BlobLongListAttribute(NS_PROVIDER.bytes("hiddenEditors"));
  public static final DBAttribute<byte[]> LAST_LINK_TYPE = NS_PROVIDER.bytes("links.lastLinkType");

  public static final DBAttribute<Date> LAST_MKT_LICENSE_CHECK_DATE = NS_CONNECTIONS.date("lastMktCheckDate");
  public static final DBAttribute<Integer> LAST_MKT_LICENSE_CHECK_RESULT = NS_CONNECTIONS.integer("lastMktCheckResult");

  public static DBIdentifiedObject createConnectionObject(String connectionID) {
    DBIdentifiedObject object = NS_CONNECTIONS.object(connectionID);
    object.initialize(DBAttribute.TYPE, SyncAttributes.TYPE_CONNECTION);
    object.initialize(SyncAttributes.CONNECTION_ID, connectionID);
    return object;
  }

  public static DBIdentity feature(String featureId) {
    return DBIdentity.fromDBObject(NS_FEATURE.object(featureId));
  }

  public static DBIdentity historyStep(String stepId) {
    return DBIdentity.fromDBObject(NS_HISTORY_STEPS.object(stepId));
  }

  @Nullable
  public static <T> Long resolveEnum(ItemVersion connection, DBItemType type, DBAttribute<T> idAttribute, T id) {
    if (id == null || connection == null) return null;
    DBReader reader = connection.getReader();
    long item = reader.query(
      DPEquals.create(SyncAttributes.CONNECTION, connection.getItem()).and(
        DPEqualsIdentified.create(DBAttribute.TYPE, type)).and(
        DPEquals.create(idAttribute, id))).getItem();
    return item <= 0 ? null : item;

  }
}
