package com.almworks.jira.provider3.app.connection.setup.weblogin;

import com.almworks.api.http.HttpClientProvider;
import com.almworks.container.TestContainer;
import com.almworks.gui.DefaultDialogManager;
import com.almworks.http.HttpLoaderFactoryImpl;
import com.almworks.http.HttpMaterialFactory;
import com.almworks.http.errors.SNIErrorHandler;
import com.almworks.http.errors.SSLProblemHandler;
import com.almworks.http.errors.URLLoaderExceptionInterceptor;
import com.almworks.util.config.Configuration;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.almworks.util.detach.Lifespan;

public class WebLoginSample extends Application {
  @Override
  public void start(Stage stage) throws Exception {
    System.setProperty("font.size.abs", "20"); // Test font
    SNIErrorHandler sniErrorHandler = new SNIErrorHandler(null);
    URLLoaderExceptionInterceptor interceptor = new URLLoaderExceptionInterceptor();
    interceptor.install(Lifespan.FOREVER);
    WebLoginBrowser panel = new WebLoginBrowser(interceptor, new SSLProblemHandler(new DefaultDialogManager(Configuration.EMPTY_CONFIGURATION, new TestContainer())));
    JiraServerDetector detector = JiraServerDetector.create(new HttpMaterialFactory(HttpClientProvider.SIMPLE, new HttpLoaderFactoryImpl()), false, sniErrorHandler, null);
    panel.attachDetector(new DetectJiraController(detector, null, null));

    stage.setScene(new Scene(panel.getWholePane()));
    stage.sizeToScene();
    stage.show();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
