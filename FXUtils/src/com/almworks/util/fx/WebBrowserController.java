package com.almworks.util.fx;


import com.almworks.util.LogHelper;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class WebBrowserController {
  private static boolean ourConsoleSetup = false;

  private WebView myWebView;
  @Nullable
  private Node myKeyboardHandler;
  @Nullable
  private TextField myLocation;
  @Nullable
  private ProgressBar myProgressBar;
  @Nullable
  private Button myReload;
  @Nullable
  private Button myForward;
  @Nullable
  private Button myBack;

  private final SimpleStringProperty myLastLocation = new SimpleStringProperty("");
  @SuppressWarnings("unchecked")
  private final FireEventSupport<BiConsumer<WebEvent<String>, WebView>> myAlertListeners = (FireEventSupport)FireEventSupport.create(BiConsumer.class);
  private final SimpleObjectProperty<Node> myReloadIcon = new SimpleObjectProperty<>();
  private final SimpleObjectProperty<Node> myCancelIcon = new SimpleObjectProperty<>();
  @Nullable
  private PopOverHelper.WithClose myErrorPopOver;
  @Nullable
  private Function<Throwable, Node> myErrorDetails;

  public WebBrowserController() {
    setReloadCancelFontIcons(FontAwesomeIcon.REPEAT, FontAwesomeIcon.REMOVE);
  }

  public void connect() {
    if (myWebView == null) return;
    WebEngine engine = myWebView.getEngine();
    engine.locationProperty().addListener((observable, oldValue, newValue) -> myLastLocation.setValue(newValue));
    if (myLocation != null) {
      myLocation.setOnAction(event -> navigate(myLocation.getText()));
      myLastLocation.addListener((observable, oldValue, newValue) -> myLocation.setText(newValue));
    }
    if (myProgressBar != null) Progress.install(engine, myProgressBar);
    if (myReload != null) {
      myReload.disableProperty().bind(Bindings.isEmpty(myLastLocation));
      myReload.graphicProperty().bind(Bindings.createObjectBinding(
              () -> engine.getLoadWorker().isRunning() ? myCancelIcon.get() : myReloadIcon.get(),
              engine.getLoadWorker().runningProperty(), myReloadIcon, myCancelIcon));
      myReload.setOnAction(event -> {
        if (engine.getLoadWorker().isRunning()) engine.getLoadWorker().cancel();
        else engine.load(myLastLocation.get());
      });
    }
    if (myKeyboardHandler != null)
      myKeyboardHandler.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
        if (event.getCode() == KeyCode.L && event.isShortcutDown() && myLocation != null) myLocation.requestFocus();
        else if (event.getCode() == KeyCode.ESCAPE && engine.getLoadWorker().isRunning()) engine.getLoadWorker().cancel();
      });
    myErrorPopOver = createErrorPopOver();
    setupHistory();
    setupConsole();
    setupAlert(engine);
    setupPopups(engine);
    setupWorkerExceptionHandler();
  }

  private void setupPopups(WebEngine engine) {
    engine.setCreatePopupHandler(param -> {
      WebBrowser browser = new WebBrowser();
      Stage stage = new Stage();
      browser.showInStage(stage);
      return browser.getWebView().getEngine();
    });
  }

  private void setupWorkerExceptionHandler() {
    TextField location = myLocation;
    if (location == null) return;
    myWebView.getEngine().getLoadWorker().exceptionProperty().addListener((observable, oldValue, exception) -> {
      if (exception != null) {
        ThreadGate.FX_OPTIMAL.execute(() -> {
          LogHelper.warning("WebBrowser.Worker:", exception.getMessage(), myWebView.getEngine().getLocation());
          LogHelper.debug("WebBrowser.Worker:", myWebView.getEngine().getLocation(), exception);
          if (myErrorPopOver != null) {
            Node message = myErrorDetails != null ? myErrorDetails.apply(exception) : null;
            if (message == null) message = new Label("Error loading page: " + exception.getLocalizedMessage());
            myErrorPopOver.show(message);
          }
        });
      }
    });
  }

  private void setupAlert(WebEngine engine) {
    EventHandler<WebEvent<String>> alert = engine.getOnAlert();
    if (alert != null) LogHelper.warning(new Exception("WebView.onAlert is already set"));
    engine.setOnAlert(event -> {
      LogHelper.warning("WebView alert", event.getData(), "at:", myLastLocation.get());
      myAlertListeners.getDispatcher().accept(event, myWebView);
    });
    engine.onAlertProperty().addListener((observable, oldValue, newValue) -> LogHelper.error("WebView.onAlert modified", newValue));
  }

  private void setupHistory() {
    WebHistory history = myWebView.getEngine().getHistory();
    if (myBack != null) {
      myBack.disableProperty().bind(Bindings.equal(0, history.currentIndexProperty()));
      myBack.setOnAction(event -> history.go(-1));
    }
    if (myForward != null) {
      myForward.disableProperty().bind(Bindings
              .createBooleanBinding(() -> history.getCurrentIndex() >= history.getEntries().size() - 1, history.currentIndexProperty(), history.getEntries()));
      myForward.setOnAction(event -> history.go(1));
    }
  }

  public WebBrowserController setControls(Node keyboardHandler, WebView webView, TextField location, ProgressBar progress,
                          Button back, Button forward, Button reload) {
    myKeyboardHandler = keyboardHandler;
    myWebView = webView;
    myLocation = location;
    myProgressBar = progress;
    myBack = back;
    myForward = forward;
    myReload = reload;
    return this;
  }

  public WebBrowserController setErrorDetails(@Nullable Function<Throwable, Node> errorDetails) {
    myErrorDetails = errorDetails;
    return this;
  }

  public WebBrowserController setBackForwardFontIcons(FontAwesomeIcon back, FontAwesomeIcon forward) {
    if (myBack != null && back != null) myBack.setGraphic(new FontAwesomeIconView(back));
    if (myForward != null && forward != null) myForward.setGraphic(new FontAwesomeIconView(forward));
    return this;
  }

  public final WebBrowserController setReloadCancelFontIcons(FontAwesomeIcon reload, FontAwesomeIcon cancel) {
    setReloadCancelGraphics(reload != null ? new FontAwesomeIconView(reload) : null,
            cancel != null ? new FontAwesomeIconView(cancel) : null);
    return this;
  }

  public final WebBrowserController setReloadCancelGraphics(Node reload, Node cancel) {
    if (reload != null) myReloadIcon.setValue(reload);
    if (cancel != null) myCancelIcon.setValue(cancel);
    return this;
  }

  public WebBrowserController addAlertListener(BiConsumer<WebEvent<String>, WebView> listener) {
    myAlertListeners.addListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, listener);
    return this;
  }

  @SuppressWarnings("unused")
  public WebBrowserController apply(Consumer<? super WebBrowserController> consumer) {
    if (consumer != null) consumer.accept(this);
    return this;
  }

  public static void setupConsole() {
    if (ourConsoleSetup) return;
    ourConsoleSetup = true;
    com.sun.javafx.webkit.WebConsoleListener.setDefaultListener((webView, message, lineNumber, sourceId) ->
            LogHelper.debug("JS console:", message, "at [" + sourceId + ":" + lineNumber, "]", webView));
  }

  private static final Pattern PROTOCOL_PREFIX = Pattern.compile("^(http|https)://");
  public void navigate(String text) {
    text = Util.NN(text);
    String lower = Util.lower(text).trim();
    if (!PROTOCOL_PREFIX.matcher(lower).find()) text = "http://" + text;
    myWebView.getEngine().load(text);
  }

  @Nullable
  private PopOverHelper.WithClose createErrorPopOver() {
    if (myLocation == null) return null;
    PopOverHelper popOverHelper = new PopOverHelper(myLocation)
            .setFill(Color.valueOf("#FFE1D8"))  // Same as in CSS
            .setPopOverLocation(PopOverHelper.LOCATION_TOP_LEFT)
            .addStylesheet(FXUtil.loadCssRef(this, "/com/almworks/util/fx/errorPopOver.css"));
    PopOverHelper.WithClose errorPopOver = new PopOverHelper.WithClose(popOverHelper)
            .setContentClass("message");
    myLocation.textProperty().addListener((observable, oldValue, newValue) -> errorPopOver.hide());
    myLocation.caretPositionProperty().addListener((observable, oldValue, newValue) -> errorPopOver.hide());
    return errorPopOver;
  }

  public WebView getWebView() {
    return myWebView;
  }

  private static class Progress {
    public static void install(WebEngine engine, ProgressBar progressBar) {
      Worker<Void> worker = engine.getLoadWorker();
      progressBar.setProgress(0);
      worker.progressProperty().addListener((observable, oldValue, newValue) -> {
        if (!worker.isRunning()) return;
        double progress = newValue.doubleValue();
        if (progress <= 0) progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        else progressBar.setProgress(progress);
      });
      worker.runningProperty().addListener((observable, oldValue, running) -> {
        if (running) {
          if (progressBar.getProgress() == 0)
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        } else {
          progressBar.setProgress(0);
        }
      });
      progressBar.visibleProperty().bind(worker.runningProperty());
    }
  }
}
