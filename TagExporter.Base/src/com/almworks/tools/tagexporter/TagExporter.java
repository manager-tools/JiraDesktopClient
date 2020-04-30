package com.almworks.tools.tagexporter;

import com.almworks.util.collections.Functional;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.io.IOUtils;
import com.almworks.util.progress.Progress;
import com.almworks.util.tags.TagFileStorage;
import org.almworks.util.Log;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import static com.almworks.tools.tagexporter.TagExporterEnv.*;

public class TagExporter {
  public static TagExporter exportTags(TagExporterEnv env, File workArea, @Nullable File outFile, Progress progress, Procedure2<String, String> reportError) {
    return exportTags(env, workArea, outFile, progress, reportError, ThreadGate.STRAIGHT);
  }

  public static TagExporter exportTags(TagExporterEnv env, File workArea, File outFile, Progress progress, Procedure2<String, String> reportError, ThreadGate gate) {
    TagExporter me = new TagExporter(env, new WorkspaceStructure(workArea), outFile, progress, reportError, gate);
    me.start();
    return me;
  }

  private final TagExporterEnv myEnv;
  private final WorkspaceStructure myWorkArea;
  @Nullable
  private final File myOutFile;
  private final Progress myProgress;
  private final Procedure2<String, String> myErrorNotifier;
  private final ThreadGate myErrorNotifierGate;

  private final Object myLock = new Object();
  private boolean myCancelled = false;
  private List<TagInfo> myTags;
  private TagInfoConvertor myTagInfoConvertor;

  public TagExporter(TagExporterEnv env, WorkspaceStructure workArea, File outFile, Progress progress, final Procedure2<String, String> errorNotifier, final ThreadGate errorNotifierGate) {
    myEnv = env;
    myWorkArea = workArea;
    myOutFile = outFile;
    myProgress = progress;
    myErrorNotifier = errorNotifier;
    myErrorNotifierGate = errorNotifierGate;
  }

  public void start() {
    ThreadGate.LONG.execute(new Runnable() { public void run() {
      try {
        List<TagInfo> tags = myEnv.readTags(myWorkArea, myProgress.createDelegate(0.6));
        synchronized (myLock) {
          myTags = tags;
          myLock.notifyAll();
        }
      } catch (IOException ex) {
        notifyError(WORKSPACE_KEY, ex.getMessage() + " \u2014 is the application running?", ex);
      } catch (Exception ex) {
        notifyError(GLOBAL_KEY, ex.getMessage(), ex);
      }
    }});
    ThreadGate.LONG.execute(new Runnable() { public void run() {
      try {
        Progress configReadProgress = myProgress.createDelegate(0.3, "config.xml", "Reading workspace configuration file");
        Map<String, String> connectionIdUrl = ConnectionUrlFetcher.fetchConnectionUrls(myWorkArea);
        configReadProgress.setDone();
        synchronized (myLock) {
          myTagInfoConvertor = new TagInfoConvertor(connectionIdUrl);
          myLock.notifyAll();
        }
      } catch (Exception ex) {
        notifyError(WORKSPACE_KEY, ex.getMessage(), ex);
      }
    }});
    ThreadGate.LONG.execute(new Runnable() { public void run() {
      myProgress.setActivity(myOutFile != null ? "Writing to file" : null);
      List<TagInfo> tags;
      TagInfoConvertor tagInfoConvertor;
      PrintStream ps = null;
      try {
        synchronized (myLock) {
          while (!myCancelled && (myTags == null || myTagInfoConvertor == null)) {
            myLock.wait();
          }
          if (myCancelled) return;
          tags = myTags;
          tagInfoConvertor = myTagInfoConvertor;
        }

        ps = myOutFile == null ? System.out : new PrintStream(myOutFile, "Unicode");
        TagFileStorage.write(Functional.convertList(tags, tagInfoConvertor), ps, myWorkArea.getRootDir().getPath());
        myProgress.setDone();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        notifyError(OUT_FILE_KEY, ex.getMessage(), ex);
      } finally {
        IOUtils.closeStreamIgnoreExceptions(ps);
        myProgress.setDone();
      }
    }});
  }
  
  private void notifyError(final String key, final String message, Exception ex) {
    myErrorNotifierGate.execute(new Runnable() {
      @Override
      public void run() {
        myErrorNotifier.invoke(key, message);
      }
    });
    Log.warn("TE: " + message, ex);
    cancel();
  }

  public void cancel() {
    synchronized (myLock) {
      myCancelled = true;
    }
    myProgress.setDone();
  }

  public Progress getProgress() {
    return myProgress;
  }

  @Nullable
  public File getOutputFile() {
    return myOutFile;
  }

  public boolean isCancelled() {
    synchronized (myLock) {
      return myCancelled;
    }
  }
}
