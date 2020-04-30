package com.almworks.api.platform;

import com.almworks.api.gui.BuildNumber;
import com.almworks.util.properties.Role;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * @author : Dyoma
 */
public interface ProductInformation {
  String BETA = "BETA";
  String EAP = "EAP";
  String RC = "RC";
  String DEBUG = "DEBUG";
  String RELEASE = "RELEASE";
  String RELEASE_PATCH = "RELEASE-PATCH";

  Role<ProductInformation> ROLE = Role.role("ProductInformation");

  String getName();

  String getVersion();

  String getVersionType();

  @NotNull
  BuildNumber getBuildNumber();

  Date getBuildDate();

  boolean isProductionVersion();
}
