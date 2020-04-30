package com.almworks.platform;

import com.almworks.api.misc.CommandLine;

/**
 * :todoc:
 *
 * @author sereda
 */
public class CommandLineImpl implements CommandLine {
  private final String[] myCommandLine;

  public CommandLineImpl(String[] commandLine) {
    myCommandLine = commandLine;
  }

  public String[] getCommandLine() {
    return myCommandLine;
  }
}
