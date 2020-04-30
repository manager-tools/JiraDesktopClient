package com.almworks.api.engine;

import com.almworks.util.config.Configuration;
import com.almworks.util.config.ConfigurationException;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.ThreadAWT;
import org.jetbrains.annotations.Nullable;

/**
 * @author : Dyoma
 */
public interface ItemProvider {
  Connection createConnection(String connectionID, ReadonlyConfiguration configuration, boolean isNew) throws ConfigurationException, ProviderDisabledException;

  String getProviderID();

  String getProviderName();

  void showNewConnectionWizard();

  void showEditConnectionWizard(Connection connection);

  boolean isEditingConnection(Connection connection);

  ScalarModel<ItemProviderState> getState() throws ProviderDisabledException;

  @Nullable
  Configuration createDefaultConfiguration(String itemUrl) throws ProviderDisabledException;

  @ThreadAWT
  boolean isItemUrl(String url) throws ProviderDisabledException;

  String getLicenseFeature();

  ProviderActivationAgent createActivationAgent();

  @Nullable
  Configuration getConnectionConfig(String connectionID);

  PrimaryItemStructure getPrimaryStructure();

  @Nullable
  String getDisplayableItemIdFromUrl(String url);
}
