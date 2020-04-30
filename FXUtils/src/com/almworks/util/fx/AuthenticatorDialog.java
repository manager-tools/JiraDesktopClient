package com.almworks.util.fx;

import com.almworks.util.LogHelper;
import com.almworks.util.NoObfuscation;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.ref.WeakReference;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Handles network authentication requests and shows username/password dialog.
 * {@link #install(WebBrowserController)} this on the top-level {@link WebBrowserController} to ensure that this dialog
 * servers only one browser, but not its child windows.
 * @author dyoma
 */
public class AuthenticatorDialog {
  private static final LocalizedAccessor I18N = CurrentLocale.createAccessor(AuthenticatorDialog.class.getClassLoader(), "com/almworks/util/fx/authDialog");

  private final WeakReference<WebBrowserController> myCurrentController;

  private AuthenticatorDialog(WebBrowserController controller) {
    myCurrentController = new WeakReference<>(controller);
  }

  public static void install(WebBrowserController controller) {
    AuthenticatorDialog dialog = new AuthenticatorDialog(controller);
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        LogHelper.warning("Authentication requested:", getRequestingScheme(), getRequestingURL(), getRequestingPrompt());
        URL url = getRequestingURL();
        String prompt = getRequestingPrompt();
        return dialog.askCredentials(url, prompt);
      }
    });
  }

  private PasswordAuthentication askCredentials(URL url, String prompt) {
    WebBrowserController controller = myCurrentController.get();
    if (controller == null) return null;
    return ThreadGate.FX_IMMEDIATE.compute(() -> {
      WebView webView = controller.getWebView();
      if (webView == null) return null;
      Window window = FXUtil.findWindow(webView);
      if (window == null || !window.isShowing()) return null;
      Stage stage = new Stage();
      stage.setTitle(I18N.getString("window.title.text"));
      stage.initOwner(window);
      stage.initModality(Modality.WINDOW_MODAL);
      Dialog dialog = new Dialog(stage, url, prompt);
      stage.setScene(new Scene(dialog.getWholePane()));
      dialog.getWholePane().layout();
      stage.sizeToScene();
      stage.showAndWait();
      return dialog.getResult();
    });
  }

  private static class Dialog implements NoObfuscation {
    @FXML
    private Text myPrompt;
    @FXML
    private TextField myUsername;
    @FXML
    private Parent myWholePane;
    @FXML
    private PasswordField myPassword;
    @FXML
    private Button myCancel;
    @FXML
    private Button myOk;

    private boolean myOkPressed = false;

    public Dialog(Stage stage, URL url, String prompt) {
      FXUtil.loadFxml(this, "/com/almworks/util/fx/authDialog.fxml", "/com/almworks/util/fx/authDialog.properties");
      FXUtil.addFontSizeStyle(myWholePane);
      myOk.disableProperty().bind(Bindings.createBooleanBinding(
        () -> myUsername.getText().trim().isEmpty() || myPassword.getText().isEmpty(),
        myUsername.textProperty(), myPassword.textProperty()));
      myOk.setOnAction(event -> {
        myOkPressed = true;
        stage.close();
      });
      myPrompt.setText(I18N.message2("prompt.text").formatMessage(url.toExternalForm(), prompt));
      myCancel.setOnAction(event -> stage.close());
      myWholePane.getStylesheets().add(FXUtil.loadCssRef(this, "/com/almworks/util/fx/authDialog.css"));
    }

    public Parent getWholePane() {
      return myWholePane;
    }

    public PasswordAuthentication getResult() {
      if (!myOkPressed) return null;
      String username = myUsername.getText().trim();
      if (username.isEmpty()) return null;
      String password = myPassword.getText();
      if (password.isEmpty()) return null;
      return new PasswordAuthentication(username, password.toCharArray());
    }
  }
}
