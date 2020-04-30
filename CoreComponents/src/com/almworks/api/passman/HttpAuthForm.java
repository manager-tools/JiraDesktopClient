package com.almworks.api.passman;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.AnActionListener;
import com.almworks.util.ui.actions.CantPerformException;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author dyoma
 */
public class HttpAuthForm implements UIComponentWrapper {
  private JTextField myHost;
  private JTextField myUsername;
  private JPasswordField myPassword;
  private JRadioButton myRememberPassword;
  private JRadioButton myRememberPasswordUntilCloses;
  private JPanel myWholePanel;
  private JLabel myUsernameLabel;
  private JLabel myPasswordLabel;
  private JLabel myTopLabel;
  private final ButtonGroup mySaveTypeButtonGroup = new ButtonGroup();
  private PMDomain myLastDomain;

  public HttpAuthForm(Configuration configuration) {
    setupLabelFor();
    setupButtons(configuration);
    if(Env.isMac()) {
      myWholePanel.setBorder(UIUtil.BORDER_5);
    }
    myTopLabel.putClientProperty(UIUtil.SET_DEFAULT_LABEL_ALIGNMENT, false);
    UIUtil.setDefaultLabelAlignment(myWholePanel);
    Aqua.disableMnemonics(myWholePanel);
  }

  private void setupLabelFor() {
    myUsernameLabel.setLabelFor(myUsername);
    myPasswordLabel.setLabelFor(myPassword);
  }

  private void setupButtons(Configuration configuration) {
    mySaveTypeButtonGroup.add(myRememberPassword);
    mySaveTypeButtonGroup.add(myRememberPasswordUntilCloses);
    UIUtil.setupButtonGroup(mySaveTypeButtonGroup, configuration, "rememberPassword");
  }

  public void dispose() {
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void loadDataForURL(@Nullable String url, final PasswordManager passman) {
    if (url == null)
      return;
    URL uurl;
    try {
      uurl = new URL(url);
    } catch (MalformedURLException e) {
      return;
    }

    String host = uurl.getHost();
    int port = uurl.getPort();
    String hostText = uurl.getProtocol() + "://" + host;
    if (port > 0 && port != uurl.getDefaultPort())
      hostText += ":" + port;
    myHost.setText(hostText);

    myLastDomain = new PMDomain(host, port);
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        PMCredentials credentials = passman.loadCredentials(myLastDomain);

        myUsername.setText(credentials == null ? "" : credentials.getUsername());
        myPassword.setText(credentials == null ? "" : credentials.getPassword());
      }
    });
  }

  public void saveData(final PasswordManager passman) {
    if (myLastDomain == null) {
      assert false;
      return;
    }

    final PMCredentials credentials = new PMCredentials(myUsername.getText(), myPassword.getText());
    final boolean saveOnDisk = myRememberPassword.isSelected();
    final PMDomain domain = myLastDomain;
    ThreadGate.LONG(this).execute(new Runnable() {
      public void run() {
        passman.saveCredentials(domain, credentials, saveOnDisk);
      }
    });
    myLastDomain = null;
  }

  public static boolean askUser(Configuration formConfig, DialogBuilder builder, PasswordManager passwordManager,
    @Nullable String url) {
    HttpAuthForm form = new HttpAuthForm(formConfig);
    builder.setContent(form);
    builder.setTitle(L.dialog("HTTP Authentication"));
    builder.setEmptyCancelAction();
    builder.setModal(true);
    builder.setEmptyOkAction();
    final boolean[] ok = {false};
    builder.addOkListener(new AnActionListener() {
      public void perform(ActionContext context) throws CantPerformException {
        ok[0] = true;
      }
    });
    form.loadDataForURL(url, passwordManager);
    builder.showWindow();
    if (ok[0])
      form.saveData(passwordManager);
    return ok[0];
  }
}
