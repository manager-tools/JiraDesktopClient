package com.almworks.jira.provider3.app.connection.setup.serverpage;

import com.almworks.api.engine.Connection;
import com.almworks.api.http.HttpUtils;
import com.almworks.jira.provider3.app.connection.setup.JiraConnectionWizard;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.restconnector.BasicAuthCredentials;
import com.almworks.restconnector.JiraCredentials;
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

import javax.swing.*;
import java.net.MalformedURLException;
import java.util.ArrayList;

class CredentialsPanelController implements UrlPage.PagePanel {
  private static final Factory<String> MESSAGE_NO_VALID_URL = JiraConnectionWizard.LOCAL.getFactory("wizard.message.noValidUrl.short");
  private static final Factory<String> MESSAGE_MISSING_CREDENTIALS_ANONYMOUS = JiraConnectionWizard.LOCAL.getFactory("wizard.message.missingCredentials.anonymous.short");

  private final CredentialsPanel myForm;
  private final UrlPage myUrlPage;

  private boolean myUpdatingForm = false;

  public CredentialsPanelController(UrlPage urlPage) {
    myUrlPage = urlPage;

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
      JiraCredentials credentials = collectCredentials();
      serverConfig = new ServerConfig(baseUrl, credentials, myUrlPage.getWizard().isIgnoreProxy(), false);
    }
    myUrlPage.setServerConfig(serverConfig, UrlPage.M_CREDENTIALS);
  }

  private JiraCredentials collectCredentials() {
    myForm.getCredentialsTracker().reset(isAnonymous(), getUserName());
    if (isAnonymous()) return JiraCredentials.ANONYMOUS;
    String login = getUserName();
    String password = getPassword();
    return BasicAuthCredentials.establishConnection(login, password);
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
}
