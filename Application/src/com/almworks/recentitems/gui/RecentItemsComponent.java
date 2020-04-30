package com.almworks.recentitems.gui;

import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.StatusBar;
import com.almworks.api.gui.StatusBarLink;
import com.almworks.recentitems.RecordType;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.util.Terms;
import com.almworks.util.advmodel.AListModel;
import com.almworks.util.collections.Modifiable;
import com.almworks.util.components.Link;
import com.almworks.util.datetime.DateUtil;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.threads.Bottleneck;
import com.almworks.util.ui.ColorUtil;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIComponentWrapper2;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.detach.DetachComposite;
import org.picocontainer.Startable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RecentItemsComponent implements Startable, Runnable, ActionListener {
  private static final int DIMMER_DELAY = 30000;
  private static final int REFRESH_DELAY = 60000;

  private final Modifiable myLoaderModifiable;
  private final AListModel<LoadedRecord> myLoadedModel;

  private final StatusBar myStatusBar;
  private final TimeTrackingCustomizer myCustomizer;

  private final DetachComposite myLife = new DetachComposite();
  private final Bottleneck myRefreshBottleneck = new Bottleneck(500, ThreadGate.AWT, this);
  private final Timer myRefresher = new Timer(REFRESH_DELAY, null);

  private final Link myLink = new StatusBarLink();
  private final Timer myDimmer = new Timer(DIMMER_DELAY, null);
  private final Color myBrightFg = myLink.getForeground();
  private final Color myDimmedFg = ColorUtil.between(myBrightFg, UIManager.getColor("Panel.background"), 0.75f);
  private final UIComponentWrapper myWrapper = createWrapper();

  public RecentItemsComponent(
    RecentItemsLoader loader, MainWindowManager windowManager, TimeTrackingCustomizer customizer)
  {
    myLoaderModifiable = loader.getModifiable();
    myLoadedModel = loader.getLoadedModel();
    myStatusBar = windowManager.getStatusBar();
    myCustomizer = customizer;
    prepareLink();
    prepareDimmer();
    prepareRefresher();
  }

  private void prepareLink() {
    myLink.setBorder(new EmptyBorder(0, 5, 0, 5));
    myLink.setToolTipText(String.format(
      "<html>Click here or press %s to show most recently uploaded %s",
      Shortcuts.menu("/"), Local.text(Terms.key_artifacts))); // todo get rid of hardcoded shortcut
    myLink.addActionListener(this);
  }

  private void prepareDimmer() {
    myDimmer.setRepeats(false);
    myDimmer.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myLink.setForeground(myDimmedFg);
      }
    });
  }

  private void prepareRefresher() {
    myRefresher.setInitialDelay(0);
    myRefresher.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        myRefreshBottleneck.request();
      }
    });
  }

  private UIComponentWrapper createWrapper() {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(myLink, BorderLayout.WEST);
    return new UIComponentWrapper2.Simple(panel);
  }

  @Override
  public void start() {
    myLoaderModifiable.addAWTChangeListener(myLife, myRefreshBottleneck);
    myRefresher.start();
  }

  @Override
  public void stop() {
    myRefresher.stop();
    myLife.detach();
    myStatusBar.showContextComponent(null);
  }

  @Override
  public void run() {
    showLastRecord();
  }

  private void showLastRecord() {
    if(myLoadedModel.getSize() > 0) {
      final LoadedRecord lastRecord = myLoadedModel.getAt(0);
      showRecord(lastRecord);
      myStatusBar.showContextComponent(myWrapper);
    } else {
      myStatusBar.showContextComponent(null);
    }
  }

  private void showRecord(LoadedRecord lastRecord) {
    showText(formatRecord(lastRecord), isOld(lastRecord));
  }

  private String formatRecord(LoadedRecord record) {
    return DateUtil.toLocalDateOrTime(record.myTimestamp) + ": "
      + formatAction(record) + " " + myCustomizer.getItemKey(record.myItem);
  }

  private String formatAction(LoadedRecord record) {
    return record.myType == RecordType.NEW_UPLOAD ? "Created" : "Updated";
  }

  private boolean isOld(LoadedRecord lastRecord) {
    return System.currentTimeMillis() - lastRecord.myTimestamp.getTime() > DIMMER_DELAY;
  }

  private void showText(String text, boolean old) {
    myLink.setText(text);
    if(old) {
      myDimmer.stop();
      myLink.setForeground(myDimmedFg);
    } else {
      myLink.setForeground(myBrightFg);
      myDimmer.restart();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    linkClicked();
  }

  private void linkClicked() {
    RecentItemsDialog.showDialog(myLink);
  }
}
