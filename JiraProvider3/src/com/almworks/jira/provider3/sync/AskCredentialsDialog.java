package com.almworks.jira.provider3.sync;

import com.almworks.api.connector.ConnectorException;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.DialogResult;
import com.almworks.util.components.URLLink;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

class AskCredentialsDialog {
  private JPanel myWholePanel;
  private JTextField myUsername;
  private JPasswordField myPassword;
  private JCheckBox myUseAccountCredentials;
  private JTextArea myMessageArea;
  private URLLink myConnectionLink;

  public AskCredentialsDialog() {
    init();
  }

  public static boolean show(DialogManager dialogManager, StringBuffer username, StringBuffer password,
    @Nullable("Anonymous is not allowed") boolean[] anonymous, String connectionName, String baseUrl, String problemMessage)
  {
    DialogResult<Boolean> dr = new DialogResult<Boolean>(dialogManager.createBuilder("askCredentials"));
    dr.setOkResult(Boolean.TRUE);
    dr.setCancelResult(Boolean.FALSE);
    dr.pack();

    AskCredentialsDialog dialog = new AskCredentialsDialog();
    if (anonymous == null) dialog.noAnonymous();
    dialog.setup(username.toString(), password.toString(), problemMessage);
    dialog.myConnectionLink.setUrlText(connectionName);
    dialog.myConnectionLink.setUrl(baseUrl);
    dialog.myConnectionLink.setShowTooltip(true);

    Boolean r = dr.showModal("Please Enter Correct Jira Credentials", dialog.getComponent());
    boolean result = r != null && r;

    if (result) {
      if (anonymous != null) anonymous[0] = !dialog.isCredentialUsed();
      if (anonymous == null || !anonymous[0]) {
        username.setLength(0);
        username.append(dialog.getUsername());
        password.setLength(0);
        password.append(dialog.getPassword());
      }
    }

    return result;
  }

  private void noAnonymous() {
    myUseAccountCredentials.setSelected(true);
    myUseAccountCredentials.setVisible(false);
  }

  private void setup(String username, String password, String problemMessage) {
    myUseAccountCredentials.setSelected(true);
    myUsername.setText(username);
    myPassword.setText(password);
    String message = "Server did not accept your username and password. Probably they have changed. " +
      "Please enter correct username and password." +
      (myUseAccountCredentials.isVisible() ? " You can also switch to anonymous mode." : "") +
      "\n\nProblem details: " +
      problemMessage;
    myMessageArea.setText(message);
    UIUtil.scrollToTop(myMessageArea);
  }

  private void init() {
    UIUtil.setupConditionalEnabled(myUseAccountCredentials, false, myUsername, myPassword);
  }

  private String getPassword() {
    return myPassword.getText();
  }

  private String getUsername() {
    return myUsername.getText();
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public boolean isCredentialUsed() {
    return myUseAccountCredentials.isSelected();
  }

  public static void showWrongCredentials(final ConnectorException e) {
    ThreadGate.AWT_OPTIMAL.execute(new Runnable() {
      @Override
      public void run() {
        DialogsUtil.showErrorMessage(null, new JLabel("<html>" + e.getLongDescription()), "Jira Credentials");
      }
    });
  }
}
