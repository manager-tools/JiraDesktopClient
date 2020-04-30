package com.almworks.tags;

import com.almworks.api.container.ComponentContainer;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.StatusBar;
import com.almworks.api.gui.StatusBarMessage;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure2;
import com.almworks.util.i18n.Local;
import com.almworks.util.images.Icons;
import com.almworks.util.progress.Progress;
import com.almworks.util.progress.ProgressActivity;
import org.almworks.util.Const;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ImportTagsOnFirstRunUi implements Procedure2<Boolean, String>, ChangeListener {
  private static final String MESSAGE_STEM =
    Local.parse("Migrating tags from the old " + Terms.ref_Deskzilla + " database");
  private StatusBarMessage myMessage;
  private final Progress myProgress = new Progress();
  private final DetachComposite myLife = new DetachComposite();

  /**
   * Creates some animation that indicates that tags are being imported.
   * @return Procedure: first parameter is "success", second -- message (nullable)
   */
  public static ImportTagsOnFirstRunUi setupUi(ComponentContainer container) {
    MainWindowManager mainWindowManager = container.getActor(MainWindowManager.ROLE);
    ImportTagsOnFirstRunUi ui = new ImportTagsOnFirstRunUi();
    if (mainWindowManager != null) {
      StatusBar statusBar = mainWindowManager.getStatusBar();
      String progressMsg = MESSAGE_STEM + '\u2026';
      ui.myMessage = new StatusBarMessage(statusBar, null, progressMsg, Icons.TAG_DEFAULT, null);
      ui.myMessage.setVisible(true);
      ui.myProgress.getModifiable().addAWTChangeListener(ui.myLife, ui);
    }
    return ui;
  }

  @Override
  public void invoke(Boolean aBoolean, String s) {
    if (myMessage == null) return;
    
    if (Boolean.TRUE.equals(aBoolean)) {
      myMessage.setText(MESSAGE_STEM + " done" + (s != null ? " (" + s + ')' : '.'));
    } else if (Boolean.FALSE.equals(aBoolean)) {
      myMessage.setText(MESSAGE_STEM + " failed.");
      if (s != null) myMessage.setTooltip(s);
    } else {
      Log.warn("Wrong param", new Exception());
      return;
    }
    final Timer timer = new Timer((int)(15 * Const.SECOND), null);
    timer.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myMessage.setVisible(false);
        timer.stop();
      }
    });
    timer.start();
  }

  public Progress getProgress() {
    return myProgress;
  }

  @Override
  public void onChange() {
    if (myMessage != null) {
      ProgressActivity activity = myProgress.getActivity();
      myMessage.setTooltip(activity != null ? activity.toString() : "");
    }
  }
}
