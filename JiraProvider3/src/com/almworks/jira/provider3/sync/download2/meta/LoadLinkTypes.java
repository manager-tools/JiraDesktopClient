package com.almworks.jira.provider3.sync.download2.meta;

import com.almworks.api.connector.ConnectorException;
import com.almworks.items.entities.api.Entity;
import com.almworks.items.entities.api.collector.transaction.EntityBag2;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.jira.provider3.sync.schema.ServerLinkType;
import com.almworks.restconnector.RequestPolicy;
import com.almworks.restconnector.RestResponse;
import com.almworks.restconnector.RestSession;
import com.almworks.restconnector.json.ArrayKey;
import com.almworks.restconnector.json.JSONKey;
import com.almworks.restconnector.json.JSONValueException;
import com.almworks.util.LocalLog;
import com.almworks.util.LogHelper;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.List;

/**
 * @author dyoma
 */
class LoadLinkTypes {
  private static final LocalLog log = LocalLog.topLevel("loadLinkTypes");
  private static final ArrayKey<JSONObject> ISSUE_LINK_TYPES = ArrayKey.objectArray("issueLinkTypes");
  private static final JSONKey<Integer> ID = JSONKey.integer("id");
  private static final JSONKey<String> NAME = JSONKey.text("name");
  private static final JSONKey<String> INWARD = JSONKey.text("inward");
  private static final JSONKey<String> OUTWARD = JSONKey.text("outward");

  public static void perform(RestSession session, EntityTransaction transaction) {
    List<JSONObject> typesList = loadLinkTypes(session);
    if (typesList == null) return;
    EntityBag2 types = transaction.addBag(ServerLinkType.TYPE);
    for (JSONObject typeObj : typesList) {
      try {
        Entity entity = ServerLinkType.create(ID.getNotNull(typeObj), NAME.getNotNull(typeObj),
                INWARD.getNotNull(typeObj), OUTWARD.getNotNull(typeObj));
        types.exclude(transaction.addEntity(entity));
      } catch (JSONValueException e) {
        log.debug("Missing linkType property", e);
      }
    }
  }

  @Nullable("When failed to load link types")
  private static List<JSONObject> loadLinkTypes(RestSession session) {
    try {
      RestResponse response = session.restGet("api/2/issueLinkType", RequestPolicy.SAFE_TO_RETRY);
      if (!response.isSuccessful()) {
        LogHelper.debug("Failed to load linkTypes"); // An old JIRA has no this resource
        return null;
      }
      JSONObject jsonObject = null;
      try {
        jsonObject = response.getJSONObject();
      } catch (ParseException e) {
        log.debug("Failed to parse", e);
      }
      List<JSONObject> list = ISSUE_LINK_TYPES.list(jsonObject);
      return list.isEmpty() ? null : list;
    } catch (ConnectorException e) {
      log.warning(e);
      return null;
    }
  }
}
