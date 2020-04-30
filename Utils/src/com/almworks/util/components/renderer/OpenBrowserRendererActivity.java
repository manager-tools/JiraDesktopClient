package com.almworks.util.components.renderer;

import com.almworks.util.components.RendererActivityController;
import com.almworks.util.files.ExternalBrowser;
import com.almworks.util.ui.ComponentProperty;

import java.awt.*;

public class OpenBrowserRendererActivity implements RendererActivity {
  private final ExternalBrowser myBrowser;

  public OpenBrowserRendererActivity(ExternalBrowser browser) {
    myBrowser = browser;
  }

  public void apply(RendererActivityController controller, Rectangle rectangle) {
    myBrowser.openBrowser();
  }

  public static OpenBrowserRendererActivity openBrowser(String url, boolean encoded) {
    return new OpenBrowserRendererActivity(ExternalBrowser.createOpenInBrowser(url, encoded));
  }

  public <T> void storeValue(ComponentProperty<T> key, T value) {
  }
}
