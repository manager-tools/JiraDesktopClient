package com.almworks.http.ui;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.http.auth.HttpAuthChallengeData;
import com.almworks.api.http.auth.HttpAuthCredentials;
import com.almworks.api.http.auth.HttpAuthDialog;
import com.almworks.api.http.auth.HttpAuthPersistOption;
import com.almworks.util.L;
import com.almworks.util.Pair;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationUtil;
import com.almworks.util.model.BasicScalarModel;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnAbstractAction;
import org.almworks.util.Log;
import org.almworks.util.Util;

import javax.swing.*;

public class HttpAuthDialogImpl implements UIComponentWrapper, HttpAuthDialog {
  private static final String OUR_HOST_LABEL = "<html>The server has requested HTTP authentication.<br>" + "<br>" +
    "These username and password you usually enter into web browser to get through to " +
    "the issue tracker web site.<br><br></html>";
  private static final String OUR_PROXY_LABEL = "<html>The server has requested HTTP PROXY authentication.<br>" +
    "<br>" + "These username and password you usually enter into web browser " + "to access proxy server." +
    "<br><br></html>";

  private JPanel myWholePanel;
  private JRadioButton myRememberInMemoryButton;
  private JRadioButton myAskEachTimeButton;
  private JRadioButton myRememberOnDiskButton;
  private JPasswordField myPassword;
  private JTextField myUsername;
  private JTextField myRealm;
  private JTextField myURL;

  private final ButtonGroup myRadioGroup;
  private final DialogManager myDialogManager;
  private final Configuration myConfiguration;

  private boolean myShowing = false;
  private JLabel myURLLabel;
  private JLabel myBannerLabel;
  private JLabel myUsernameLabel;
  private JLabel myPasswordLabel;

  public HttpAuthDialogImpl(DialogManager dialogManager, Configuration configuration) {
    myDialogManager = dialogManager;
    myConfiguration = configuration;

    myRadioGroup = new ButtonGroup();
    myRadioGroup.add(myRememberOnDiskButton);
    myRadioGroup.add(myRememberInMemoryButton);
    myRadioGroup.add(myAskEachTimeButton);
    UIUtil.setupButtonGroup(myRadioGroup, configuration, "persistOption");
    myUsernameLabel.setLabelFor(myUsername);
    myPasswordLabel.setLabelFor(myPassword);

    myBannerLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
    if(Aqua.isAqua()) {
      myWholePanel.setBorder(UIUtil.BORDER_5);
    }
  }

  public static HttpAuthDialogImpl create(ComponentContainer provider) {
    return provider.instantiate(HttpAuthDialogImpl.class);
  }

  public void dispose() {
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  private Pair<HttpAuthCredentials, HttpAuthPersistOption> showModal(HttpAuthChallengeData data,
    HttpAuthCredentials failed, boolean proxy)
  {

    setData(data, failed, proxy);
    final BasicScalarModel<Boolean> myResult = BasicScalarModel.create(false, false);
    DialogBuilder builder = myDialogManager.createBuilder("httpAuth");
    builder.setOkAction(new AnAbstractAction(L.actionName("OK")) {
      public void perform(ActionContext context) {
        myResult.setValue(Boolean.TRUE);
      }
    });
    builder.setCancelAction(new AnAbstractAction(L.actionName("Cancel")) {
      public void perform(ActionContext context) {
        myResult.setValue(Boolean.FALSE);
      }
    });
    builder.setContent(this);
    builder.setModal(true);
    builder.setTitle(L.dialog("HTTP Authentication"));
    builder.setIgnoreStoredSize(true);
    builder.setInitialFocusOwner(myUsername);
    builder.showWindow();
    Boolean b = myResult.getValue();
    boolean yes = b != null && b.booleanValue();
    return yes ? getData() : null;
  }

  private Pair<HttpAuthCredentials, HttpAuthPersistOption> getData() {
    HttpAuthCredentials credentials = new HttpAuthCredentials(myUsername.getText(), myPassword.getText());
    HttpAuthPersistOption option = HttpAuthPersistOption.DONT_KEEP;
    if (myRememberInMemoryButton.isSelected())
      option = HttpAuthPersistOption.KEEP_IN_MEMORY;
    if (myRememberOnDiskButton.isSelected())
      option = HttpAuthPersistOption.KEEP_ON_DISK;
    return Pair.create(credentials, option);
  }

  private void setData(HttpAuthChallengeData data, HttpAuthCredentials failed, boolean proxy) {
    myURL.setText(proxy ? data.getHost() + ":" + data.getPort() : data.getHost());
    String scheme = Util.upper(Util.NN(data.getAuthScheme()));
    String realm = Util.NN(data.getRealm(), "N/A");
    myRealm.setText(realm + " (" + scheme + " Authentication)");
    myUsername.setText(failed == null ? "" : failed.getUsername());
    myPassword.setText(failed == null ? "" : failed.getPassword());
    myBannerLabel.setText(proxy ? OUR_PROXY_LABEL : OUR_HOST_LABEL);
    myURLLabel.setText(proxy ? "Proxy Server:" : "URL:");
    myWholePanel.revalidate();
  }

  public Pair<HttpAuthCredentials, HttpAuthPersistOption> show(HttpAuthChallengeData data, HttpAuthCredentials failed,
    boolean proxy)
  {
    if (myShowing) {
      HttpAuthDialogImpl temp = new HttpAuthDialogImpl(myDialogManager, ConfigurationUtil.copy(myConfiguration));
      if (temp.myShowing) {
        assert false;
        // wtf?
        Log.warn("HADI.show");
        return null;
      }
      return temp.show(data, failed, proxy);
    }
    myShowing = true;
    try {
      return showModal(data, failed, proxy);
    } finally {
      myShowing = false;
    }
  }
}
