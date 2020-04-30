package com.almworks.util.fx;

import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.NoObfuscation;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.almworks.util.Util;

public class WebBrowser implements NoObfuscation {
  private final WebBrowserController myController = new WebBrowserController();
  @FXML
  private VBox myWholePane;
  @FXML
  private TextField myLocation;
  @FXML
  private WebView myWebView;
  @FXML
  private ProgressBar myProgressBar;
  @FXML
  private Button myReload;
  @FXML
  private Button myForward;
  @FXML
  private Button myBack;

  public WebBrowser() {
    FXUtil.loadFxml(this, "/com/almworks/util/fx/webBrowser.fxml", null);
    HBox parent = Util.castNullable(HBox.class, myReload.getParent());
    if (Env.getBoolean(GlobalProperties.INTERNAL_ACTIONS) && parent != null) {
      Button debug = new Button("Debug");
      debug.setOnAction(event -> myController.getWebView().getEngine()
        .executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}"));
      parent.getChildren().add(debug);
    }
    myController
            .setControls(myWholePane, myWebView, myLocation, myProgressBar, myBack, myForward, myReload)
            .setBackForwardFontIcons(FontAwesomeIcon.CHEVRON_LEFT, FontAwesomeIcon.CHEVRON_RIGHT)
            .connect();
  }

  public WebBrowserController getController() {
    return myController;
  }

  public void navigate(String text) {
    myController.navigate(text);
  }

  public Parent getNode() {
    return myWholePane;
  }

  public WebView getWebView() {
    return myWebView;
  }


  public void showInStage(Stage stage) {
    Scene scene = new Scene(myWholePane);
    stage.setScene(scene);
    stage.show();
  }
}
