package com.almworks.util.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Window;

public class FxWebViewAlert {
  public void install(WebBrowserController controller) {
    controller.addAlertListener(this::onAlert);
  }

  private void onAlert(WebEvent<String> event, WebView webView) {
    Alert alert = new Alert(Alert.AlertType.NONE, event.getData(), ButtonType.CLOSE);
    alert.setTitle("Alert");
    Window window = FXUtil.findWindow(webView);
    if (window != null) {
      alert.initModality(Modality.WINDOW_MODAL);
      alert.initOwner(window);
    } else alert.initModality(Modality.APPLICATION_MODAL);
    alert.show();

  }
}
