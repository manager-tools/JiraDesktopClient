package com.almworks.engine.items;

import com.almworks.api.install.Setup;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.Env;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressUtil;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

public class ExportTags {
  private static final String PATH_TO_TAGEXPORTER_3_JAR = "path.to.tagerxporter3.jar";
  private final File myTagExporter3Jar;
  private final File myTargetFile;
  private volatile boolean myHadErrors;
  
  private ExportTags(File tagExporter3Jar, File targetFile) {
    myTagExporter3Jar = tagExporter3Jar;
    myTargetFile = targetFile;
  }
  
  public static ExportTags create(File targetFile, WorkArea workArea) {
    File tagExporterJarFile = getTagExporterJarFile(workArea);
    if (!tagExporterJarFile.isFile()) {
      Log.warn("tagexporter3.jar cannot be found; '" + tagExporterJarFile + "' is not a file");
      return null;
    }
    return new ExportTags(tagExporterJarFile, targetFile);
  }

  private static File getTagExporterJarFile(WorkArea workArea) {
    String overridingProp = Env.getString(PATH_TO_TAGEXPORTER_3_JAR);
    if (overridingProp != null) {
      File f = new File(overridingProp);
      if (f.isFile()) {
        Log.warn("Using path to tagexporter3.jar from properties: '" + overridingProp + '\'');
        return f;
      } else {
        Log.warn("Path to tagexporter3.jar from properties is invalid, using standard");
      }
    }
    File f = new File(workArea.getInstallationEtcDir(), "tagexporter/tagexporter3.jar");
    Log.warn("Using path to tag exporter " + f);
    return f;
  }

  @Nullable
  public String getTargetFileName() {
    return myTargetFile.getPath();
  }
  
  public void start(Progress progress) throws Exception {
    URLClassLoader jarLoader = new URLClassLoader(new URL[] {myTagExporter3Jar.toURI().toURL()}, getClass().getClassLoader());
    Class<?> tagExporterClass = jarLoader.loadClass("com.almworks.tools.tagexporter.TagExporter3");
    Method exportTagsMethod = tagExporterClass.getMethod("exportTags", File.class, File.class, Progress.class, Procedure2.class);

    File workspaceDir = Setup.getWorkspaceDir();
    myHadErrors = false;
    exportTagsMethod.invoke(null, workspaceDir, myTargetFile, progress, new Procedure2<String, String>() {
      @Override
      public void invoke(String key, String message) {
        Log.warn(key + ": " + message);
        myHadErrors = true;
      }
    });
  }
  
  public void runSafe() {
    try {
      final Progress progress = new Progress();
      ProgressUtil.setupLoggingProgress(Lifespan.FOREVER, progress, Level.FINE);
      start(progress);
      ProgressUtil.waitForProgress(Lifespan.FOREVER, progress);
    } catch (InterruptedException e) {
      throw new RuntimeInterruptedException(e);
    } catch (Exception e) {
      Log.warn("Failed to export tags", e);
    }
  }
}
