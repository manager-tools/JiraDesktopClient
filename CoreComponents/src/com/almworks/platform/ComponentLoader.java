package com.almworks.platform;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.container.ComponentContainerDataProvider;
import com.almworks.api.container.MissingContainerFactoryException;
import com.almworks.api.container.RootContainer;
import com.almworks.api.container.RootContainerFactory;
import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import com.almworks.api.misc.CommandLine;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.platform.ProductInformation;
import com.almworks.platform.about.ApplicationAbout;
import com.almworks.platform.about.SplashStage;
import com.almworks.util.ApacheLoggerInstaller;
import com.almworks.util.DecentFormatter;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.LongEventQueue;
import com.almworks.util.exec.LongEventQueueEnv;
import com.almworks.util.exec.LongEventQueueImpl;
import com.almworks.util.threads.MainThreadGroup;
import com.almworks.util.ui.MacIntegration;
import javafx.application.Application;
import javafx.stage.Stage;
import org.almworks.util.Collections15;
import org.almworks.util.Failure;
import org.almworks.util.Log;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ComponentLoader {
  private static final String LAUNCHER_DEBUG_DEFAULT_COMPONENTS = "com/almworks/rc/DebugComponents.xml";

  private ProductInformation myProductInfo;
  private WorkAreaImpl myWorkArea;

  public static void launch(final String[] args) {
    new Thread(() -> MockApp.launch(MockApp.class, args), "FX startup").start();
    installEnvImpl();
    MainThreadGroup.getOrCreate().startThread("loader", new Runnable() {
      public void run() {
        try {
          new ComponentLoader(args).run();
        } catch (Throwable t) {
          if (t instanceof LicenseFailure) {
            System.err.println("LICENSE FAILURE");
            System.exit(2);
          } else if (t instanceof ControlPassedException) {
            System.err.println("APPLICATION IS RUNNING");
            System.exit(3);
          }
          System.err.println("FATAL ERROR");
          t.printStackTrace(System.err);
          try {
            Log.error("FATAL ERROR", t);
          } catch (Throwable t2) {
            System.err.println("Additionally, the following error encountered when trying to log the fatal error");
            t2.printStackTrace(System.err);
          }
          System.exit(1);
        }
      }
    });
  }

  public static List<String> getUrlsToOpen(String[] args) {
    final String option = "--open=";
    final List<String> urls = Collections15.arrayList();
    for(final String arg : args) {
      if(arg.startsWith(option) && arg.length() > option.length()) {
        urls.add(arg.substring(option.length()));
      }
    }
    return urls;
  }

  private static void installEnvImpl() {
    Env.setImpl(new AppEnvImpl());
  }

  private final String[] myCommandLine;
  private ComponentConfiguration myComponentConfiguration;
  private RootContainer myContainer;
  private List<ComponentDescriptor> myComponents = new ArrayList<>();

  private ComponentLoader(String[] commandLine) {
    myCommandLine = commandLine;
  }

  private void run() throws InterruptedException, ControlPassedException {
    SplashStage.perform(SplashStage::show);
    startAWT();
    createWorkArea();
    createLongEventQueue();
    createContainer();
    lockWorkArea();
    tuneLogging();
    ApplicationAbout.installAndShowSplash(myContainer);
    analyzeComponents();
    instantiateComponentDescriptors();
    registerComponents();
    prepareContext();
    startApplication();
    ApplicationLoadStatusImpl startup = myContainer.getActor(ApplicationLoadStatusImpl.class);
    if (startup != null) startup.allowStartup();
  }

  private void prepareContext() {
    Context.add(new ComponentContainerDataProvider(myContainer), "container");
    Context.globalize();
  }

  private void createLongEventQueue() {
    LongEventQueueImpl queue = new LongEventQueueImpl(new LongEventQueueEnv() {
      public boolean getSingleThread() {
        return Env.getBoolean(GlobalProperties.SINGLE_WORKER);
      }
    });

    // this was causing context cycles 8-[]
//    Context.add(InstanceProvider.instance(queue), "LongEventQueue");
    LongEventQueue.installStatic(queue);
  }

  private void createWorkArea() {
    maybeMigrateWorkArea10();
    myWorkArea = new WorkAreaImpl(getUrlsToOpen(myCommandLine));
  }

  private void maybeMigrateWorkArea10() {
    if (!Setup.isWorkspaceOldfashioned())
      return;
    String userHome = System.getProperty("user.home");
    if (userHome == null)
      return;
    File dir12;
    try {
      dir12 = new File(userHome, Setup.getWorkspaceDirectoryInUserHome());
    } catch (Exception e) {
      // ignore
      return;
    }
    if (dir12.exists()) {
      // already exists - ignore
      return;
    }
    File dir10 = Setup.getWorkspaceDir();
    assert dir10.equals(new File(Setup.getHomeDir(), Setup.DIR_WORKSPACE_OLD_FASHIONED));
    WorkspaceMigration12.migrate(dir10, dir12);
  }

  private void startAWT() throws InterruptedException {
    try {
      SwingUtilities.invokeAndWait(() -> {
        Thread.currentThread().setContextClassLoader(ComponentLoader.class.getClassLoader());
        LAFUtil.initializeLookAndFeel();
        Log.debug("laf installed");
        if(Env.isMac()) {
          MacIntegration.installMacHandlers();
        }
      });
    } catch (InvocationTargetException e) {
      throw new Failure(e);
    }
  }

  private void createContainer() {
    try {
      RootContainerFactory factory = RootContainerFactory.findFactory();
      Log.debug("creating container with " + factory);
      myContainer = factory.create();
      myProductInfo = new ProductInformationImpl2();
      myContainer.registerStartupActor(CommandLine.ROLE, new CommandLineImpl(myCommandLine));
      myContainer.registerStartupActor(WorkArea.APPLICATION_WORK_AREA, myWorkArea);
      myContainer.registerActor(ProductInformation.ROLE, myProductInfo);
      myContainer.registerActorClass(ApplicationLoadStatus.ROLE, ApplicationLoadStatusImpl.class);
    } catch (MissingContainerFactoryException e) {
      throw new Failure(e);
    }
  }

  private void lockWorkArea() throws ControlPassedException {
    myWorkArea.start();
  }

  private void analyzeComponents() {
    File hintsDir = myWorkArea.getComponentHintsDir();
    File componentDir = new File(Setup.getHomeDir(), Setup.DIR_COMPONENTS);
    assert Log.debug("components = " + componentDir);
    assert Log.debug("component hints = " + hintsDir);

    myComponentConfiguration = ComponentConfiguration.buildConfiguration(componentDir, hintsDir);
    assert Log.debug("component configuration loaded: " + myComponentConfiguration.getUsedJarFiles().size() +
      " jars; " + myComponentConfiguration.getComponentClassNames().size() + " component candidates");

    try {
      myComponentConfiguration.addDebugComponents(
        getClass().getClassLoader().getResourceAsStream(getDebugComponentsResource()));
    } catch (Exception e) {
      Log.debug(e);
    }
  }

  private static String getDebugComponentsResource() {
    return Env.getString(TrackerProperties.DEBUG_COMPONENTS, LAUNCHER_DEBUG_DEFAULT_COMPONENTS);
  }

  private void instantiateComponentDescriptors() {
    for (Iterator<String> it = myComponentConfiguration.getComponentClassNames().iterator(); it.hasNext();) {
      String className = it.next();
      try {
        Class<ComponentDescriptor> clazz = ((Class<ComponentDescriptor>) getClass().getClassLoader()
          .loadClass(className));
        ComponentDescriptor descriptor = clazz.newInstance();
        myComponents.add(descriptor);
      } catch (Exception e) {
        Log.warn("could not instantiate component descriptor of class (" + className + "), ignoring", e);
      }
    }
  }

  /**
   * Tune logging handlers by following rules (based on ProductInformation and <code>tracker.debug</code> property):
   * <table border="1">
   * <tr><th>versionType</th><th>tracker.debug property</th><th>FileHandler</th><th>ConsoleHandler</th></tr>
   * <tr><td rowspan="2">EAP/DEBUG</td><td><code>true<code></td><td rowspan="3">Level.<b>FINEST</b></td><td>Level.<b>INFO</b></td></tr>
   * <tr><td>-</td><td rowspan="2">Level.<b>WARNING</b></td></tr>
   * <tr><td rowspan="2">Other</td><td><code>true</code></tr>
   * <tr><td>-</td><td>Level.<b>INFO</b></td><td>Level.<b>ERROR</b></td></tr>
   * </table>
   */
  private void tuneLogging() {
    ApacheLoggerInstaller.install();

    Logger logger = Log.getRootLogger();
    // leaving FINE level on root logger may result in severe performance degradation, esp. in jdk 1.6
    logger.setLevel(Level.WARNING);
    Handler[] handlers = createHandlers();
    Log.replaceHandlers(logger, handlers);

    Log.getApplicationLogger().setLevel(Level.FINE);
    Logger.getLogger("com.almworks").setLevel(Level.FINE);
  }

  private Handler[] createHandlers() {
    boolean development = !myProductInfo.isProductionVersion();
    boolean specialDebug = com.almworks.util.Env.getBoolean(TrackerProperties.DEBUG);
    DecentFormatter formatter = new DecentFormatter();

    ConsoleHandler consoleHandler = new ConsoleHandler();
    consoleHandler.setFormatter(formatter);
    consoleHandler.setLevel(getLoggingLevelConsoleHandler(development, specialDebug));

    Handler fileHandler = getLoggingFileHandler();
    if (fileHandler != null) {
      fileHandler.setFormatter(formatter);
      fileHandler.setLevel(getLoggingLevelFileHandler(development, specialDebug));
      try {
        fileHandler.setEncoding("UTF-8");
      } catch (UnsupportedEncodingException e) {
        // ignore
      }
    }

    return fileHandler == null ? new Handler[] {consoleHandler} : new Handler[] {consoleHandler, fileHandler};
  }

  private static Level getLoggingLevelFileHandler(boolean development, boolean specialDebug) {
    return specialDebug ? Level.FINE : (development ? Level.FINE : Level.WARNING);
  }

  private static Level getLoggingLevelConsoleHandler(boolean development, boolean specialDebug) {
    return development ? (specialDebug ? Level.INFO : Level.WARNING) : (specialDebug ? Level.WARNING : Level.SEVERE);
  }

  private static Handler getLoggingFileHandler() {
    Handler newHandler = null;
    try {
      newHandler = Setup.getLoggingFileHandler();
    } catch (IOException e) {
      // fall through
    }
    return newHandler;
  }

  private void registerComponents() {
    if (myComponents.size() == 0) {
      throw new Failure("No application components are found.");
    }
    for (ComponentDescriptor descriptor : myComponents) {
      descriptor.registerActors(myContainer);
      assert Log.debug("descriptor " + descriptor + " registered");
    }
  }

  private void startApplication() {
    if (Env.getBoolean(TrackerProperties.DEBUG)) {
      myContainer.startWithDebugging();
    } else {
      myContainer.start();
    }
    assert Log.debug("application started");
  }

  public void abort(int exitCode, String message, Throwable throwable) {
    Log.error(message, throwable);
    System.exit(exitCode);
  }

  private static class LicenseFailure extends Failure {
  }


  public static class MockApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
      SplashStage.init(primaryStage);
    }
  }
}