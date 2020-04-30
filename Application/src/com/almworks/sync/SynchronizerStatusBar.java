package com.almworks.sync;

import com.almworks.api.engine.SyncProblem;
import com.almworks.api.engine.Synchronizer;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.StatusBar;
import com.almworks.api.gui.StatusBarComponent;
import com.almworks.api.gui.StatusBarLink;
import com.almworks.api.sync.SynchronizationWindow;
import com.almworks.util.L;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.Link;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ScalarModel;
import com.almworks.util.model.ScalarModelEvent;
import com.almworks.util.model.SetHolderUtils;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

class SynchronizerStatusBar implements StatusBarComponent {
  private final DetachComposite myDetach = new DetachComposite();
  private final Link myLabel;
  private final StatusBar myStatusBar;
  private final SynchronizationWindow mySyncWindow;
  private final Synchronizer mySynchronizer;

  public SynchronizerStatusBar(MainWindowManager windowManager, Synchronizer synchronizer,
    SynchronizationWindow syncWindow) {

    mySynchronizer = synchronizer;
    mySyncWindow = syncWindow;
    myStatusBar = windowManager.getStatusBar();
    myLabel = new StatusBarLink();
    myLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
  }

  public int getReservedWidth() {
/*
    Font font = myLabel.getFont();
    if (font != null) {
      FontMetrics fm = myLabel.getFontMetrics(font);
      if (fm != null) {
        int width = fm.stringWidth("Synchronized <24 min ago");
        width += Icons.STATUSBAR_SYNCHRONIZATION_IN_PROGRESS.getIconWidth();
        width += myLabel.getIconTextGap();
        Insets insets = myLabel.getInsets();
        width += insets.left + insets.right;
        width += 4;
        return width;
      }
    }
*/
    return 0;
  }

  public void attach() {
    listenSynchronizer();
    listenButton();
    startTimer();
  }

  public JComponent getComponent() {
    return myLabel;
  }

  public void dispose() {
    myDetach.detach();
  }

  public void start() {
    ThreadGate.AWT.execute(new Runnable() {
      public void run() {
        addStatusElement();
      }
    });
  }

  private void addStatusElement() {
    myStatusBar.addComponent(StatusBar.Section.SYNCHRONIZATION_STATUS, this);
  }

  private String getProblemDescriptions() {
    Collection<SyncProblem> problemsCollection = mySynchronizer.getProblems().copyCurrent();
    if (problemsCollection.size() == 0)
      return "";
    Set<String> descs = Collections15.hashSet();
    for (Iterator<SyncProblem> ii = problemsCollection.iterator(); ii.hasNext();) {
      SyncProblem problem = ii.next();
      if (problem.isSerious())
        descs.add(problem.getShortDescription());
    }
    int i = 0;
    StringBuffer result = new StringBuffer("<br><b>");
    Iterator<String> ii;
    for (ii = descs.iterator(); ii.hasNext();) {
      String name = ii.next();
      if (i > 0)
        result.append("<br>");
      result.append(name);
      i++;
      if (i > 3)
        break;
    }
    result.append("</b>");
    if (ii.hasNext())
      result.append("<br><i>and others</i>");
    return result.toString();
  }

  private void listenButton() {
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mySyncWindow.show();
      }
    };
    myLabel.addActionListener(listener);
    myDetach.add(new Detach() {
      protected void doDetach() {
        myLabel.removeActionListener(listener);
      }
    });
  }

  private void listenSynchronizer() {
    mySynchronizer.getSyncState().getEventSource().addAWTListener(myDetach, new ScalarModel.Adapter<Synchronizer.State>() {
      public void onScalarChanged(ScalarModelEvent<Synchronizer.State> event) {
        update();
      }
    });
    mySynchronizer.getProblems().addInitListener(myDetach, ThreadGate.AWT, SetHolderUtils.fromChangeListener(new ChangeListener() {
      public void onChange() {
        update();
      }
    }));
  }

  private void startTimer() {
    final Timer timer = new Timer(15000, new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        update();
      }
    });
    timer.setRepeats(true);
    timer.start();
    myDetach.add(new Detach() {
      protected void doDetach() {
        timer.stop();
      }
    });
  }

  private void update() {
    Synchronizer.State state = mySynchronizer.getSyncState().getValue();
    Date date = mySynchronizer.getLastSyncTime().getValue();
    String problems = getProblemDescriptions();
    String briefDate = "on " + DateUtil.toLocalDateTime(date);

    String text;
    Icon icon;
    String tooltipText;

    if (state == Synchronizer.State.FAILED) {
      icon = Icons.STATUSBAR_SYNCHRONIZATION_PROBLEMS;
      text = "Synchronization had problems";
      tooltipText = "<html>Last synchronization had problems." + problems + "<br>Synchronization was started " +
        briefDate;
    } else if (state == Synchronizer.State.SUSPENDED) {
      icon = Icons.STATUSBAR_SYNCHRONIZATION_IN_PROGRESS;
      text = "Suspended";
      tooltipText = "<html>Synchronization is suspended." + problems + "<br>Synchronization was started " + briefDate;
    } else if (state == Synchronizer.State.WORKING) {
      icon = Icons.STATUSBAR_SYNCHRONIZATION_IN_PROGRESS.getIcon();
      text = "Synchronizing";
      tooltipText = "<html>Synchronization is in progress." + problems + "<br>Synchronization started " + briefDate;
    } else if (state == Synchronizer.State.IDLE) {
      if (date.getTime() > 86400000) {
        icon = null;
        text = "Synchronized " + DateUtil.toFriendlyDateTime(date, DateUtil.US_MONTH_DAY, true);
        tooltipText = "<html>Last update occurred " + briefDate + "." + problems;
      } else {
        icon = Icons.STATUSBAR_SYNCHRONIZATION_UNKNOWN;
        text = "Unsynchronized";
        tooltipText = "<html>Synchronization has never been started.";
      }
    } else {
      text = null;
      icon = null;
      tooltipText = null;
    }
    myLabel.setIcon(icon);
    myLabel.setText(L.tooltip(text));
    myLabel.setToolTipText(L.html(tooltipText));
  }
}
