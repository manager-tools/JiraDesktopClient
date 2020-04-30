package com.almworks.jira.provider3.app.connection.setup.serverpage;

import com.almworks.api.engine.GlobalLoginController;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.jira.provider3.app.connection.JiraConfiguration;
import com.almworks.jira.provider3.app.connection.setup.JiraConnectionWizard;
import com.almworks.jira.provider3.app.connection.setup.ServerConfig;
import com.almworks.jira.provider3.app.connection.setup.weblogin.WebLoginParams;
import com.almworks.spi.provider.wizard.BasePage;
import com.almworks.spi.provider.wizard.ConnectionWizard;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.Link;
import com.almworks.util.components.URLLink;
import com.almworks.util.config.Configuration;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.InlineLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.CantPerformException;
import com.jgoodies.forms.layout.*;
import org.almworks.util.Failure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UrlPage extends BasePage {
  /** Configure connection URL and credentials */
  static final int M_CREDENTIALS = 1;
  /** Configure connection with WebLogin */
  static final int M_WEB_LOGIN = 2;
  /** Show both WebLogin and Credentials panels */
  private static final int M_BOTH = M_CREDENTIALS | M_WEB_LOGIN;
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(WebLoginPanel.class.getClassLoader(), "com/almworks/jira/provider3/app/connection/setup/serverpage/message");

  private final JiraConnectionWizard myWizard;
  private final WebLoginParams.Dependencies myWebLoginDependencies;
  private final CredentialsPanelController myCredentials;
  private final WebLoginPanel myWebLogin;
  private final JPanel myWholePanel = new JPanel(new BorderLayout(5, 5));
  private int myMode;
  private ServerConfig myServerConfig;
  private JPanel mySwitchPanel;

  public UrlPage(JiraConnectionWizard wizard, GlobalLoginController loginController, WebLoginParams.Dependencies dependencies) {
    super(ConnectionWizard.URL_PAGE_ID, ConnectionWizard.CANCEL_ID, ConnectionWizard.TEST_PAGE_ID);
    myWizard = wizard;
    myWebLoginDependencies = dependencies;
    myMode = detectPageMode();
    myWebLogin = new WebLoginPanel(this);
    myCredentials = new CredentialsPanelController(this, loginController);
    myWholePanel.setBorder(UIUtil.EDITOR_PANEL_BORDER);
    JPanel initialPanel;
    JPanel switchPanel;
    if (myMode == M_BOTH) {
      FormLayout layout = new FormLayout(new ColumnSpec[]{
              new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, 1), new ColumnSpec(ColumnSpec.FILL, Sizes.PREFERRED, 1)
      }, new RowSpec[]{new RowSpec(RowSpec.FILL, Sizes.PREFERRED, 1)});
      layout.setColumnGroups(new int[][]{new int[]{1, 2}});
      JPanel panel = new JPanel(layout);
      panel.add(myCredentials.getWholePanel(), new CellConstraints(1, 1));
      JPanel webPanel = myWebLogin.getWholePanel();
      panel.add(webPanel, new CellConstraints(2, 1));
      webPanel.setBorder(new EmptyBorder(0, 2 * UIUtil.EDITOR_PANEL_BORDER_WIDTH, 0, 0));
      UIUtil.addOuterBorder(webPanel, UIUtil.createWestBevel(webPanel.getBackground()));
      UIUtil.addOuterBorder(webPanel, new EmptyBorder(0, 2 * UIUtil.EDITOR_PANEL_BORDER_WIDTH, 0, 0));
      initialPanel = panel;
      switchPanel = null;
    } else if (myMode == M_WEB_LOGIN) {
      initialPanel = myWebLogin.getWholePanel();
      switchPanel = null;
    } else { // M_CREDENTIALS
      initialPanel = myCredentials.getWholePanel();
      switchPanel = new JPanel(new BorderLayout());
      Link connect = new Link();
      connect.setText(I18N.getString("panel.switchToWebLogin.action.text"));
      connect.addActionListener(e -> WebLoginPanel.openBrowser(this));
      connect.setHorizontalAlignment(SwingConstants.RIGHT);
      URLLink doc = new URLLink(I18N.getString("panel.switchToWebLogin.learnMore.url"), true, I18N.getString("panel.switchToWebLogin.learnMore.text"));
      doc.setHorizontalAlignment(SwingConstants.RIGHT);
      JPanel links = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 5, true));
      links.add(connect);
      links.add(doc);
      switchPanel.add(links, BorderLayout.EAST);
    }
    myWholePanel.add(initialPanel, BorderLayout.CENTER);
    if (switchPanel != null) {
      mySwitchPanel = switchPanel;
      myWholePanel.add(mySwitchPanel, BorderLayout.SOUTH);
    }
    setPanel(myWholePanel);
    myMoreAction = new AdvancedAction(wizard);
  }

  /**
   * Checks what options (credentials, web-login) should be available to a user
   * @return url page mode
   */
  private int detectPageMode() {
    if (isNewConnection()) return M_BOTH;
    return JiraConfiguration.isWebLogin(myWizard.getWizardConfig()) ? M_WEB_LOGIN : M_CREDENTIALS;
  }

  public final boolean isNewConnection() {
    return myWizard.getEditedConnection() == null;
  }

  public JiraConnectionWizard getWizard() {
    return myWizard;
  }

  public WebLoginParams.Dependencies getWebLoginDependencies() {
    return myWebLoginDependencies;
  }

  @NotNull
  public static ALabel setupHeaderLabel(ALabel header) {
    UIUtil.adjustFont(header, 1.4F, Font.BOLD, true);
    header.setForeground(ColorUtil.between(header.getForeground(), Color.BLUE, 0.3F));
    return header;
  }

  public static void fixHeaderComment(JLabel label, JPanel wholePanel) {
    label.setForeground(ColorUtil.between(label.getForeground(), wholePanel.getBackground(), 0.5f));
    Font font = label.getFont();
    label.setFont(font.deriveFont(Font.PLAIN, font.getSize() * 0.9f));
  }

  private void switchMode(int newMode) {
    if (myMode == M_BOTH || (newMode != M_CREDENTIALS && newMode != M_WEB_LOGIN) || newMode == myMode) return;
    myMode = newMode;
    myWholePanel.remove(myWebLogin.getWholePanel());
    myWholePanel.remove(myCredentials.getWholePanel());
    PagePanel panel = newMode == M_CREDENTIALS ? myCredentials : myWebLogin;
    myWholePanel.add(panel.getWholePanel());
    myWholePanel.invalidate();
    myWholePanel.revalidate();
    myWholePanel.repaint();
    myWizard.setExtConfig(null);
    clearServerConfig();
    panel.onAboutToDisplay();
    myWholePanel.remove(mySwitchPanel);
    mySwitchPanel = null;
  }

  private JPanel createSwitchToCredentials() {
    JPanel panel = new JPanel(new BorderLayout());
    Link link = new Link();
    link.setText("Configure Jira URL and credentials");
    link.addActionListener(e -> switchMode(M_CREDENTIALS));
    panel.add(link, BorderLayout.EAST);
    return panel;
  }

  @Override
  public void aboutToDisplayPage(String prevPageID) {
    clearServerConfig();
    myCredentials.onAboutToDisplay();
    myWebLogin.onAboutToDisplay();
  }

  private void clearServerConfig() {
    myWizard.setExtConfig(null);
    myServerConfig = null;
    myWizard.setNextEnabled(false);
  }

  void setServerConfig(@Nullable ServerConfig extConfig, int source) {
    if ((myMode & source) == 0) return;
    myWizard.setExtConfig(null);
    if (extConfig != null || myServerConfig == null) myServerConfig = extConfig;
    if (myMode != M_BOTH || source == M_CREDENTIALS) // On new connection only credentials panel can control Next button
      myWizard.setNextEnabled(extConfig != null);
  }

  @Override
  protected JComponent createContent() {
    throw new Failure("Should not call");
  }

  public String getRawUrlValue() {
    return isCredentialsVisible() ? myCredentials.getUrl() : "";
  }

  public void updateUrl(String baseUrl) {
    myCredentials.updateUrl(baseUrl);
  }

  public boolean isAnonymous() {
    return isCredentialsVisible() && myCredentials.isAnonymous();
  }

  private boolean isCredentialsVisible() {
    return (myMode & M_CREDENTIALS) != 0;
  }

  public String getUserName() {
    return isCredentialsVisible() ? myCredentials.getUserName() : "";
  }

  @Nullable
  public ServerConfig prepareTestConnection() {
    return myServerConfig;
  }

  class AdvancedAction extends ConnectionWizard.BaseAdvancedAction {
    AdvancedAction(ConnectionWizard wizard) {
      super(wizard);
    }

    @Override
    public void perform(ActionContext context) throws CantPerformException {
      final DialogBuilder builder = createBuider();
      final ButtonModel ignoreModel = createIgnoreModel();
      createDialogContent(builder, ignoreModel);
      addOkListener(builder, ignoreModel);
      builder.showWindow();
    }

    private void createDialogContent(DialogBuilder builder, ButtonModel ignoreModel) {
      final JPanel content = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 5, true));
      content.add(createProxySection(ignoreModel));
      content.add(Box.createVerticalStrut(10));
      builder.setContent(content);
    }

    private void addOkListener(DialogBuilder builder, final ButtonModel ignoreModel) {
      builder.addOkListener(context -> ok(ignoreModel.isSelected()));
    }

    private void ok(boolean ignoreProxy) {
      Configuration wizardConfig = getWizard().getWizardConfig();
      if(ignoreProxy) {
        wizardConfig.setSetting(JiraConfiguration.IGNORE_PROXY, true);
      } else {
        wizardConfig.removeSettings(JiraConfiguration.IGNORE_PROXY);
      }
    }
  }

  interface PagePanel {
    void onAboutToDisplay();

    JComponent getWholePanel();
  }
}
