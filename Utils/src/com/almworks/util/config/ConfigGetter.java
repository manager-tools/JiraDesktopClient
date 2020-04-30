package com.almworks.util.config;

import com.almworks.util.collections.Convertor;

public class ConfigGetter extends Convertor<ReadonlyConfiguration, String> {
  private final String mySetting;
  private final String myDefault;

  public ConfigGetter(String setting, String aDefault) {
    mySetting = setting;
    myDefault = aDefault;
  }

  @Override
  public String convert(ReadonlyConfiguration configuration) {
    return configuration != null ? configuration.getSetting(mySetting, myDefault) : myDefault;
  }
}
