package com.almworks.jira.provider3.app.connection.setup;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.engine.EngineUtils;
import com.almworks.api.engine.GlobalLoginController;
import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.app.connection.setup.serverpage.UrlPage;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginConfig;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginParams;
import com.almworks.restconnector.BasicAuthCredentials;
import com.almworks.restconnector.CookieJiraCredentials;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.login.LoginJiraCredentials;
import com.almworks.spi.provider.NewConnectionSink;
import com.almworks.spi.provider.wizard.ConnectionWizard;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.FixedListModel;
import com.almworks.util.collections.Convertors;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Connection wizard for JIRA.
 */
public class JiraConnectionWizard extends ConnectionWizard {
  public static final LocalizedAccessor LOCAL = CurrentLocale.createAccessor(JiraConnectionWizard.class.getClassLoader(), "com/almworks/jira/provider3/license");
  private final ReadonlyConfiguration myOldConfig;
  private final ConnectionTestController myTestController;

  private final UrlPage myUrlPage;
  private final TestPage myTestPage;
  private final ProjectsPage myProjPage;
  private final NamePage myNamePage;
  private final InitPage myInitPage;
  private final WaitPage myWaitPage;
  @Nullable
  private ServerConfig myExtConfig = null;


  public static JiraConnectionWizard forNewConnection(ComponentContainer container) {
    return new JiraConnectionWizard("New Jira Connection", container, null);
  }

  public static JiraConnectionWizard forEditing(
    ComponentContainer container, JiraConnection3 connection)
  {
    return new JiraConnectionWizard("Edit Jira Connection", container, connection);
  }

  private JiraConnectionWizard(String title, ComponentContainer container, JiraConnection3 connection) {
    super(title, container, connection);

    myOldConfig = connection != null ? ConfigurationUtil.copy(connection.getConfiguration()) : null;
    myUrlPage = new UrlPage(this, myContainer.getActor(GlobalLoginController.ROLE),
      WebLoginParams.Dependencies.fromContainer(myContainer));
    myTestPage = new TestPage();
    myProjPage = new ProjectsPage();
    myNamePage = new NamePage();
    myInitPage = new InitPage();
    myWaitPage = new WaitPage();
    initialize(myUrlPage, myTestPage, myProjPage, myNamePage, myInitPage, myWaitPage);

    myTestController = createTestController();
  }

  private ConnectionTestController createTestController() {
    return new ConnectionTestController(this, myContainer);
  }

  @Override
  protected void cancelConnectionTest() {
    myTestController.stopTest();
  }

  @Override
  public String getRawUrlValue() {
    return myUrlPage.getRawUrlValue();
  }

  public void updateUrl(String newUrl) {
    JiraConfiguration.setBaseUrl(myWizardConfig, newUrl);
    myUrlPage.updateUrl(JiraConfiguration.getBaseUrl(myWizardConfig));
  }

  public void setExtConfig(@Nullable ServerConfig extConfig) {
    myExtConfig = extConfig;
  }

  public boolean hasExtConfig() {
    return myExtConfig != null;
  }

  @Override
  public boolean isIgnoreProxy() {
    return myWizardConfig.getBooleanSetting(JiraConfiguration.IGNORE_PROXY, false);
  }

  @Override
  public boolean isAnonymous() {
    ServerConnectionInfo info = myTestController.getServerInfo();
    if (info != null) return info.getCredentials().isAnonymous();
    return myUrlPage.isAnonymous();
  }

  @Nullable
  public ServerConfig prepareTestConnection() {
    return myExtConfig != null ? myExtConfig : myUrlPage.prepareTestConnection();
  }

  public String getUserName() {
    return myUrlPage.getUserName();
  }

  public void clearMessages() {
    myTestPage.clearMessages();
  }

  public void showTesting(boolean inProgress) {
    myTestPage.showTesting(inProgress);
  }

  public void showInfo(boolean problem, String shortMessage, @Nullable String longMessageHtml) {
    myTestPage.showInfo(problem, shortMessage, longMessageHtml);
  }

  public void showSuccessful() {
    myTestPage.showSuccessful();
  }

  @Override
  protected boolean isSyncSettingChanged() {
    return myEditedConnection != null && !JiraConfiguration.isSyncSettingEqual(myWizardConfig, myOldConfig);
  }

  @Override
  protected NewConnectionSink getNewConnectionSink() {
    return myInitPage;
  }

  /**
   * Connection test page.
   */
  final class TestPage extends BaseTestPage {
    TestPage() {
      createPanel();
    }

    @Override
    protected void beginConnectionTest() {
      myTestController.testConnectionNow();
    }
  }

  /**
   * Project selection page.
   */
  final class ProjectsPage extends BaseUnitsPage<ConnectionTestController.Project> {
    ProjectsPage() {
      createPanel();
    }

    @Override
    public void aboutToDisplayPage(String prevPageID) {
      if(TEST_PAGE_ID.equals(prevPageID)) {
        setAvailableProjectsPreservingSelection();
      }
      super.aboutToDisplayPage(prevPageID);
    }

    private void setAvailableProjectsPreservingSelection() {
      ServerConnectionInfo serverInfo = myTestController.getServerInfo();
      List<ConnectionTestController.Project> projects = serverInfo != null ? serverInfo.getProjects() : null;
      getUnitsList().setCollectionModel(FixedListModel.create(projects), true);
    }

    @Override
    protected void loadSelectionFromConfiguration() {
      final Set<Integer> filter = JiraConfiguration.getProjectsFilter(myWizardConfig);
      if(filter != null) setSelectedProjects(filter);
    }

    private void setSelectedProjects(final Set<Integer> filter) {
      final List<ConnectionTestController.Project> selection = Collections15.arrayList();
      for(final ConnectionTestController.Project p : getAllUnits()) {
        if(filter.contains(p.getId())) {
          selection.add(p);
        }
      }
      getUnitsList().getCheckedAccessor().setSelected(selection);
    }

    @Override
    protected void saveSelectionToConfiguration() {
      if(isFiltering()) {
        myWizardConfig.setSettings(JiraConfiguration.FILTERED_PROJECTS,
          ConnectionTestController.STRING_ID.collectList(getSelectedUnits()));
      } else {
        myWizardConfig.removeSettings(JiraConfiguration.FILTERED_PROJECTS);
      }
      ServerConnectionInfo serverInfo = myTestController.getServerInfo();
      if (serverInfo == null) LogHelper.error("Missing server info");
      else {
        JiraConfiguration.setBaseUrl(myWizardConfig, serverInfo.getBaseUrl());
        JiraCredentials credentials = serverInfo.getCredentials();
        if (credentials instanceof LoginJiraCredentials) {
          JiraConfiguration.setLoginPassword(myWizardConfig, ((LoginJiraCredentials) credentials).toLoginInfo());
        } else if (credentials instanceof BasicAuthCredentials) {
          JiraConfiguration.setBasicAuth(myWizardConfig, ((BasicAuthCredentials) credentials).toLoginInfo());
        } else if (credentials instanceof CookieJiraCredentials){
          CookieJiraCredentials cookie = (CookieJiraCredentials) credentials;
          JiraConfiguration.setJiraUsername(myWizardConfig, cookie.getUsername());
          JiraConfiguration.setWebLogin(myWizardConfig, WebLoginConfig.create(cookie.getCookies()));
        } else LogHelper.error("Unknown credentials", credentials);
      }
    }

    public List<String> getSelectedProjectNames() {
      return isFiltering()
        ? Convertors.TO_STRING.collectList(getSelectedUnits())
        : Collections15.<String>emptyList();
    }
  }

  /**
   * Name & Review page.
   */
  final class NamePage extends BaseNamePage {
    NamePage() {
      createPanel();
    }

    @Override
    protected List<String> getSelectedUnitNames() {
      return myProjPage.getSelectedProjectNames();
    }

    @Override
    protected String getUsernameText() {
      ServerConnectionInfo serverInfo = myTestController.getServerInfo();
      if (serverInfo != null) {
        JiraCredentials credentials = serverInfo.getCredentials();
        String displayName = serverInfo.getUserDisplayName();
        if ((credentials != null && !credentials.isAnonymous()) || displayName != null) {
          if (credentials == null) return displayName;
          String text = displayName;
          String username = credentials.getUsername();
          if (text == null) text = username;
          else if (username.length() > 0) text += " (" + username + ")";
          return text;
        }
      }
      return super.getUsernameText();
    }

    @Override
    protected String suggestConnectionNameByUrl() {
      ServerConnectionInfo serverInfo = myTestController.getServerInfo();
      String name = serverInfo != null ? serverInfo.getTitle() : null;
      if(name == null || name.isEmpty()) {
        name = EngineUtils.suggestConnectionNameByUrl(getRawUrlValue());
      }
      return name;
    }
  }
}