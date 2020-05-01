package com.almworks.jira.provider3.sync.download2.details;

import com.almworks.items.api.DBIdentifiedObject;
import com.almworks.items.entities.api.collector.transaction.EntityTransaction;
import com.almworks.items.entities.api.collector.typetable.TransactionTestUtil;
import com.almworks.items.sync.util.identity.DBIdentity;
import com.almworks.jira.provider3.custom.FieldKind;
import com.almworks.jira.provider3.custom.impl.CustomFieldsComponent;
import com.almworks.jira.provider3.custom.loadxml.FieldKeysLoader;
import com.almworks.jira.provider3.schema.Jira;
import com.almworks.jira.provider3.sync.ServerInfo;
import com.almworks.jira.provider3.sync.download2.TestResources;
import com.almworks.jira.provider3.sync.schema.*;
import com.almworks.util.tests.BaseTestCase;
import org.almworks.util.TypedKey;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LoadDetailsRTests extends BaseTestCase {
  private static final TestResources RESOURCES = TestResources.create(LoadDetailsRTests.class, "com/almworks/jira/provider3/sync/download2/details/");
  private static Map<String,FieldKind> CUSTOM_FIELDS_MAP;

  static {
    try {
      FieldKeysLoader loader = FieldKeysLoader.load("/com/almworks/jira/provider3/customFields.xml", CustomFieldsComponent.SCHEMA);
      List<Map<TypedKey<?>, ?>> kinds = loader.getLoadedKinds();
      CUSTOM_FIELDS_MAP = CustomFieldsComponent.createKindsMap(false, kinds);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // https://pdyoma.atlassian.net/rest/api/2/issue/TP-1?expand=operations%2Ctransitions%2Cschema
  public void testNewCloudUsers() throws IOException, ParseException {
    runTest("newCloudUsers.json", "newCloudUsers.json.txt");
  }

  private static final String CONNECTION_ID = "CONN-ECTI-ON_I-D";
  private void runTest(String source, String result) throws IOException, ParseException {
    Object json = RESOURCES.loadJson(source);
    CollectOperations operations = new CollectOperations();
    RESOURCES.parseJsonResource(source, operations.getIssueHandler());
    DBIdentifiedObject connectionObject = Jira.createConnectionObject(CONNECTION_ID);
    DBIdentity connection = DBIdentity.fromDBObject(connectionObject);
    EntityTransaction transaction = ServerInfo.priCreateTransaction(CONNECTION_ID, connection);
    LoadDetails details = new LoadDetails(transaction, new CustomFieldsSchema.RestLoader(CUSTOM_FIELDS_MAP, CONNECTION_ID));
    RESOURCES.parseJsonResource(source, details.createHandler("Test Issue"));
    RESOURCES.assertTextEquals(result, TransactionTestUtil.printTransaction(transaction, ServerIssue.TYPE, ServerComment.TYPE, ServerLink.TYPE, ServerLinkType.TYPE, ServerWorklog.TYPE, ServerAttachment.TYPE));
  }
}
