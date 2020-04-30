package com.almworks.integrate;

import com.almworks.api.install.Setup;
import com.almworks.util.Env;
import com.almworks.util.components.FileSelectionField;
import com.almworks.util.files.FileUtil;
import org.almworks.util.Log;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

class IntegrateWithIDEA implements IntegrationProcedure {
  private static final String TRACKLINK_PLUGIN_DIRECTORY = "tracklink";
  private static final String PLUGIN_LIB_DIRECTORY = "lib";
  private static final String[] IDEA_DIRS = {".IntelliJIdea90", ".IntelliJIdea9M1", ".IntelliJIdea80", ".IntelliJIdea70", ".IntelliJIdea"};
  private static final String[] LIBS = {"commons-codec.jar", "xmlrpc.jar", "almworks-tracker-api.jar", "twocents.jar"};
  private static final String TRACKLINK_BUNDLE_DIR = "tracklink";
  private static final String TRACKLINK_JAR = "tracklink.jar";
  private static final File EMPTY = new File("");

  private FileSelectionField myDirectoryChooser;
  private JLabel mySelectLabel;
  private JPanel myWholePanel;

  public IntegrateWithIDEA() {
    JTextField field = myDirectoryChooser.getField();
    mySelectLabel.setLabelFor(field);
    field.setColumns(10);
    JFileChooser fileChooser = myDirectoryChooser.getFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setApproveButtonText("Select");
    myDirectoryChooser.setFile(getDefaultIdeaPluginDirectory());
  }

  private File getDefaultIdeaPluginDirectory() {
    String userHome = Env.getString("user.home");
    if (userHome == null || userHome.length() == 0)
      return EMPTY;
    File home = new File(userHome);
    File ideaHome = null;
    for (String dir : IDEA_DIRS) {
      File homeCandidate = new File(home, dir);
      if (homeCandidate.isDirectory()) {
        ideaHome = homeCandidate;
        break;
      }
    }
    if (ideaHome == null) {
      return EMPTY;
    }
    return new File(new File(ideaHome, "config"), "plugins");
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public String getTitle() {
    return "IntelliJ IDEA 7.0M2 or later";
  }

  @Override
  public String toString() {
    return getTitle();
  }

  public boolean integrate() {
    File file = myDirectoryChooser.getFile();
    if (!file.isDirectory()) {
      JOptionPane.showMessageDialog(myWholePanel, "Cannot proceed: please select an existing IDEA plug-ins directory.", "Cannot Integrate", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    int r = JOptionPane.showConfirmDialog(myWholePanel,
      "<html><body>" + Setup.getProductName() + " is about to install TrackLink plug-in for IntelliJ IDEA.<br>" +
        "Please shutdown IDEA if it is running.", "Confirm Plug-in Installation", JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.INFORMATION_MESSAGE);
    if (r != JOptionPane.OK_OPTION) {
      return false;
    }

    try {
      doInstall(file);
      JOptionPane.showMessageDialog(myWholePanel, "Plug-in installation is successful.", "Integration Successful", JOptionPane.INFORMATION_MESSAGE);
      return true;
    } catch (IOException e) {
      Log.warn("plug-in installation failed", e);
      JOptionPane.showMessageDialog(myWholePanel, "Plug-in installation failed [see logs]", "Integration Failed", JOptionPane.ERROR_MESSAGE);
      return false;
    }
  }

  public boolean checkAvailability() {
    File homeDir = Setup.getHomeDir();
    File tracklinkDir = new File(new File(homeDir, Setup.DIR_ETC), TRACKLINK_BUNDLE_DIR);
    File tracklinkJar = new File(new File(tracklinkDir, PLUGIN_LIB_DIRECTORY), TRACKLINK_JAR);
    return tracklinkJar.isFile();
  }

  private void doInstall(File file) throws IOException {
    File pluginDir = new File(file, TRACKLINK_PLUGIN_DIRECTORY);
    File pluginLib = new File(pluginDir, PLUGIN_LIB_DIRECTORY);
    if (!pluginDir.isDirectory()) {
      pluginDir.mkdir();
    }
    if (!pluginLib.isDirectory()) {
      pluginLib.mkdir();
    }

    File homeDir = Setup.getHomeDir();
    File homeLib = new File(homeDir, Setup.DIR_LIBRARIES);

    // copy libs
    for (String lib : LIBS) {
      FileUtil.copyFile(new File(homeLib, lib), new File(pluginLib, lib), true);
    }

    // copy image
    File tracklinkDir = new File(new File(homeDir, Setup.DIR_ETC), TRACKLINK_BUNDLE_DIR);
    FileUtil.copyAllRecursively(tracklinkDir, pluginDir);

    // check there's tracklink.jar
    if (!new File(pluginLib, TRACKLINK_JAR).isFile()) {
      throw new IOException("cannot find tracklink.jar");
    }
  }

  static boolean isTracklinkBundled() {
    return getTracklinkJar() != null;
  }

  private static File getTracklinkJar() {
    File homeDir = Setup.getHomeDir();
    File tracklinkDir = new File(new File(homeDir, Setup.DIR_ETC), TRACKLINK_BUNDLE_DIR);
    File tracklinkJar = new File(new File(tracklinkDir, PLUGIN_LIB_DIRECTORY), TRACKLINK_JAR);
    return tracklinkJar.isFile() ? tracklinkJar : null;
  }
}
