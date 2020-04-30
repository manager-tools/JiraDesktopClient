package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.http.errors.URLLoaderExceptionInterceptor;
import com.almworks.restconnector.operations.LoadUserInfo;
import com.almworks.util.components.plaf.patches.FontSizeSettingPatch;
import com.almworks.util.fx.AuthenticatorDialog;
import com.almworks.util.fx.FXUtil;
import com.almworks.util.fx.FxWebViewAlert;
import com.almworks.util.fx.WebBrowserController;
import com.almworks.util.i18n.text.CurrentLocale;
import com.almworks.util.i18n.text.LocalizedAccessor;
import com.sun.javafx.binding.ObjectConstant;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

class WebLoginBrowser {
  static final LocalizedAccessor I18N = CurrentLocale.createAccessor(WebLoginBrowser.class.getClassLoader(), "com/almworks/jira/provider3/app/connection/setup/weblogin/webLogin");
  public static final LocalizedAccessor.Value LOAD_FAILED_HEADER = I18N.getFactory("webLogin.browser.loadFailed.header");
  public static final LocalizedAccessor.Value LOAD_FAILED_DETAILS = I18N.getFactory("webLogin.browser.loadFailed.details");

  private static final Image DISCONNECTED = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/connect_ico_1.png"));
  private static final Image CANNOT_CONNECT = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/connect_ico_2.png"));
  private static final Image CAN_CONNECT = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/connect_ico_3.png"));
  private static final Image PREV = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/arrow_back.png"));
  private static final Image NEXT = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/arrow_forward.png"));
  private static final Image RELOAD = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/reload.png"));
  private static final Image STOP = new Image(WebLoginBrowser.class.getResourceAsStream("/com/almworks/jira/provider3/app/connection/setup/weblogin/reload.png"));

  private final VBox myWholePane = new VBox();
  private final WebView myWebView = new WebView();
  private final Label myNoContent = new Label();
  private final Label myConnectIcon = new Label();
  private final ImageView myConnectedImage = new ImageView();
  private final Label myNoJira = new Label();
  private final Label myHowConnected = new Label();
  private final Label myJiraConnected = new Label();
  private final Button myConnectButton = new Button();
  private final Button myPrevButton = new Button();
  private final Button myNextButton = new Button();
  private final Button myReloadStopButton = new Button();
  private final TextField myLocation = new TextField();
  private final ProgressBar myProgress = new ProgressBar();

  private final WebBrowserController myBrowserController = new WebBrowserController();

  public WebLoginBrowser(URLLoaderExceptionInterceptor interceptor, SSLProblemHandler sslProblemHandler) {
    myWholePane.getStylesheets().add(FXUtil.loadCssRef(this, "/com/almworks/jira/provider3/app/connection/setup/weblogin/webLogin.css"));

    myWebView.getStyleClass().add("webView");
    myNoContent.setText(I18N.getString("webLogin.browser.noContent"));
    myNoContent.getStyleClass().add("hint");

    myConnectIcon.setGraphic(myConnectedImage);
    myConnectedImage.setImage(DISCONNECTED);
    myConnectedImage.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> myConnectButton.getFont().getSize() * 1.2, myConnectButton.fontProperty()));
    myConnectedImage.setPreserveRatio(true);
    myConnectedImage.setSmooth(true);

    myNoJira.setText(I18N.getString("webLogin.panel.noJira"));

    myConnectButton.setText(I18N.getString("webLogin.panel.buttonConnect"));
    myConnectButton.getStyleClass().add("connectButton");

    setButtonImage(myPrevButton, PREV);
    myPrevButton.getStyleClass().add("navigationButton");
    setButtonImage(myNextButton, NEXT);
    myNextButton.getStyleClass().add("navigationButton");

    setButtonImage(myReloadStopButton, RELOAD);
    myBrowserController.setReloadCancelGraphics(createButtonImage(RELOAD), null);
    myReloadStopButton.getStyleClass().add("reloadButton");

    myLocation.setPromptText(I18N.getString("webLogin.panel.location.prompt"));
    myLocation.getStyleClass().add("locationText");
    myProgress.getStyleClass().add("progressBar");

    myWholePane.getChildren().addAll(createTopPanel(), createContent());
    myBrowserController.setControls(myWholePane, myWebView, myLocation, myProgress, myPrevButton, myNextButton, myReloadStopButton);
    myBrowserController.setErrorDetails(WebkitLogAnalyser.create(myWebView, interceptor, sslProblemHandler)::getDetails);

    showTextInTooltip(myHowConnected);
    showTextInTooltip(myJiraConnected);
    myHowConnected.visibleProperty().bind(myJiraConnected.visibleProperty());
    myJiraConnected.visibleProperty().bind(Bindings.not(myNoJira.visibleProperty()));
    AuthenticatorDialog.install(myBrowserController);
  }

  private void showTextInTooltip(Label label) {
    Tooltip tooltip = new Tooltip();
    label.setTooltip(tooltip);
    tooltip.textProperty().bind(label.textProperty());
  }

  public Parent getWholePane() {
    return myWholePane;
  }

  public Button getConnectButton() {
    return myConnectButton;
  }

  public WebView getWebView() {
    return myWebView;
  }

  public WebBrowserController getBrowserController() {
    return myBrowserController;
  }

  private void setButtonImage(Button button, Image image) {
    button.setGraphic(createButtonImage(image));
  }

  @NotNull
  private ImageView createButtonImage(Image image) {
    ImageView view = new ImageView(image);
    view.fitHeightProperty().bind(Bindings.createDoubleBinding(() -> myConnectButton.getFont().getSize() * 1.2, myConnectButton.fontProperty()));
    view.setPreserveRatio(true);
    view.setSmooth(true);
    return view;
  }

  @NotNull
  private GridPane createTopPanel() {
    GridPane gridPane = new GridPane();
    gridPane.getStyleClass().add("topPanel");
    StackPane statePane = new StackPane();
    statePane.getStyleClass().add("stateBox");
    VBox stateBox = new VBox();
    stateBox.getChildren().addAll(myHowConnected, myJiraConnected);
    myNoJira.setMaxWidth(Double.MAX_VALUE);
    statePane.getChildren().addAll(myNoJira, stateBox);
    GridPane.setHgrow(statePane, Priority.ALWAYS);

    myConnectButton.setMinWidth(Region.USE_PREF_SIZE);

    Pane spacer = new Pane();
    spacer.setMaxHeight(Double.MAX_VALUE);
    spacer.getStyleClass().add("spacer");
    GridPane.setFillHeight(spacer, true);

    Node location = createLocation();
    GridPane.setHgrow(location, Priority.ALWAYS);
    gridPane.getChildren().addAll(myConnectIcon, statePane,  myConnectButton, spacer, myPrevButton, myNextButton, location);

    GridPane.setConstraints(myConnectIcon, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER);
    GridPane.setConstraints(statePane, 1, 0, 1, 1, HPos.LEFT, VPos.BASELINE);
    GridPane.setConstraints(myConnectButton, 2, 0, 1, 1, HPos.LEFT, VPos.BASELINE);
    GridPane.setConstraints(spacer, 3, 0, 1, 1, HPos.LEFT, VPos.CENTER);
    GridPane.setConstraints(myPrevButton, 4, 0, 1, 1, HPos.LEFT, VPos.CENTER);
    GridPane.setConstraints(myNextButton, 5, 0, 1, 1, HPos.LEFT, VPos.CENTER);
    GridPane.setConstraints(location, 6, 0, 1, 1, HPos.LEFT, VPos.BASELINE);

    FXUtil.addFontSizeStyle(gridPane);
    return gridPane;
  }

  private Node createLocation() {
    AnchorPane pane = new AnchorPane();
    pane.getChildren().addAll(myLocation, myProgress, myReloadStopButton);
    myProgress.setMouseTransparent(true);
    AnchorPane.setLeftAnchor(myLocation, 0d);
    AnchorPane.setRightAnchor(myLocation, 0d);
    AnchorPane.setLeftAnchor(myProgress, 0d);
    AnchorPane.setRightAnchor(myProgress, 0d);
    AnchorPane.setTopAnchor(myProgress, 0d);
    AnchorPane.setBottomAnchor(myProgress, 0d);
    AnchorPane.setRightAnchor(myReloadStopButton, 0d);
    pane.getStyleClass().add("locationPane");
    return pane;
  }

  @NotNull
  private StackPane createContent() {
    StackPane stackPane = new StackPane();
    VBox.setVgrow(stackPane, Priority.ALWAYS);
    FXUtil.addFontSizeStyle(myNoContent);
    stackPane.getChildren().addAll(myWebView, myNoContent);
    ReadOnlyObjectProperty<Document> documentProperty = myWebView.getEngine().documentProperty();
    ChangeListener<Document> listener = new ChangeListener<Document>() {
      @Override
      public void changed(ObservableValue<? extends Document> observable, Document oldValue, Document newValue) {
        myNoContent.setVisible(false);
        documentProperty.removeListener(this);
      }
    };
    documentProperty.addListener(listener);
    int size = FontSizeSettingPatch.getOverrideFontSize();
    myWebView.setZoom(size > 0 ? (float) size / 12 : 1);

    return stackPane;
  }

  public void attachDetector(DetectJiraController detector) {
    myBrowserController.connect();
    new FxWebViewAlert().install(myBrowserController);

    ObservableObjectValue<DetectedJiraServer> serverProperty = detector.serverProperty();
    ObservableStringValue cantConnect = detector.cannotConnectProperty();
    myNoJira.visibleProperty().bind(Bindings.isNull(serverProperty));
    myHowConnected.textProperty().bind(Bindings.createStringBinding(() -> {
      DetectedJiraServer server = serverProperty.get();
      if (server == null) return "";
      LoadUserInfo myself = server.getMyself();
      String displayableUsername = myself != null ? myself.getDisplayName() : null;
      return displayableUsername == null ? I18N.getString("webLogin.panel.connected.anonymous")
              : I18N.messageStr("webLogin.panel.connected.account").formatMessage(displayableUsername);
    }, serverProperty));
    myJiraConnected.textProperty().bind(Bindings.createStringBinding(() -> {
      DetectedJiraServer server = serverProperty.get();
      if (server == null) return "";
      return I18N.messageStr("webLogin.panel.connected.serverName").formatMessage(server.getServerInfo().getServerTitle());
    }, serverProperty));
    myConnectedImage.imageProperty().bind(Bindings.createObjectBinding(() -> {
      DetectedJiraServer server = serverProperty.get();
      if (server == null) return DISCONNECTED;
      String reason = cantConnect.get();
      return reason != null ? CANNOT_CONNECT : CAN_CONNECT;
    }, serverProperty, cantConnect));
    myConnectIcon.tooltipProperty().bind(Bindings.createObjectBinding(() -> {
      DetectedJiraServer server = serverProperty.get();
      if (server == null) return null;
      String reason = cantConnect.get();
      return reason == null ? null : new Tooltip(reason);
    }, serverProperty, cantConnect));
    myConnectButton.disableProperty().bind(Bindings.createBooleanBinding(
            () -> serverProperty.get() == null || cantConnect.get() != null,
            serverProperty, cantConnect));
    detector.attachWebView(myWebView);
  }

  private static class WebkitLogAnalyser {
    /**
     * Collects exceptions to this list to find the reason of "unknown error"
     */
    private final List<LogRecord> myExceptions = new ArrayList<>();
    private final SSLProblemHandler mySSLProblemHandler;

    public WebkitLogAnalyser(SSLProblemHandler sslProblemHandler) {
      mySSLProblemHandler = sslProblemHandler;
    }

    public static WebkitLogAnalyser create(WebView webView, URLLoaderExceptionInterceptor interceptor, SSLProblemHandler sslProblemHandler) {
      WebkitLogAnalyser analyser = new WebkitLogAnalyser(sslProblemHandler);
      interceptor.addProcessor(analyser::processException);
      webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
        switch (newValue) {
          case SCHEDULED:
          case SUCCEEDED:
          case CANCELLED:
            analyser.clearLog();
            break;
        }
      });
      return analyser;
    }

    private void clearLog() {
      synchronized (myExceptions) {
        myExceptions.clear();
      }
    }

    /**
     * This method can handle WebKit URL loader logged exception. The log records may help to find out the cause of unknown errors.
     * @param logRecord log record to process
     */
    private void processException(LogRecord logRecord) {
      if (logRecord.getLevel().intValue() < Level.WARNING.intValue()
              && SNIErrorHandler.isMyWebkitMessage(logRecord) == null) return;
      synchronized (myExceptions) {
        myExceptions.add(logRecord);
      }
    }

    public Node getDetails(Throwable throwable) {
      String message = throwable.getMessage();
      VBox box = new VBox();
      box.setStyle("-fx-spacing: .25em; -fx-min-width: 15em");
      box.getChildren().add(new Label(LOAD_FAILED_HEADER.create()));
      if ("unknown error".equalsIgnoreCase(message)) {
        ArrayList<LogRecord> list;
        synchronized (myExceptions) {
          list = new ArrayList<>(myExceptions);
        }
        for (LogRecord record : list) {
          String url = SNIErrorHandler.isMyWebkitMessage(record);
          if (url != null) {
            Hyperlink hyperlink = new Hyperlink(LOAD_FAILED_DETAILS.create());
            hyperlink.setFocusTraversable(false);
            hyperlink.visitedProperty().bind(ObjectConstant.valueOf(false));
            hyperlink.setOnAction(event -> mySSLProblemHandler.showSNIError(URI.create(url)));
            hyperlink.setStyle("-fx-padding: 0; -fx-border-width: 0");
            box.getChildren().addAll(
                    new Label(message),
                    hyperlink);
            return box;
          }
          //noinspection ThrowableResultOfMethodCallIgnored
          Throwable thrown = record.getThrown();
          if (thrown != null) message = thrown.getMessage();
        }
      }
      box.getChildren().add(new Label(message));
      return box;
    }
  }

  private static class WebViewFontScale implements EventHandler<Event> {
    private final WebView myWebView;
    private boolean myControlDown = false;
    private int myScaleFactor = 0;
    private final double myBaseScale;
    private final double myScaleExponent = 1.05;
    private final DoubleProperty myScale = new SimpleDoubleProperty(1);


    public WebViewFontScale(WebView webView) {
      int size = FontSizeSettingPatch.getOverrideFontSize();
      myBaseScale = Math.max(0.9, Math.min(1.1, size > 0 ? (float) size / 12 : 1));
      myWebView = webView;
    }

    public void install() {
      myWebView.addEventHandler(ScrollEvent.SCROLL, this);
      myWebView.addEventHandler(KeyEvent.KEY_PRESSED, this);
      myWebView.addEventHandler(KeyEvent.KEY_RELEASED, this);
      myWebView.zoomProperty().bind(myScale);
    }

    @Override
    public void handle(Event event) {
      KeyEvent keyEvent = Util.castNullable(KeyEvent.class, event);
      if (keyEvent != null) onKey(keyEvent);
      ScrollEvent scrollEvent = Util.castNullable(ScrollEvent.class, event);
      if (scrollEvent != null) onScroll(scrollEvent);
    }

    private void onScroll(ScrollEvent event) {
      if (!myControlDown) return;
      int ticks = (int) Math.round(event.getDeltaY() / event.getMultiplierY());
      myScaleFactor += ticks;
      myScale.setValue(myBaseScale*Math.pow(myScaleExponent, myScaleFactor));
    }

    private void onKey(KeyEvent event) {
      boolean down;
      if (event.getEventType() == KeyEvent.KEY_PRESSED) down = true;
      else if (event.getEventType() == KeyEvent.KEY_RELEASED) down = false;
      else return;
      if (event.getCode() == KeyCode.CONTROL) myControlDown = down;
    }
  }
}
