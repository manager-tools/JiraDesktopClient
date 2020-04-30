package com.almworks.platform.about;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.install.Setup;
import com.almworks.api.install.TrackerProperties;
import com.almworks.api.misc.WorkArea;
import com.almworks.api.platform.ProductInformation;
import com.almworks.gui.WindowTitleSections;
import com.almworks.platform.DiagnosticRecorder;
import com.almworks.util.GlobalProperties;
import com.almworks.util.LogHelper;
import com.almworks.util.commons.Procedure;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.io.IOUtils;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.ThreadSafe;
import org.almworks.util.Collections15;
import org.almworks.util.TypedKey;
import org.almworks.util.Util;
import org.jetbrains.annotations.Nullable;
import org.picocontainer.Startable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ApplicationAbout implements Startable {
  public static final Role<ApplicationAbout> ROLE = Role.role(ApplicationAbout.class);
  private final ProductInformation myProductInfo;
  @org.jetbrains.annotations.NotNull
  private final WorkArea myWorkArea;
  private final DiagnosticRecorder myDiagnostic;
  private final WindowTitleSections myWindowTitle;

  private volatile String myVersionText = null;
  private final Map<Object, Object> myAboutProperties = Collections15.hashMap();

  public ApplicationAbout(ProductInformation pi, WorkArea workArea, DiagnosticRecorder diagnostic, WindowTitleSections windowTitle) {
    myProductInfo = pi;
    myWorkArea = workArea;
    myDiagnostic = diagnostic;
    myWindowTitle = windowTitle;
    LogHelper.warning("Starting", pi.getName(), ", Workspace:",workArea.getRootDir(), ", Home:", workArea.getHomeDir());
    LogHelper.warning("Version: ", pi.getVersion() + " (" + pi.getVersionType() + "), build " + pi.getBuildNumber());
    setupAbout();
  }

  private void setupAbout() {
    StringBuilder builder = new StringBuilder();
    builder
      .append(Setup.getProductName())
      .append(" ")
      .append(myProductInfo.getVersion());
    if (!myProductInfo.isProductionVersion()) builder.append(" build ").append(myProductInfo.getBuildNumber().toDisplayableString());
    setVersionText(builder.toString());

    Map<Object, Object> properties = Collections15.hashMap();
    addProductInfo(properties, myProductInfo);
    properties.put("LicenseType", "UNSUPPORTED");
    addAboutProperties(properties);
  }

  private static final DateFormat SHORTEST_FORMAT = DateUtil.LOCAL_DATE;
  private static void addProductInfo(Map<Object, Object> properties, ProductInformation productInfo) {
    properties.put("productName", Setup.getProductName());
    properties.put("version", productInfo.getVersion());
    properties.put("buildNumber", productInfo.getBuildNumber().toDisplayableString());
    properties.put("buildDate", SHORTEST_FORMAT.format(productInfo.getBuildDate()));
  }

  @Override
  public void start() {
    myDiagnostic.addSessionInfo(new LogVersionAndEnvironment());
    setupWindowTitle();
  }

  private static final TypedKey<String> PRODUCT = TypedKey.create("product");
  private static final TypedKey<String> WORKSPACE = TypedKey.create("workspace");
  private void setupWindowTitle() {
    ThreadGate.AWT.execute(() -> {
      myWindowTitle.appendSection(PRODUCT, Setup.getProductName());
      if(Setup.isWorkspaceExplicit()) {
        File dir = Setup.getWorkspaceDir();
        if (dir == null) LogHelper.error("Null workspace");
        else {
          String workarea = dir.getAbsolutePath();
          if (workarea.endsWith(File.separator))
            workarea = workarea.substring(0, workarea.length() - File.separator.length());
          myWindowTitle.appendSection(WORKSPACE, " - ", workarea);
        }
      }
    });
  }

  @Override
  public void stop() {
  }

  @ThreadSafe
  @Nullable
  public String getVersionText() {
    return myVersionText;
  }

  @ThreadSafe
  public void setVersionText(String versionText) {
    myVersionText = versionText;
    updateSplash();
  }

  @ThreadSafe
  @Nullable
  public String getLicenseHtml() {
    return "UNSUPPORTED";
  }

  private static final MessageFormat SPLASH_HTML;
  static {
    try (InputStream stream = ApplicationAbout.class.getResourceAsStream("/com/almworks/platform/about/splash.html")) {
      SPLASH_HTML = new MessageFormat(IOUtils.transferToString(stream, "UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @ThreadSafe
  private void updateSplash() {
    SplashStage.perform(splashStage -> splashStage.setHtml(SPLASH_HTML.format(new Object[]{
            Util.NN(getVersionText()),
            Util.NN(getLicenseHtml())
    })));
  }

  public static void installAndShowSplash(MutableComponentContainer container) {
    container.registerActorClass(ROLE, ApplicationAbout.class);
  }

  @ThreadSafe
  public Map<?, ?> getAboutProperties() {
    synchronized (myAboutProperties) {
      return Collections15.hashMap(myAboutProperties);
    }
  }

  @ThreadSafe
  public void addAboutProperties(Map<?, ?> properties) {
    synchronized (myAboutProperties) {
      myAboutProperties.putAll(properties);
    }
  }

  private class LogVersionAndEnvironment implements Procedure<Logger> {
    @Override
    public void invoke(Logger logger) {
      ProductInformation pi = myProductInfo;
      WorkArea wa = myWorkArea;
      logger.info("Starting " + pi.getName());
      logger.info("Workspace: " + wa.getRootDir());
      logger.info("Home: " + wa.getHomeDir());
      logger.info(pi.getVersion() + " (" + pi.getVersionType() + "), build " + pi.getBuildNumber());

      String[] logProps = {
        "java.version", "java.vm.version", "java.vm.vendor", "java.vm.name", "java.vm.info", "java.specification.version", "os.name",
        "os.version", "os.arch", "sun.os.patch.level", "sun.arch.data.model", "sun.cpu.endian", "user.country",
        "user.timezone", "file.encoding", "user.language",};
      for(String prop : logProps) {
        logger.info(prop + " = " + System.getProperty(prop));
      }

      Runtime runtime = Runtime.getRuntime();
      logger.info("cpus = " + runtime.availableProcessors());
      logger.info("mx = " + FileUtil.getMemoryMegs(runtime.maxMemory()) + "m");
      logger.info("mem = " + FileUtil.getMemoryMegs(runtime.totalMemory()) + "m");

      boolean assertions = false;
      try {
        //noinspection AssertWithSideEffects
        assert assertions = true;
      } catch (AssertionError e) {
        assertions = true;
      }
      //noinspection ConstantConditions
      logger.info("assertions = " + (assertions ? "on" : "off"));

      Properties properties = System.getProperties();
      for(Map.Entry<Object, Object> entry : properties.entrySet()) {
        String key = String.valueOf(entry.getKey());
        if(TrackerProperties.hasProperty(key) || GlobalProperties.hasProperty(key)) {
          logger.info(entry.getKey() + " = " + entry.getValue());
        }
      }
    }
  }
}
