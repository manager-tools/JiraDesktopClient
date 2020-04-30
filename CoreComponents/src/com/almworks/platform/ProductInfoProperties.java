package com.almworks.platform;

class ProductInfoProperties {
  private final String version = "3.8.5.debug";
  private final String buildNumber = "9999";
  private final String versionType = "DEBUG";
  private final String productName = "#Client for Jira";
  private final String buildDate = "2020/04/01 14:51 MSK";

  public String getVersion() {
    return version;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public String getVersionType() {
    return versionType;
  }

  public String getProductName() {
    return productName;
  }

  public String getBuildDate() {
    return buildDate;
  }
}