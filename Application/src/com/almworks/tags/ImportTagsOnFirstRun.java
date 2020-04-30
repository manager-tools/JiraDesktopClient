package com.almworks.tags;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.application.tree.RootNode;
import com.almworks.api.engine.Engine;
import com.almworks.api.engine.EngineUtils;
import com.almworks.api.install.Setup;
import com.almworks.api.misc.WorkArea;
import com.almworks.items.api.Database;
import com.almworks.util.Env;
import com.almworks.util.Terms;
import com.almworks.util.commons.Function;
import com.almworks.util.commons.Procedure;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressUtil;
import com.almworks.util.properties.Role;
import com.almworks.util.tags.TagFileStorage;
import org.almworks.util.ExceptionUtil;
import org.almworks.util.Log;
import org.almworks.util.RuntimeInterruptedException;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

import static com.almworks.util.collections.Containers.constF;

public class ImportTagsOnFirstRun {
  public static final Role<ImportTagsOnFirstRun> ROLE = Role.role("ImportTagsOnFirstRun");
  public static final String PATH_TO_TAGEXPORTER_JAR = "path.to.TagExporter.jar";

  private final Engine myEngine;
  private final Database myDb;
  private final WorkArea myWorkArea;
  private final ExplorerComponent myExplorer;

  private volatile boolean myHadErrors;
  private File myTagsFile;

  public ImportTagsOnFirstRun(Engine engine, Database db, WorkArea workArea, ExplorerComponent explorer) {
    myEngine = engine;
    myDb = db;
    myWorkArea = workArea;
    myExplorer = explorer;
  }

  public void run(final Procedure2<Boolean, String> onFinish, final Progress progress) {
    ThreadGate.LONG(this).execute(new Runnable() {
      @Override
      public void run() {
        Lifecycle life = new Lifecycle();
        Procedure2<Boolean, String> safeOnFinish = safeProcedure(onFinish);
        try {
          myHadErrors = false;
          runSafe(life.lifespan(), progress, safeOnFinish);
        } catch (Exception ex) {
          processError(ex);
        } catch (ExceptionInInitializerError ex) {
          // May originate from invokation of TagExporter's methods
          processError(ex);
        } finally {
          life.dispose();
          if (myHadErrors)
            safeOnFinish.invoke(false, null);
        }
      }
    });
  }

  private void processError(Throwable t) {
    Log.error("Failed to import tags on the first run: " + t);
    myHadErrors = true;
    if (t instanceof InterruptedException || t instanceof RuntimeInterruptedException)
      ExceptionUtil.rethrow(t);
  }

  private static <T1, T2> Procedure2<T1, T2> safeProcedure(final Procedure2<T1, T2> unsafe) {
    return new Procedure2<T1, T2>() {
      @Override
      public void invoke(T1 t1, T2 t2) {
        try {
          unsafe.invoke(t1, t2);
        } catch (Exception t) {
          Log.warn(t);
        }
      }
    };
  }

  private void runSafe(Lifespan outerLife, final Progress progress, final Procedure2<Boolean, String> onFinish) throws Exception {
    myTagsFile = null;
    boolean hasOldDb = exportTagsFromOldDb(outerLife, progress.createDelegate(0.3, "Export"));
    if (!hasOldDb) {
      onFinish.invoke(true, Local.parse("No old " + Terms.ref_Deskzilla + " database has been found."));
      return;
    }
    if (myHadErrors) return;
    if (myTagsFile == null || !myTagsFile.isFile()) throw new Exception("Export tags from the old database failed");
    EngineUtils.runWhenConnectionsAreStable(Lifespan.FOREVER, ThreadGate.LONG(this), new Runnable() {
      @Override
      public void run() {
        importTagsIntoNewDbSafe(progress.createDelegate(0.7, "Import"), onFinish);
      }
    });
  }

  /**
   * @return false if there is no old db in the workspace. true if there is and tags were successfully extracted
   * */
  private boolean exportTagsFromOldDb(Lifespan lifespan, Progress progress) throws Exception {
    File tagExporterJar = getTagExporterJarFile();
    if (!tagExporterJar.isFile()) throw new Exception(tagExporterJar + " is not a file");
    URLClassLoader jarLoader = new URLClassLoader(new URL[] {tagExporterJar.toURI().toURL()}, getClass().getClassLoader());
    Class<?> tagExporterClass = jarLoader.loadClass("com.almworks.tools.tagexporter.TagExporter2x");
    Method exportTagsMethod = tagExporterClass.getMethod("exportTags", File.class, File.class, Progress.class, Procedure2.class);
    Method checkIsOldDb = tagExporterClass.getMethod("is2xWorkspace", File.class);

    File workspaceDir = Setup.getWorkspaceDir();
    Object isOldDb = checkIsOldDb.invoke(null, workspaceDir);
    if (!Boolean.TRUE.equals(isOldDb)) return false;

    myTagsFile = new File(workspaceDir, TagFileStorage.DEFAULT_FILE_NAME);
    ProgressUtil.setupLoggingProgress(lifespan, progress, Level.FINE);
    exportTagsMethod.invoke(null, workspaceDir, myTagsFile, progress, reportError);
    return true;
  }

  private File getTagExporterJarFile() {
    String overridingProp = Env.getString(PATH_TO_TAGEXPORTER_JAR);
    if (overridingProp != null) {
      File f = new File(overridingProp);
      if (f.isFile()) {
        Log.warn("Using path to TagExporter.jar from properties: '" + overridingProp + '\'');
        return f;
      } else {
        Log.warn("Path to TagExporter.jar from properties is invalid, using standard");
      }
    }
    File f = new File(myWorkArea.getInstallationEtcDir(), "tagexporter/tagexporter.jar");
    Log.warn("Using path to tag exporter " + f);
    return f;
  }

  private final Procedure2<String, String> reportError = new Procedure2<String, String>() {
    @Override
    public void invoke(String key, String message) {
      Log.error(key + " problem: " + message);
      myHadErrors = true;
    }
  };


  private void importTagsIntoNewDbSafe(Progress progress, Procedure2<Boolean, String> onFinish) {
    Lifecycle innerLife = new Lifecycle();
    try {
      importTagsIntoNewDb(innerLife.lifespan(), progress, onFinish);
    } catch (Exception ex) {
      processError(ex);
    } finally {
      innerLife.dispose();
      if (myHadErrors)
        onFinish.invoke(false, null);
    }
  }

  private void importTagsIntoNewDb(final Lifespan life, final Progress progress, Procedure2<Boolean, String> onFinish)
    throws InterruptedException
  {
    ProgressUtil.setupLoggingProgress(life, progress, Level.FINE);
    ThreadGate.AWT.execute(new Runnable() {
      @Override
      public void run() {
        RootNode rootNode = myExplorer.getRootNode();
        if (rootNode == null) {
          Log.error("Cannot import tags from old workspace: no root node");
          myHadErrors = true;
          progress.setDone();
          return;
        }
        final Procedure<String> onFatalError = new Procedure<String>() {
          @Override
          public void invoke(String arg) {
            Log.error("Cannot import tags from old workspace: " + arg);
            myHadErrors = true;
          }
        };

        Function<String, Boolean> confirmAllMerges = constF(true).<String>f();
        TagsImporter tagsImporter = TagsImporter.create(myTagsFile, progress, rootNode, myEngine, myDb, confirmAllMerges, onFatalError);
        tagsImporter.run();
      }
    });
    ProgressUtil.waitForProgress(life, progress);

    Log.debug("Tags have been successfully imported from the old workspace");
    onFinish.invoke(true, null);
  }
}
