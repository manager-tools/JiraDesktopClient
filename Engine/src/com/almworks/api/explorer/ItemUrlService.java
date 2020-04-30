package com.almworks.api.explorer;

import com.almworks.api.engine.ItemProvider;
import com.almworks.api.search.TextSearchExecutor;
import com.almworks.util.commons.Procedure;
import com.almworks.util.config.Configuration;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadAWT;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.jetbrains.annotations.Nullable;

public interface ItemUrlService {
  Role<ItemUrlService> ROLE = Role.role(ItemUrlService.class);

  /**
   * @param confirmationHandler can be null, in that case default confirmation strategy is used.
   **/
  @ThreadAWT
  void showItem(String url, @Nullable ConfirmationHandler confirmationHandler) throws CantPerformExceptionExplained;

  SearchBuilder createSearchBuilder();

  interface SearchBuilder {

    boolean addUrl(String url);

    @Nullable
    TextSearchExecutor createExecutor();
  }

  interface ConfirmationHandler {
    void confirmCreateConnection(ItemProvider provider, Configuration configuration, Procedure<Boolean> onConfirm);

    ConfirmationHandler ALWAYS_AGREE = new ConfirmationHandler() {
      public void confirmCreateConnection(ItemProvider provider, Configuration configuration,
        Procedure<Boolean> onConfirm)
      {
        onConfirm.invoke(true);
      }
    };

    ConfirmationHandler ALWAYS_DENY = new ConfirmationHandler() {
      public void confirmCreateConnection(ItemProvider provider, Configuration configuration,
        Procedure<Boolean> onConfirm)
      {
        onConfirm.invoke(false);
      }
    };
  }
}
