package com.almworks.export;

import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AActionButton;
import com.almworks.util.progress.ProgressSource;
import com.almworks.util.text.NameMnemonic;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionBridge;
import com.almworks.util.ui.actions.AnAction;
import org.almworks.util.detach.Lifecycle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

import static com.almworks.util.ui.SingleChildLayout.CONTAINER;
import static com.almworks.util.ui.SingleChildLayout.PREFERRED;

public class ExportInProgressForm {
  private final DialogBuilder myDialogBuilder;

  private static final String PROGRESS = "progress";
  private static final String DONE = "done";
  private static final String ERROR = "error";

  private final JPanel myWholePanel = new JPanel(new BorderLayout(5, 5));
  private final JLabel myProgressLabel = new JLabel();
  private final JProgressBar myProgressBar = new JProgressBar(0, 100) {
    private int myMinSize = -1;
    public Dimension getPreferredSize() {
      Dimension size = null;
      try {
        size = super.getPreferredSize();
      } catch (NullPointerException e) {
        // see UIUtil.getProgressBarPreferredSize
      }
      if (size == null)
        size = new Dimension(100, 24);
      if (myMinSize < 0)
        myMinSize = UIUtil.getColumnWidth(this) * 20;
      if (size.getWidth() < myMinSize)
        size = new Dimension(myMinSize, size.height);
      return size;
    }
  };
  private final JPanel myDoneActionsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 1));
  private final JPanel myProgressOrDone = new JPanel(new CardLayout());
  private final JScrollPane myErrorScrollPane = new JScrollPane() {
    public Dimension getPreferredSize() {
      Dimension size = myErrorArea.getPreferredSize();
      Insets insets = getInsets();
      return new Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom);
    }
  };
  private final JTextArea myErrorArea = new JTextArea(3, 40);

  private final Lifecycle myProgressLife = new Lifecycle();
  private final boolean mySilent;
  private AnAction myDefaultDoneAction = null;
  private boolean myDefaultDoneActionPerformed = false;

  public ExportInProgressForm(DialogManager dialogManager, boolean silent) {
    mySilent = silent;
    myDialogBuilder = dialogManager.createBuilder("exportInProgress");
    setupDialog();
    myDialogBuilder.setTitle("Export");
    myDialogBuilder.setModal(false);
    myDialogBuilder.setContent(myWholePanel);
    myDialogBuilder.setIgnoreStoredSize(true);
  }

  private void setupDialog() {
    myWholePanel.add(myProgressLabel, BorderLayout.NORTH);
    myWholePanel.add(myProgressOrDone, BorderLayout.CENTER);

    JPanel progress = SingleChildLayout.envelop(myProgressBar, CONTAINER, PREFERRED, CONTAINER, PREFERRED, 0.5F, 0F);
    myProgressOrDone.add(progress, PROGRESS);
    myProgressOrDone.add(myDoneActionsPanel, DONE);

    myErrorScrollPane.setViewportView(myErrorArea);
    myProgressOrDone.add(myErrorScrollPane, ERROR);
    myErrorArea.setEditable(false);
    myErrorArea.setLineWrap(true);
    myErrorArea.setWrapStyleWord(true);
  }

  public void setProgress(final ProgressSource progress) {
    myProgressLife.cycle();
    progress.getModifiable().addAWTChangeListener(myProgressLife.lifespan(), new ChangeListener() {
      public void onChange() {
        if (progress.isDone()) {
          myProgressLife.cycle();
          List<String> errors = progress.getErrors(null);
          if (errors == null || errors.size() == 0) {
            ((CardLayout) myProgressOrDone.getLayout()).show(myProgressOrDone, DONE);
            myProgressLabel.setText("Export complete.");
            if (mySilent) {
              WindowController controller = myDialogBuilder.getWindowContainer().getActor(WindowController.ROLE);
              if (controller != null) {
                controller.close();
              }
              if (myDefaultDoneAction != null && !myDefaultDoneActionPerformed) {
                myDefaultDoneActionPerformed = true;
                ActionBridge bridge = new ActionBridge(myDefaultDoneAction, myDoneActionsPanel);
                bridge.performIfEnabled();
              }
            }
          } else {
            ((CardLayout) myProgressOrDone.getLayout()).show(myProgressOrDone, ERROR);
            myProgressLabel.setText("Export failed.");
            StringBuffer buf = new StringBuffer();
            for (String error : errors) {
              if (buf.length() > 0)
                buf.append('\n');
              buf.append(error);
            }
            myErrorArea.setText(buf.toString());
          }
        } else {
          int percent = (int)Math.round(progress.getProgress() * 100F);
          if (percent != myProgressBar.getValue()) {
            myProgressBar.setValue(percent);
            myProgressLabel.setText("Export in progress\u2026 " + percent + "%");
          }
        }
      }
    });
  }
//  public void setProgress(final ProgressSource<String> progress) {
//    myProgressLife.cycle();
//    progress.addAWTChangeListener(myProgressLife.lifespan(), new ChangeListener() {
//      public void onChange() {
//        Progress<String> p = progress.getProgress();
//        if (p.isDone()) {
//          myProgressLife.cycle();
//          List<ProgressError> errors = p.getErrors();
//          if (errors.size() == 0) {
//            ((CardLayout) myProgressOrDone.getLayout()).show(myProgressOrDone, DONE);
//            myProgressLabel.setText("Export complete.");
//            if (mySilent) {
//              WindowController controller = myDialogBuilder.getWindowContainer().getActor(WindowController.ROLE);
//              if (controller != null) {
//                controller.close();
//              }
//              if (myDefaultDoneAction != null && !myDefaultDoneActionPerformed) {
//                myDefaultDoneActionPerformed = true;
//                ActionBridge bridge = new ActionBridge(myDefaultDoneAction, myDoneActionsPanel);
//                bridge.performIfEnabled();
//              }
//            }
//          } else {
//            ((CardLayout) myProgressOrDone.getLayout()).show(myProgressOrDone, ERROR);
//            myProgressLabel.setText("Export failed.");
//            StringBuffer buf = new StringBuffer();
//            for (ProgressError error : errors) {
//              if (buf.length() > 0)
//                buf.append('\n');
//              buf.append(error.getDisplayableMessage());
//            }
//            myErrorArea.setText(buf.toString());
//          }
//        } else {
//          int percent = Math.round(p.getProgress() * 100F);
//          if (percent != myProgressBar.getValue()) {
//            myProgressBar.setValue(percent);
//            myProgressLabel.setText("Export in progress... " + percent + "%");
//          }
//        }
//      }
//    });
//  }

  public void setDoneActions(List<AnAction> actions) {
    myDoneActionsPanel.removeAll();
    for (AnAction action : actions) {
      myDoneActionsPanel.add(new AActionButton(action));
      if (myDefaultDoneAction == null) {
        // hack use first action as a default
        myDefaultDoneAction = action;
      }
    }
  }

  public void setCancelAction(AnAction action) {
    myDialogBuilder.setCancelAction(action);
  }

  public void show() {
    myDialogBuilder.showWindow();
  }

  public void setTargetFile(File file) {
    if (file == null)
      return;
    JPanel filePanel = new JPanel(new BorderLayout(5, 0));
    JLabel label = new JLabel();
    JTextField field = new JTextField();
    NameMnemonic.parseString("&File:").setToLabel(label);
    label.setLabelFor(field);
    UIUtil.setFieldText(field, file.getAbsolutePath());
    field.setEditable(false);
    filePanel.add(label, BorderLayout.WEST);
    filePanel.add(field, BorderLayout.CENTER);
    filePanel.setBorder(new EmptyBorder(11, 0, 0, 0));
    myWholePanel.add(filePanel, BorderLayout.SOUTH);
  }
}
