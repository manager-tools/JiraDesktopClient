package com.almworks.integrate;

import com.almworks.api.install.Setup;
import com.almworks.util.Env;
import com.almworks.util.files.FileActions;

import javax.swing.*;
import java.io.File;

public class IntegrateWithVS implements IntegrationProcedure {
  private static final String TRACKLINK_NET_DIR = "tracklink.net";
  private static final String TRACKLINK_VSI = "tracklink.vsi";

  private final JLabel myComponent = new JLabel(
    "<html>Press Integrate to install TrackLink.NET add-on to Visual Studio.");

  public IntegrateWithVS() {
    myComponent.setVerticalAlignment(JLabel.TOP);
  }

  public String getTitle() {
    return "Visual Studio 2008 or later";
  }

  public boolean integrate() {
    File vsi = getVsi();
    FileActions.openFile(vsi, null);
    return true;
  }

  public JComponent getComponent() {
    return myComponent;
  }

  public boolean checkAvailability() {
    if (!Env.isWindows()) return false;
    File vsi = getVsi();
    return vsi.isFile();
  }

  private static File getVsi() {
    File homeDir = Setup.getHomeDir();
    File tracklinkDir = new File(new File(homeDir, Setup.DIR_ETC), TRACKLINK_NET_DIR);
    File vsi = new File(tracklinkDir, TRACKLINK_VSI);
    return vsi;
  }
}
