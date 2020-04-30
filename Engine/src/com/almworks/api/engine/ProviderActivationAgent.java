package com.almworks.api.engine;

import com.almworks.util.model.ScalarModel;
import com.almworks.util.threads.CanBlock;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public interface ProviderActivationAgent {
  String getActivationIntroText(int siteCount);

  @Nullable
  URL normalizeUrl(String url);

  /**
   * @return error description, or null if successful
   */
  @Nullable
  @CanBlock
  String isUrlAccessible(String useUrl, ScalarModel<Boolean> cancelFlag);
}
