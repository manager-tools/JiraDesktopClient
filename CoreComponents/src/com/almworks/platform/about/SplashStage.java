package com.almworks.platform.about;

import com.almworks.api.install.Setup;
import com.almworks.util.NoObfuscation;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.fx.FXUtil;
import com.almworks.util.threads.ThreadFX;
import com.almworks.util.threads.ThreadSafe;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.almworks.util.RuntimeInterruptedException;

import java.util.function.Consumer;

public class SplashStage {
  private static final Image ICON = new Image(SplashStage.class.getResourceAsStream("/com/almworks/rc/i64.png"));
  private static SplashStage ourInstance;
  private static final Object ourLock = new Object();

  private final Stage myStage;
  private final Form myForm = new Form();

  private SplashStage(Stage stage) {
    myStage = stage;
    myStage.initStyle(StageStyle.UNDECORATED);
    myForm.myWebPane.setVisible(false);
    Scene scene = new Scene(myForm.myWholePane);
    myStage.setScene(scene);
    myStage.sizeToScene();
    myStage.getIcons().add(ICON);
    myStage.setTitle(Setup.getProductName());
  }

  @ThreadFX
  public static void init(Stage stage) {
    synchronized (ourLock) {
      if (ourInstance != null) return;
      ourInstance = new SplashStage(stage);
      ourLock.notifyAll();
    }
  }

  @ThreadFX
  public void show() {
    myStage.show();
  }

  @ThreadFX
  public void hide() {
    myStage.hide();
  }

  @ThreadFX
  public void setHtml(String html) {
    myForm.myWebPane.setVisible(true);
    myForm.myWebView.getEngine().loadContent(html, "text/html");
  }

  @ThreadSafe
  public static void perform(Consumer<SplashStage> operation) {
    SplashStage splashStage;
    synchronized (ourLock) {
      while (true) {
        splashStage = SplashStage.ourInstance;
        if (splashStage != null) break;
        try {
          ourLock.wait(50);
        } catch (InterruptedException e) {
          throw new RuntimeInterruptedException(e);
        }
      }
    }
    SplashStage finalStage = splashStage;
    ThreadGate.FX_OPTIMAL.execute(() -> operation.accept(finalStage));
  }

  private static class Form implements NoObfuscation {
    @FXML
    private StackPane myWebPane;
    @FXML
    private StackPane myWholePane;
    @FXML
    private WebView myWebView;

    public Form() {
      FXUtil.loadFxml(this, "/com/almworks/launcher/splash.fxml", null);
    }
  }
}
