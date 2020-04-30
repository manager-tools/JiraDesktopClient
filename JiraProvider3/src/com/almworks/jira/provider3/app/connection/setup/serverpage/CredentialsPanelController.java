package com.almworks.jira.provider3.app.connection.setup.serverpage;

import com.almworks.api.engine.Connection;
import com.almworks.api.engine.GlobalLoginController;
import com.almworks.api.http.HttpUtils;
import com.almworks.jira.provider3.app.connection.setup.JiraConnectionWizard;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.jira.provider3.sync.JiraLoginController;
import com.almworks.restconnector.BasicAuthCredentials;
import com.almworks.restconnector.JiraCredentials;
import com.almworks.restconnector.login.LoginController;
import com.almworks.restconnector.login.LoginJiraCredentials;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.completion.CompletingComboBox;
import com.almworks.util.components.completion.NotifingComboBoxEditor;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.MalformedURLException;
import java.util.ArrayList;

class CredentialsPanelController implements UrlPage.PagePanel {
  private static final Factory<String> MESSAGE_NO_VALID_URL = JiraConnectionWizard.LOCAL.getFactory("wizard.message.noValidUrl.short");
  private static final Factory<String> MESSAGE_MISSING_CREDENTIALS_ANONYMOUS = JiraConnectionWizard.LOCAL.getFactory("wizard.message.missingCredentials.anonymous.short");

  private final CredentialsPanel myForm;
  private final GlobalLoginController myLoginController;
  private final UrlPage myUrlPage;

  private boolean myUpdatingForm = false;

  public CredentialsPanelController(UrlPage urlPage, GlobalLoginController loginController) {
    myUrlPage = urlPage;
    myLoginController = loginController;

    myForm = new CredentialsPanel(urlPage.isNewConnection());
    final NotifingComboBoxEditor<String> editor = myForm.getUrl().getController().getEditor();
    if (editor == null) LogHelper.error("Editor not configured yet");
    else {
      UIUtil.addTextListener(Lifespan.FOREVER, editor.getTextComponent(), () -> {
        if (myUpdatingForm) return;
        String url = editor.getText();
        updateForm(() -> myForm.getUrl().setSelectedItem(url));
        updateUrlNotifications();
        updateCredentialsNotification();
        updateServerConfig();
      });
    }
    myForm.addCredentialsListener(() -> {
      if (myUpdatingForm) return;
      updateCredentialsNotification();
      updateServerConfig();
    });
  }

  private void updateUrlNotifications() {
    String url = getUrl();
    String finalUrl;
    try {
      finalUrl = HttpUtils.normalizeBaseUrl(url);
    } catch (MalformedURLException e) {
      finalUrl = null;
    }
    myForm.getErrorMessage().setUrlError(finalUrl == null ? MESSAGE_NO_VALID_URL.create() : null);
  }

  private void updateServerConfig() {
    ServerConfig serverConfig;
    if (myForm.hasError()) serverConfig = null;
    else {
      String baseUrl = getUrl();
      JiraCredentials credentials = collectCredentials(baseUrl);
      serverConfig = new ServerConfig(baseUrl, credentials, myUrlPage.getWizard().isIgnoreProxy(), false);
    }
    myUrlPage.setServerConfig(serverConfig, UrlPage.M_CREDENTIALS);
  }

  private JiraCredentials collectCredentials(String baseUrl) {
    if (isCredentialsUpdated()) myLoginController.clearFailureFlag(JiraLoginController.getSite(baseUrl));
    myForm.getCredentialsTracker().reset(isAnonymous(), getUserName());
    String login = getUserName();
    String password = getPassword();
    if (isBasicAuth()) {
      return BasicAuthCredentials.establishConnection(login, password);
    } else {
      boolean anonymous = isAnonymous();
      String loginToken = JiraLoginController.getLoginToken(baseUrl, login, password);
      LoginController loginController = new MyLoginController(loginToken, myLoginController);

      if (anonymous) return LoginJiraCredentials.anonymous(loginController);
      return LoginJiraCredentials.authenticated(login, password, null, loginController);
    }
  }

  public JPanel getWholePanel() {
    return myForm.getWholePanel();
  }

  public boolean isAnonymous() {
    return myForm.isAnonymous();
  }

  public String getUserName() {
    return isAnonymous() ? null : myForm.getUsername().getText().trim();
  }

  public String getPassword() {
    //noinspection deprecation
    return isAnonymous() ? null : myForm.getPassword().getText();
  }

  public boolean isBasicAuth() {
    return myForm.isBasicAuth();
  }

  private boolean myFirstCall = true;
  public void onAboutToDisplay() {
    JiraConnectionWizard wizard = myUrlPage.getWizard();
    Connection editedConnection = wizard.getEditedConnection();
    Configuration wizardConfig = wizard.getWizardConfig();
    boolean firstCall = myFirstCall;
    myFirstCall = false;
    LogHelper.assertError(!myUpdatingForm);
    updateForm(() -> {
      myForm.getUrl().setEnabled(editedConnection == null);
      if (firstCall) {
        loadKnownUrls();
        if (editedConnection != null) loadValuesFromConfiguration(wizardConfig);
      }
      updateCredentialsNotification();
      updateUrlNotifications();
    });
    updateServerConfig();
  }

  private void updateCredentialsNotification() {
    if (!isAnonymous() && (Util.NN(getUserName()).isEmpty() || Util.NN(getPassword()).isEmpty()))
      myForm.getErrorMessage().setCredentialsError(MESSAGE_MISSING_CREDENTIALS_ANONYMOUS.create());
    else myForm.getErrorMessage().setCredentialsError(null);
  }

  public void loadValuesFromConfiguration(ReadonlyConfiguration config) {
    myForm.loadValuesFromConfiguration(config);
  }

  private void updateForm(Runnable update) {
    boolean before = myUpdatingForm;
    myUpdatingForm = true;
    try {
      update.run();
    } finally {
      myUpdatingForm = before;
    }
  }

  private void loadKnownUrls() {
    LogHelper.assertError(myUpdatingForm);
    initUrls();
  }

  public void initUrls() {
    CompletingComboBox<String> url = myForm.getUrl();
    url.setEditable(true);
    url.getController().setModel(SelectionInListModel.create(new ArrayList<>(), null));
    url.setSelectedItem("");
  }

  public String getUrl() {
    CompletingComboBox<String> url = myForm.getUrl();
    if(url.isEditable()) {
      return String.valueOf(url.getEditor().getItem()).trim();
    } else {
      return String.valueOf(url.getSelectedItem());
    }
  }

  public void updateUrl(String newUrl) {
    updateForm(() -> myForm.getUrl().setSelectedItem(newUrl));
  }


  public boolean isCredentialsUpdated() {
    String loginName = getUserName();
    return isAnonymous() || myForm.getCredentialsTracker().isUpdated(loginName);
  }

  private static class MyLoginController implements LoginController {
    private final String mySite;
    private final GlobalLoginController myGlobalLogin;

    public MyLoginController(String site, GlobalLoginController globalLogin) {
      mySite = site;
      myGlobalLogin = globalLogin;
    }

    @Override
    public String getInvalidLoginMessage(String login) {
      return JiraLoginController.getInvalidLoginMessage(myGlobalLogin, mySite);
    }

    @Override
    public JiraCredentials loginInvalid(@NotNull LoginJiraCredentials credentials, String message) {
      return null;
    }
  }
}
