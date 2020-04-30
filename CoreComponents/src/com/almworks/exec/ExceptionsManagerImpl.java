package com.almworks.exec;

import com.almworks.api.exec.ExceptionsManager;
import com.almworks.api.exec.FatalError;
import com.almworks.api.install.Setup;
import com.almworks.util.DiagnosticException;
import com.almworks.util.L;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.Gateable;
import com.almworks.util.exec.SeparateEventQueueGate;
import com.almworks.util.threads.Marshaller;
import com.almworks.util.ui.errors.*;
import org.almworks.util.Log;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author : Dyoma
 */
public class ExceptionsManagerImpl implements Startable, ExceptionsManager {
  private static final KnownProblem[] KNOWN_PROBLEMS = {
    new ProblemDraggableTreeExpansionInJdk16(), new ProblemAwtDoesNotLikeDisplayChanges(),
    new ProblemMacNPEWhenDragging(), new ProblemMacPaintsHeaderOfDraggedColumnEvenIfItIsRemoved(),
    new ProblemMacTabbedPaneIndex(), new ProblemClipboardAccessThrowsExceptionInUICode(),
    new ProblemMacNonJavaException(),
    new ProblemFuckingLameSwabungaLibrary(),
    new ProblemMacComboBlinkRepaintsWhenNotDisplayable(),
    /* must come last */ new ProblemJavaLibraryException()
  };

  private final FireEventSupport<Listener> myListeners = FireEventSupport.createSynchronized(Listener.class);
  private final Marshaller<Listener> myMarshaller = Marshaller.create(Listener.class);
  private DetachComposite myDetach = null;
  public static volatile byte[] myMemoryReserve;

  public ExceptionsManagerImpl() {
  }

  public Detach addListener(Listener listener) {
    SeparateEventQueueGate gate = new SeparateEventQueueGate("exceptions").start();
    return myListeners.addStraightListener(myMarshaller.marshal(listener, gate));
  }

  public boolean isAnyExceptionOccured() {
    return Log.isExceptionOccured();
  }

  public void start() {
    assert myDetach == null;
    myDetach = new DetachComposite();

    final Handler handler = new Handler() {
      public void close() throws SecurityException {
      }

      public void flush() {
      }

      public void publish(LogRecord record) {
        final int severity = record.getLevel().intValue();
        final boolean error = severity >= Level.SEVERE.intValue();

        Throwable throwable = record.getThrown();
        boolean diag = false;
        if(throwable instanceof DiagnosticException) {
          diag = true;
          final Throwable cause = throwable.getCause();
          if(cause != null) {
            throwable = cause;
          }
        }

        if(error || diag) {
          final String type = diag ? ExceptionEvent.DIAGNOSTIC : ExceptionEvent.ERROR;
          if(!handleKnownProblem(throwable)) {
            ExceptionEvent event = new ExceptionEvent(Thread.currentThread(), throwable, type, record.getMessage());
            fireExceptionOccured(event);
          }
        }
      }
    };

    Log.getRootLogger().addHandler(handler);
    myDetach.add(new Detach() {
      protected void doDetach() {
        Log.getRootLogger().removeHandler(handler);
      }
    });

    prepareForOutOfMemory();
  }

  private void prepareForOutOfMemory() {
    myMemoryReserve = new byte[102400];
    long max = Runtime.getRuntime().maxMemory();
    if (max == Long.MAX_VALUE)
      max = -1;
    else
      max = max / 1024 / 1024;

    String message =
      "<html><body>" +
      "There is not enough memory for the application.<br><br>" +
      "" +
      "Unfortunately, the application has used up all system memory that<br>" +
      "has been allocated for it, and will need to close.<br><br>" +
      "" +
      "This may happen if you have loaded a lot of data or due to a defect<br>" +
      "in the application. To reduce probability of this happening in the future, <br>" +
      "you can increase amount of memory allocated to Java VM.<br><br>" +
      "" +
      (max > 0 ? ("Do this by adding -J-Xmx" + (max * 2) + "m option if you run " + Setup.getProductId() + ".exe,<br>" +
      "or by adding -Xmx" + (max * 2) + "m option if you run java with " + Setup.getProductId() + ".jar.<br><br>") : "")
      + ""
      + "We apologize for this incident.<br><br>" +
      "" +
      "Press OK to terminate the application.";

    final Gateable dialog = FatalError.prepareErrorDialog(L.dialog("Out of Memory"), L.content(message), false);

    Log.installOutOfMemoryHandler(new Runnable() {
      public void run() {
        // try to free some memory
        myMemoryReserve = null;
        try {
          System.gc();
        } catch (Throwable e) {
          // ignore!
        }

        FatalError.showDialogAndTerminate(dialog);
      }
    });
  }

  public void stop() {
    assert myDetach != null;
    myDetach.detach();
    myDetach = null;
  }

  private void fireExceptionOccured(ExceptionEvent event) {
    myListeners.getDispatcherSnapshot().onException(event);
  }

  public static boolean handleKnownProblem(Throwable throwable) {
    if(throwable == null) {
      return false;
    }

    final StackTraceElement[] trace = throwable.getStackTrace();
    for(final KnownProblem problem : KNOWN_PROBLEMS) {
      if(problem.handle(throwable, trace)) {
        return true;
      }
    }

    return false;
  }
}

