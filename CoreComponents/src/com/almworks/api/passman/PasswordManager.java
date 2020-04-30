package com.almworks.api.passman;

import com.almworks.util.properties.Role;

public interface PasswordManager {
  Role<PasswordManager> ROLE = Role.role("pm");

  PMCredentials loadCredentials(PMDomain domain);

  void saveCredentials(PMDomain domain, PMCredentials credentials, boolean saveOnDisk);
}
