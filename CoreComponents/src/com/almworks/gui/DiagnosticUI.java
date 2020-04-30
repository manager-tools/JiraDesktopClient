package com.almworks.gui;

import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.StatusBarMessage;
import com.almworks.api.gui.StatusBarMessages;
import com.almworks.platform.DiagnosticRecorder;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileActions;
import com.almworks.util.ui.actions.*;
import org.picocontainer.Startable;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class DiagnosticUI implements Startable, ActionListener {
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  private final DiagnosticRecorder myRecorder;
  private final ActionRegistry myRegistry;
  private final StatusBarMessage myMessage;

  public DiagnosticUI(DiagnosticRecorder recorder, ActionRegistry registry, MainWindowManager mwm) {
    myRecorder = recorder;
    myRegistry = registry;
    myMessage = StatusBarMessages.createDiagnosticMode(
      mwm.getStatusBar(), this, "Recording Diagnostics", "Click here to stop recording");
  }

  @Override
  public void start() {
    myRegistry.registerAction(MainMenu.Tools.RECORD_DIAGNOSTICS, new StartStopAction());
  }

  @Override
  public void stop() {}

  private void startRecording() {
    ThreadGate.LONG.execute(new Runnable() {
      @Override
      public void run() {
        myRecorder.startSession();
        ThreadGate.AWT.execute(new Runnable() {
          @Override
          public void run() {
            myMessage.setVisible(true);
            myModifiable.fireChanged();
          }
        });
      }
    });
  }

  private void stopRecording(final Component owner) {
    ThreadGate.LONG.execute(new Runnable() {
      @Override
      public void run() {
        final File dir = myRecorder.stopSession();
        if(dir != null) {
          ThreadGate.AWT.execute(new Runnable() {
            @Override
            public void run() {
              FileActions.highlightFolderOrShowContents(dir, owner);
              myMessage.setVisible(false);
              myModifiable.fireChanged();
            }
          });
        }
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    stopRecording(myMessage.getComponent());
  }

  private class StartStopAction extends SimpleAction {
    public StartStopAction() {
      super("&Diagnostics Mode");
    }

    @Override
    protected void customUpdate(UpdateContext context) throws CantPerformException {
      context.putPresentationProperty(PresentationKey.TOGGLED_ON, myRecorder.isRecording());
      context.updateOnChange(myModifiable);
    }

    @Override
    protected void doPerform(ActionContext context) throws CantPerformException {
      if(myRecorder.isRecording()) {
        stopRecording(context.getComponent());
      } else {
        startRecording();
      }
    }
  }
}
