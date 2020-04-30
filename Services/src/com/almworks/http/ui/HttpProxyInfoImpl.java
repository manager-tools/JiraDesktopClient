package com.almworks.http.ui;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.http.HttpProxyInfo;
import com.almworks.util.WeakEncryption;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Lazy;
import com.almworks.util.config.Configuration;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.GeneralSecurityException;

public class HttpProxyInfoImpl extends SimpleModifiable implements HttpProxyInfo {
  private final Configuration myConfiguration;

  private static final String USE_PROXY = "useProxy";
  private static final String PROXY_HOST = "proxyHost";
  private static final String PROXY_PORT = "proxyPort";
  private static final String USE_PROXY_AUTH = "proxyAuth";
  private static final String PROXY_USER = "proxyUser";
  private static final String PROXY_PASSWORD = "proxyPassword";

  private Lazy<Form> myForm = new Lazy<Form>() {
    @NotNull
    public Form instantiate() {
      return new Form();
    }
  };

  public HttpProxyInfoImpl(Configuration configuration) {
    myConfiguration = configuration;
  }

  public void editProxySettings(ActionContext context) throws CantPerformException {
    DialogManager manager = context.getSourceObject(DialogManager.ROLE);
    DialogBuilder builder = manager.createBuilder("proxyInfo");
    builder.setTitle("Configure Proxy");
    builder.setContent(myForm.get().myWholePanel);
    builder.setEmptyCancelAction();
    builder.setEmptyOkAction();
    builder.addOkListener(new AnActionListener() {
      public void perform(ActionContext context) throws CantPerformException {
        changeSettings();
      }
    });
    builder.setModal(true);

    myForm.get().setValues(this);
    builder.showWindow();
  }

  public String getProxyUser() {
    return myConfiguration.getSetting(PROXY_USER, null);
  }

  public String getProxyPassword() {
    String setting = myConfiguration.getSetting(PROXY_PASSWORD, null);
    if (setting == null || setting.trim().length() == 0)
      return null;
    try {
      return WeakEncryption.decryptString(setting);
    } catch (GeneralSecurityException e) {
      return null;
    }
  }

  public Modifiable getModifiable() {
    return this;
  }

  private void changeSettings() {
    Form form = myForm.get();
    myConfiguration.setSetting(PROXY_USER, Util.NN(form.myUsername.getText()));
    myConfiguration.setSetting(PROXY_PASSWORD, getPassword(form));
    myConfiguration.setSetting(USE_PROXY_AUTH, form.myAuthRequired.isSelected());
    myConfiguration.setSetting(PROXY_HOST, Util.NN(form.myHost.getText()));
    myConfiguration.setSetting(PROXY_PORT, getPort(form));
    myConfiguration.setSetting(USE_PROXY, form.myUseProxyButton.isSelected());
    fireChanged();
  }

  private String getPassword(Form form) {
    String plain = Util.NN(form.myPassword.getText());
    if (plain.length() == 0)
      return "";
    else
      return WeakEncryption.encryptString(plain);
  }

  private String getPort(Form form) {
    String port = form.myPort.getText();
    int nport;
    try {
      nport = Integer.parseInt(port);
    } catch (NumberFormatException e) {
      nport = -1;
    }
    if (nport <= 0 || nport > 65536)
      nport = -1;
    return port;
  }

  public boolean isUsingProxy() {
    return myConfiguration.getBooleanSetting(USE_PROXY, false);
  }

  public boolean isAuthenticatedProxy() {
    return myConfiguration.getBooleanSetting(USE_PROXY_AUTH, false);
  }

  public String getProxyHost() {
    return myConfiguration.getSetting(PROXY_HOST, null);
  }

  public int getProxyPort() {
    return myConfiguration.getIntegerSetting(PROXY_PORT, -1);
  }


  static class Form implements ActionListener {
    private JPanel myWholePanel;
    private JTextField myPort;
    private JTextField myHost;
    private JCheckBox myUseProxyButton;
    private JCheckBox myAuthRequired;
    private JTextField myUsername;
    private JPasswordField myPassword;
    private JLabel myHostLabel;
    private JLabel myPortLabel;
    private JLabel myUsernameLabel;
    private JLabel myPasswordLabel;

    public Form() {
      myWholePanel.setBorder(new EmptyBorder(9, 9, 9, 9));
      myUseProxyButton.addActionListener(this);
      myAuthRequired.addActionListener(this);
      enableControls();
      setupVisual();
    }

    public void actionPerformed(ActionEvent e) {
      enableControls();
    }

    private void setupVisual() {
      myHostLabel.setLabelFor(myHost);
      myPortLabel.setLabelFor(myPort);
      myUsernameLabel.setLabelFor(myUsername);
      myPasswordLabel.setLabelFor(myPassword);
    }

    private void enableControls() {
      boolean use = myUseProxyButton.isSelected();
      myHost.setEnabled(use);
      myPort.setEnabled(use);
      myAuthRequired.setEnabled(use);
      boolean auth = use && myAuthRequired.isSelected();
      myUsername.setEnabled(auth);
      myPassword.setEnabled(auth);
    }

    void setValues(HttpProxyInfo info) {
      myHost.setText(info.getProxyHost());
      int port = info.getProxyPort();
      myPort.setText(port > 0 ? Integer.toString(port) : "");
      myUseProxyButton.setSelected(info.isUsingProxy());
      myUsername.setText(Util.NN(info.getProxyUser()));
      myPassword.setText(Util.NN(info.getProxyPassword()));
      myAuthRequired.setSelected(info.isAuthenticatedProxy());
      enableControls();
    }
  }
}
