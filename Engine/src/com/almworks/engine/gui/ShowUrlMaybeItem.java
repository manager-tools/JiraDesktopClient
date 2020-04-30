package com.almworks.engine.gui;

import com.almworks.api.engine.ItemProvider;
import com.almworks.api.engine.ProviderDisabledException;
import com.almworks.api.explorer.ItemUrlService;
import com.almworks.util.LogHelper;
import com.almworks.util.components.RendererActivityController;
import com.almworks.util.components.renderer.RendererActivity;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.ui.ComponentProperty;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class ShowUrlMaybeItem implements RendererActivity {
  private final String myUrl;
  @Nullable
  private ItemUrlService myUrlService;
  @Nullable
  private final ItemProvider myProvider;

  public ShowUrlMaybeItem(String url, @Nullable ItemUrlService urlService, ItemProvider provider) {
    myUrl = url;
    myUrlService = urlService;
    myProvider = provider;
  }

  @Override
  public void apply(RendererActivityController controller, Rectangle rectangle) {
    try {
      if (myUrlService != null) {
        myUrlService.showItem(myUrl, null);
      }
    } catch (CantPerformExceptionExplained ex) {
      maybeReportException(ex);
      myUrlService = null;
    }
    if (myUrlService == null) {
      ExternalBrowser.createOpenInBrowser(myUrl, true).openBrowser();
    }
  }

  private void maybeReportException(CantPerformExceptionExplained ex) {
    if (myProvider != null) {
      try {
        if (myProvider.isItemUrl(myUrl))
          LogHelper.debug(ex);
      } catch (ProviderDisabledException e) {
        LogHelper.warning(e);
      }
    }
  }

  @Override
  public <T> void storeValue(ComponentProperty<T> key, T value) {
  }
}
