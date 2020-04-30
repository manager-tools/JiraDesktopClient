package com.almworks.jira.provider3.app.connection;

import com.almworks.api.engine.ConnectionState;
import com.almworks.api.engine.InitializationState;
import com.almworks.jira.provider3.remotedata.issue.fields.JsonUserParser;
import com.almworks.jira.provider3.sync.ServerFields;
import com.almworks.spi.provider.DefaultQueriesBuilderSupport;
import com.almworks.util.text.parser.FormulaWriter;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

class JiraDefaultQueriesBuilder extends DefaultQueriesBuilderSupport {
  private final JiraConnection3 myConnection;

  public JiraDefaultQueriesBuilder(JiraConnection3 connection) {
    myConnection = connection;
  }

  public static String buildDefaultQueriesXML(JiraConnection3 connection) {
    ConnectionState state = connection.getState().getValue();
    if (state != ConnectionState.READY)
      return null;
    InitializationState initializationState = connection.getContext().getInitializationState().getValue();
    if (initializationState == null || !initializationState.isInitialized())
      return null;
    return new JiraDefaultQueriesBuilder(connection).buildXML();
  }

  public final String buildXML() {
    Element xml = new Element("defaultQueries");
    Element root = createFolder(xml, "Sample Queries");

    createQuery(root, "All Issues", "");
    JsonUserParser.LoadedUser thisUser = myConnection.getConfigHolder().getConnectionLoadedUser();
    if (thisUser != null) {
      String userRemoteIdQuoted = FormulaWriter.quoteIfNeeded(thisUser.getAccountId());
      Element assignedToMe = createQuery(root, "Assigned to Me and Unresolved",
        "(\"ASSIGNEE_USER\" in (" + userRemoteIdQuoted + ") & \"RESOLUTION_ID\" in (\"-1\"))");
      createEnumDistribution(assignedToMe, ServerFields.LegacyIds.ID_PRIORITY, "Priority");
      Element reportedByMe = createQuery(root, "Reported by Me", "(\"REPORTER_USER\" in (" + userRemoteIdQuoted + "))");
      createEnumDistribution(reportedByMe, ServerFields.LegacyIds.ID_STATUS, "Status");
    }
    createQuery(root, "Updated Today", "(UPDATED during d 0 0)");

    return new XMLOutputter(Format.getPrettyFormat()).outputString(xml);
  }

  private Element createEnumDistribution(Element root, String xmlId, String name) {
    return createLazyDistribution(root, xmlId, name, true);
  }
}
