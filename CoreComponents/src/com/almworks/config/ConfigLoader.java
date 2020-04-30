package com.almworks.config;

import com.almworks.api.config.ConfigNames;
import com.almworks.api.install.Setup;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.BadFormatException;
import com.almworks.util.L;
import com.almworks.util.Terms;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.files.FileUtil;
import com.almworks.util.i18n.Local;
import com.almworks.util.ui.DialogsUtil;
import com.almworks.util.ui.UIUtil;
import org.almworks.util.Failure;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.File;
import java.io.IOException;

class ConfigLoader {
  private static final String DEFAULT_CONFIG = "<config></config>";
  public static final String BROKEN_SUFFIX = ".BROKEN";

  private final WorkArea myWorkArea;

  public ConfigLoader(WorkArea workArea) {
    myWorkArea = workArea;
  }

  public static JDOMConfigurator loadConfig(WorkArea workArea) {
    ConfigNames.register();
    ConfigLoader loader = new ConfigLoader(workArea);
    return loader.readConfig();
  }

  private JDOMConfigurator readConfig() {
    ensureConfigExists();
    JDOMConfigurator configurator = readConfigFile();
    configurator.start();
    return configurator;
  }

  private JDOMConfigurator readConfigFile() {
    try {
      JDOMConfigurator configurator = loadConfigurator();
      if (configurator == null) {
        removeBrokenConfig();
        configurator = loadConfigurator();
        assert configurator != null;
      }
      return configurator;
    } catch (IOException e) {
      showHtmlError("Error reading configuration from " + getConfigFile() + ".<br><br>" +
        "The configuration file may be broken or locked. You can restore an automatically backed up<br>" +
        "configuration file from directory " + myWorkArea.getConfigBackupDir() + ".<br><br>" +
        "Details: " + e.getMessage() + "<br><br>" +
        "The application will now exit.");
      System.exit(2);
      return null;
    }
  }

  private void removeBrokenConfig() throws IOException {
    notifyBrokenConfig();
    File configFile = myWorkArea.getConfigFile();
    File brokenFile = getBrokenFile();
    FileUtil.copyFile(configFile, brokenFile);
    configFile.delete();
    ensureConfigExists();
  }

  private JDOMConfigurator loadConfigurator() throws IOException {
    JDOMConfigurator configurator;
    try {
      ensureConfigExists();
      configurator = new JDOMConfigurator(getConfigFile(), myWorkArea.getConfigBackupDir());
    } catch (BadFormatException e) {
      configurator = null;
    }
    return configurator;
  }

  private void notifyBrokenConfig() {
    int r = DialogsUtil.askUser(UIUtil.getDefaultDialogOwner(), createHtmlMessage("Configuration file " + getConfigFile() + " is invalid.<br><br>" +
      "The configuration file may be broken or incorrectly edited. You can restore<br>" +
      "an automatically backed up configuration file from directory:<br>" +
      "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + myWorkArea.getConfigBackupDir() + "<br><br>" +
      "Press <b>Yes</b> if you'd like to drop the broken configuration file and start the application;<br>" +
      "Press <b>No</b> if you'd like to exit the application and fix the problem manually (for example, <br>" +
      "by copying a backup configuration file).<br><br>" +
      "Would you like to drop the broken configuration and start " +
      Local.text(Terms.key_Deskzilla) + "?"), "Configuration File Problem", JOptionPane.YES_NO_OPTION);
    if (r != JOptionPane.YES_OPTION) {
      throw new Failure("start-up aborted by user");
    }
  }

  private File getBrokenFile() {
    String backupFileName = getConfigFile().getPath().concat(BROKEN_SUFFIX);
    return new File(backupFileName);
  }

  private File getConfigFile() {
    return myWorkArea.getConfigFile();
  }

  private void ensureConfigExists() {
    File configFile = getConfigFile();
    if (!configFile.exists()) {
      try {
        FileUtil.writeFile(configFile, DEFAULT_CONFIG);
      } catch (IOException e) {
        throw new Failure("cannot write config file", e);
      }
    }
  }

  private static void showHtmlError(String messageHtml) {
    DialogsUtil.showErrorMessage(UIUtil.getDefaultDialogOwner(), createHtmlMessage(messageHtml), Setup.getProductName());
  }

  private static JLabel createHtmlMessage(String messageHtml) {
    JLabel message = new JLabel();
    message.setBorder(new EmptyBorder(2, 2, 2, 2));
    message.setText(L.html("<html><body>" + messageHtml + "</body></html>"));
    return message;
  }
}
