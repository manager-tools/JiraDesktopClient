package com.almworks.jira.provider3.app.connection;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.syncreg.ItemHypercubeUtils;
import com.almworks.items.gui.meta.LoadedItemKey;
import com.almworks.jira.provider3.schema.Project;
import com.almworks.restconnector.login.JiraLoginInfo;
import com.almworks.spi.provider.DefaultInformationPanel;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.UIComponentWrapper;
import org.almworks.util.Collections15;
import org.almworks.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

class JiraConnectionPanel extends DefaultInformationPanel  {
  private final JiraConnection3 myConnection;

  public JiraConnectionPanel(JiraConnection3 connection) {
    super(connection, connection.getConfigurationModel());
    myConnection = connection;
    setupForm();
  }

  @Override
  protected ConnectionInfo getConnectionInfo() {
    final ReadonlyConfiguration config = myConnection.getConfiguration();
    String baseURL;
    String loginLabel;
    String loginValue;
    JiraConfigHolder holder = myConnection.getConfigHolder();
    baseURL = Util.NN(holder.getBaseUrl());
    if (!baseURL.endsWith("/")) baseURL = baseURL + "/";
    if (JiraConfiguration.isWebLogin(config)) {
      loginLabel = "Jira Account:";
      String username = JiraConfiguration.getJiraUsername(config);
      loginValue = "<html>" + (username != null ? username : "<i>Anonymous access</i>") + " <i style=\"font-size: 80%; color: #888888\">(Connected with web browser)</i>";
    } else {
      loginLabel = "Login:";
      loginValue = holder.getJiraUsername();
      if (loginValue == null || loginValue.isEmpty()) {
        JiraLoginInfo loginInfo = JiraConfiguration.getLoginInfo(config);
        loginValue = loginInfo.isAnonymous() ? "<html><i>Anonymous access</i>" : loginInfo.getLogin();
        if (loginValue == null) loginValue = "<html><i>Anonymous access</i>";
      }
    }
    return new ConnectionInfo(JiraConfiguration.getConnectionName(config), baseURL, extractStatus(),
            loginLabel, loginValue, "Products:", extractProducts(config));
  }

  private Collection<String> extractProducts(ReadonlyConfiguration config) {
    Set<Integer> filter = JiraConfiguration.getProjectsFilter(config);
    if(filter == null) {
      return Collections15.arrayList("All available projects");
    }
    List<LoadedItemKey> projects = myConnection.getGuiFeatures()
      .getEnumTypes()
      .getType(Project.ENUM_TYPE)
      .getEnumValues(ItemHypercubeUtils.createConnectionCube(myConnection));
    ArrayList<String> names = Collections15.arrayList();
    for (LoadedItemKey project : projects) {
      Integer id = project.getValue(Project.ID);
      if (id == null || !filter.contains(id)) continue;
      names.add(project.getDisplayName());
    }
    return names;
  }

  public static UIComponentWrapper getLazyWrapper(ComponentContainer container) {
    return getLazyWrapper(container, JiraConnectionPanel.class);
  }
}
