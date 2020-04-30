package com.almworks.api.config;

import com.almworks.util.config.Configuration;
import com.almworks.util.properties.Role;

public interface MiscConfig {
  Role<MiscConfig> ROLE = Role.role("miscConfiguration", MiscConfig.class);

  public Configuration getConfig(String key);
}
