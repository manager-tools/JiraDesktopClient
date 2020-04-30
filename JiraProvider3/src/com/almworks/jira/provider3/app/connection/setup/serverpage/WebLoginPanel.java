package com.almworks.jira.provider3.app.connection.setup.serverpage;

import com.almworks.gui.Wizard;
import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.jira.provider3.app.connection.JiraConnection3;
import com.almworks.jira.provider3.app.connection.setup.JiraConnectionWizard;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.jira.provider3.app.connection.setup.weblogin.ReLogin;
import com.almworks.jira.provider3.app.connection.setup.weblogin.ServerFilter;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginConfig;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginParams;
import com.almworks.restconnector.CookieJiraCredentials;
import com.almworks.spi.provider.wizard.ConnectionWizard;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.URLLink;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Util;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

class WebLoginPanel implements UrlPage.PagePanel {
  private static final LocalizedAccessor I18N = UrlPage.I18N;

  private final UrlPage myUrlPage;
  private JPanel myWholePanel;
  private ALabel myWebLoginHeader;
  private JButton myWebLoginButton;
  private JLabel myWebComment;
  private JPanel myConnectionInfoPanel;
  private JTextField myConnectionURL;
  private JTextField myConnectionAccount;
  private URLLink myLearMore;

  public WebLoginPanel(UrlPage urlPage) {
    myUrlPage = urlPage;
    UrlPage.setupHeaderLabel(myWebLoginHeader);
    myWebLoginHeader.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    myWebComment.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);

    Configuration config = urlPage.getWizard().getWizardConfig();
    TitledBorder titledBorder = new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), I18N.getString("panel.connectionDetails.border"));
    myConnectionInfoPanel.setBorder(new CompoundBorder(titledBorder, new EmptyBorder(5, 20, 5, 20)));
    myLearMore.setUrlText(I18N.getString("panel.learnMore.text"));
    if (JiraConfiguration.getWebLogin(config) == null || urlPage.isNewConnection()) {
      UrlPage.fixHeaderComment(myWebComment, myWholePanel);
      myWebComment.setText(I18N.getString("panel.comment.newConnection"));
      myConnectionInfoPanel.setVisible(false);
      myLearMore.setUrl(I18N.getString("panel.learnMore.url.newConnection"));
    } else {
      myWebComment.setText(I18N.messageStr("panel.comment.editConnection").formatMessage(Wizard.NEXT_TEXT));
      myConnectionInfoPanel.setVisible(true);
      myConnectionURL.setText(JiraConfiguration.getBaseUrl(config));
      String account = JiraConfiguration.getJiraUsername(config);
      if (account == null) {
        account = "Anonymous";
        myConnectionAccount.setFont(myConnectionAccount.getFont().deriveFont(Font.ITALIC));
      } else myConnectionAccount.setFont(myConnectionAccount.getFont().deriveFont(Font.PLAIN));
      myConnectionAccount.setText(account);
      myLearMore.setUrl(I18N.getString("panel.learnMore.url.editConnection"));
    }
    myWebLoginButton.addActionListener(e -> {
      openBrowser(urlPage);
    });
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
  }

  public static void openBrowser(UrlPage urlPage) {
    Configuration wizardConfig = urlPage.getWizard().getWizardConfig();
    WebLoginConfig webLogin = JiraConfiguration.getWebLogin(wizardConfig);
    String baseUrl = JiraConfiguration.getBaseUrl(wizardConfig);
    String initialUrl;
    if (baseUrl != null && !baseUrl.trim().isEmpty()) initialUrl = baseUrl;
    else initialUrl = urlPage.getRawUrlValue();
    JiraConnection3 editedConnection = Util.castNullable(JiraConnection3.class, urlPage.getWizard().getEditedConnection());
    String purpose = editedConnection == null ? "Configure new connection" : "Edit connection '" + editedConnection.getName() + "'";
    String configuredUrl = editedConnection != null ? JiraConfiguration.getBaseUrl(wizardConfig) : null;
    ServerFilter serverFilter = configuredUrl == null ? null
            : new ServerFilter().checkUrl(configuredUrl, ReLogin.WRONG_JIRA);
    new WebLoginParams(urlPage.getWebLoginDependencies(), urlPage.getWizard().isIgnoreProxy(), purpose)
            .setWindowTitle(I18N.getString(editedConnection != null ? "browser.window.title.edit" : "browser.window.title.new"))
            .initialUrl(initialUrl)
            .config(webLogin)
            .serverFilter(serverFilter)
            .setConnectHint(I18N.getString("browser.canConnect.popup"))
            .setConnectPopup(server -> server.getUsername() != null)
            .showBrowser(
              window -> {
                if (window != null) urlPage.getWizard().getWindowController().hide();
              },
              login -> {
                urlPage.getWizard().getWindowController().show();
                ServerConfig serverConfig = login != null ? login.getServerConfig() : null;
                if (serverConfig != null) {
                  urlPage.getWizard().setExtConfig(serverConfig);
                  urlPage.getWizard().showPage(ConnectionWizard.TEST_PAGE_ID);
                }
            });

  }

  public JPanel getWholePanel() {
    return myWholePanel;
  }

  public void onAboutToDisplay() {
    JiraConnectionWizard wizard = myUrlPage.getWizard();
    if (wizard.getEditedConnection() == null) return;
    Configuration config = wizard.getWizardConfig();
    WebLoginConfig webLogin = JiraConfiguration.getWebLogin(config);
    String username = JiraConfiguration.getJiraUsername(config);
    String baseUrl = JiraConfiguration.getBaseUrl(config);
    if (webLogin == null || baseUrl == null) {
      myUrlPage.setServerConfig(null, UrlPage.M_WEB_LOGIN);
    } else {
      CookieJiraCredentials credentials = CookieJiraCredentials.connected(username, webLogin.getCookies().getAllCookies(), null, null, null);
      ServerConfig serverConfig = new ServerConfig(baseUrl, credentials, wizard.isIgnoreProxy(), true);
      myUrlPage.setServerConfig(serverConfig, UrlPage.M_WEB_LOGIN);
    }
  }
}
