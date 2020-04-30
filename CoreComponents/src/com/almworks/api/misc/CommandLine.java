package com.almworks.api.misc;

import com.almworks.util.properties.Role;

/**
 * :todoc:
 *
 * @author sereda
 */
public interface CommandLine {
  Role<CommandLine> ROLE = Role.role("CommandLine");

  String[] getCommandLine();
}
