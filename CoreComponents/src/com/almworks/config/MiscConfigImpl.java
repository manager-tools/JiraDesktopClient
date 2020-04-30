package com.almworks.config;

import com.almworks.api.config.MiscConfig;
import com.almworks.util.config.Configuration;

public class MiscConfigImpl implements MiscConfig {
  private final Configuration myConfig;

  public MiscConfigImpl(Configuration config) {
    myConfig = config;
  }

  public Configuration getConfig(String key) {
    return myConfig.getOrCreateSubset(key);
  }
}
