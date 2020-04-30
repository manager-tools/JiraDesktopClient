package com.almworks.tools.tagexporter;

import com.almworks.util.DecentFormatter;
import com.almworks.util.NoObfuscation;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.components.plaf.LAFUtil;
import com.almworks.util.exec.LongEventQueue;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.progress.Progress;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.StringUtil;
import org.almworks.util.Util;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.logging.Level;

import static com.almworks.tools.tagexporter.TagExporterEnv.WORKSPACE_KEY;

public class TagExporterLauncher implements NoObfuscation{
  private final TagExporterEnv myEnv;

  public TagExporterLauncher(TagExporterEnv env) {
    myEnv = env;
  }
  
  public static void launch(TagExporterEnv env, String[] args) {
    new TagExporterLauncher(env).launch(args);
  }

  public void launch(String[] args) {
    setupLogging();
    if (args.length == 1 && Collections15.arrayList("?", "/?", "-h", "-help", "help").contains(args[0])) {
      System.err.println(myEnv.getFullName());
      System.err.println("Usage: tagexporter [-v] <workspace folder path>");
      System.err.println("-v: verbose mode");
      System.exit(1);
    }
    if (args.length == 0) runGui();
    else {
      boolean verbose = "-v".equals(args[0]);
      if (verbose && args.length == 1) {
        System.err.println("Please specify workspace folder");
        System.exit(1);
      }
      String workspace = verbose ? args[1] : args[0];
      if (args.length > (verbose ? 2 : 1)) {
        // count the rest of the arguments as a workspace path with spaces
        workspace = StringUtil.implode(Arrays.asList(verbose ? Arrays.copyOfRange(args, 1, args.length) : args), " ");
      }
      runCli(workspace, verbose);
    }
  }

  private static void setupLogging() {
    DecentFormatter.install("");
  }

  private void runGui() {
    Log.getApplicationLogger().setLevel(Level.ALL);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        LongEventQueue.installToContext();
        LAFUtil.initializeLookAndFeel();

        TagExporterForm.showWindow(myEnv);
      }
    });
  }

  private void runCli(String workspace, boolean verbose) {
    String error = myEnv.verifyWorkspace(new File(workspace));
    if (error != null) {
      System.err.println(error);
      System.exit(2);
    }
    Log.getApplicationLogger().setLevel(verbose ? Level.FINEST : Level.SEVERE);
    final Progress p = new Progress();
    if (verbose) {
      p.getModifiable().addChangeListener(Lifespan.FOREVER, ThreadGate.STRAIGHT, new ChangeListener() {
        @Override
        public void onChange() {
          String activity = String.valueOf(Util.NN(p.getActivity(), ""));
          if (!activity.isEmpty())
            System.err.println(activity);
        }
      });
    }
    TagExporter.exportTags(myEnv, new File(workspace), null, p, new Procedure2<String, String>() {
      @Override
      public void invoke(String key, String error) {
        String preamble = key == WORKSPACE_KEY ? "Workspace problem: " : "Error: ";
        System.err.println(preamble + error);
        System.exit(3);
      }
    });
  }
}
