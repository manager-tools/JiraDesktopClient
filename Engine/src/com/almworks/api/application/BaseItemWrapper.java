package com.almworks.api.application;

import com.almworks.api.engine.Connection;

public abstract class BaseItemWrapper implements ItemWrapper {
  @Override
  public Connection getConnection() {
    return services().getConnection();
  }

  @Override
  public MetaInfo getMetaInfo() {
    return services().getMetaInfo();
  }

  @Override
  public String getItemUrl() {
    return services().getItemUrl();
  }

  @Override
  public long getItem() {
    return services().getItem();
  }
}
