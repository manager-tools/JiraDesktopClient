package com.almworks.timetrack.gui;

import com.almworks.api.application.DBDataRoles;
import com.almworks.api.application.ItemWrapper;
import com.almworks.api.application.LoadedItem;
import com.almworks.api.application.MetaInfo;
import com.almworks.api.application.viewer.DefaultUIController;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.WindowController;
import com.almworks.api.misc.TimeService;
import com.almworks.explorer.loader.ItemUiModelImpl;
import com.almworks.items.api.DBOperationCancelledException;
import com.almworks.items.api.DBReader;
import com.almworks.items.api.Database;
import com.almworks.items.api.ReadTransaction;
import com.almworks.timetrack.api.TimeTracker;
import com.almworks.timetrack.api.TimeTrackerTask;
import com.almworks.timetrack.api.TimeTrackingCustomizer;
import com.almworks.timetrack.gui.bigtime.SelectBigTimeAction;
import com.almworks.timetrack.impl.TaskTiming;
import com.almworks.util.Env;
import com.almworks.util.Terms;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.commons.Procedure;
import com.almworks.util.components.AActionButton;
import com.almworks.util.components.AToolbar;
import com.almworks.util.components.AToolbarButton;
import com.almworks.util.components.ToolbarBuilder;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.exec.Context;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.i18n.Local;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.swing.AwtUtil;
import com.almworks.util.ui.swing.DocumentUtil;
import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.almworks.util.detach.Lifespan;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

public class TimeTrackerForm implements UIComponentWrapper {
  public static final TimeService.PeriodicalUpdate TRACKING_TICKS = TimeService.PeriodicalUpdate.createSwitchable(500);

  private JPanel myWholePanel;
  private JPanel myTimeSelectorPlace;
  private JPanel myButtonsPlace;
  private JPanel myCurrentIssuePlace;
  private JTextArea myComments;
  private JComponent myToolbar;
  private JScrollPane myCommentsScrollPane;

  private final SimpleProvider myDataProvider =
    new SimpleProvider(DBDataRoles.ITEM_ROLE, ItemWrapper.ITEM_WRAPPER, LoadedItem.LOADED_ITEM);

  private final DetachComposite myLife = new DetachComposite();

  private final ValueModel<Boolean> myTrackingActive = new ValueModel<Boolean>();

  private TimeTrackingCustomizer myCustomizer = Context.require(TimeTrackingCustomizer.ROLE);

  private final ChangeLastEventTimeAction myChangeEventTimeAction = new ChangeLastEventTimeAction();
  private final ChangeRemainingTimeAction myChangeRemainingAction = new ChangeRemainingTimeAction();

  private TimeTrackerTask myShownTask = new TimeTrackerTask(-1L);
  private final Lifecycle myShowArtifactLife = new Lifecycle();
  private final Map<MetaInfo, JComponent> myViewerCache = Collections15.hashMap();
  private final JComponent myNoIssueSelectedMessage = createNoIssue();

  private Color myStoppedColor;
  private Color myStartedColor;
  private Color myPausedColor;

  private String myCurrentComments;
  private boolean myCommentsEnabled;
  private boolean myCurrentCommentsEdited;

  private final PlainDocument myCommentsDocument = new PlainDocument();
  private final PlainDocument myCommentHintDocument = new PlainDocument();
  private DropTarget myDropTarget;

  public TimeTrackerForm() {
    initButtons();
    initViewer();
    initClosing();
    initPanel();
    initComments();

    DataProvider.DATA_PROVIDER.putClientValue(myWholePanel, myDataProvider);
    TRACKING_TICKS.installSwitch(myWholePanel, myTrackingActive);
  }

  private void initComments() {
    DocumentUtil.setDocumentText(myCommentHintDocument, "task comments");
    DocumentUtil.setDocumentText(myCommentsDocument, "");

    myComments.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        updateCommentsField();
      }

      public void focusLost(FocusEvent e) {
        saveCommentsEdit();
        updateCommentsField();
      }
    });
    myCommentsDocument.addDocumentListener(new DocumentAdapter() {
      @Override
      protected void documentChanged(DocumentEvent e) {
        myCurrentCommentsEdited = true;
        myCurrentComments = DocumentUtil.getDocumentText(myCommentsDocument);
        saveCommentsEdit();
      }
    });

    InputMap imap = myComments.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dropEdit");
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "dropEdit");
    myComments.getActionMap().put("dropEdit", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        myToolbar.requestFocusInWindow();
      }
    });

    myComments.setDragEnabled(false);

    ScrollBarPolicy.setDefaultWithHorizontal(myCommentsScrollPane, ScrollBarPolicy.AS_NEEDED);
  }

  private void saveCommentsEdit() {
    TimeTracker tt = Context.require(TimeTracker.class);
    TimeTrackerTask task = tt.getCurrentTask();
    if (task == null)
      return;
    List<TaskTiming> timings = tt.getTaskTimings(task);
    if (timings == null || timings.isEmpty())
      return;
    TaskTiming t = timings.get(timings.size() - 1);
    TaskTiming r = new TaskTiming(t.getStarted(), t.getStopped(), DocumentUtil.getDocumentText(myCommentsDocument));
    tt.replaceTiming(task, t, r);
    myCurrentCommentsEdited = false;
  }

  private void initPanel() {
    myWholePanel.setOpaque(true);
    if (Aqua.isAqua()) {
      myWholePanel.setBorder(UIUtil.BORDER_5);
    }

    final Color panel = AwtUtil.getPanelBackground();
    myStoppedColor = ColorUtil.between(panel, Color.RED, 0.07F);
    myStartedColor = ColorUtil.between(panel, Color.GREEN, 0.1F);
    myPausedColor = ColorUtil.between(panel, Color.YELLOW, 0.1F);
  }

  private void initClosing() {
    AbstractAction closer = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        try {
          new DefaultActionContext(myWholePanel).getSourceObject(WindowController.ROLE).close();
        } catch (CantPerformException ee) {
          Log.debug(ee);
        }
      }
    };
    myWholePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    myWholePanel.getActionMap().put("close", closer);
  }

  private static JComponent createNoIssue() {
    return SingleChildLayout.envelopCenter(
      new JLabel(Local.parse("<html><center>No " + Terms.ref_artifacts + " in work.")));
  }

  private void initViewer() {
    myCurrentIssuePlace.setOpaque(false);

    if (Aqua.isAqua()) {
      myCurrentIssuePlace.setBorder(new CompoundBorder(Aqua.MAC_BORDER_NORTH_SOUTH, UIUtil.BORDER_5));
    } else {
      final Color lbg = ColorUtil.between(myWholePanel.getBackground(), Color.BLACK, 0.1F);
      myCurrentIssuePlace.setBorder(new CompoundBorder(new LineBorder(lbg, 1), UIUtil.BORDER_5));
    }
  }

  private void initButtons() {
    final SelectBigTimeAction sbta = new SelectBigTimeAction();
    myLife.add(sbta.getDetach());
    myTimeSelectorPlace.add(SingleChildLayout.envelop(AToolbar.createActionButton(sbta, null), 0.5F, 0.5F), BorderLayout.CENTER);
    myTimeSelectorPlace.setOpaque(false);

    final ToolbarBuilder b1 = ToolbarBuilder.smallVisibleButtons();
    b1.addAction(MainMenu.Tools.TIME_TRACKING_START);
    b1.addComponent(Box.createHorizontalStrut(8));
    b1.addAction(MainMenu.Tools.TIME_TRACKING_PAUSE);
    b1.addComponent(Box.createHorizontalStrut(8));
    b1.addAction(MainMenu.Tools.TIME_TRACKING_STOP);
    final JPanel controlsToolbar = b1.createHorizontalPanel();
    controlsToolbar.setBorder(new EmptyBorder(4, 0, 4, 0));
    controlsToolbar.setOpaque(false);

    final JPanel startedToolbar = new JPanel(new InlineLayout(InlineLayout.VERTICAL, 0, false));
    startedToolbar.add(createAdjustedButton(myChangeEventTimeAction));
    startedToolbar.add(createAdjustedButton(myChangeRemainingAction));
    startedToolbar.setOpaque(false);

    myButtonsPlace.add(SingleChildLayout.envelop(controlsToolbar, 0.5F, 0.5F), BorderLayout.NORTH);
    myButtonsPlace.add(SingleChildLayout.envelop(startedToolbar, 0.5F, 0.5F), BorderLayout.SOUTH);
    myButtonsPlace.setOpaque(false);
  }

  private AActionButton createAdjustedButton(AnAction action) {
    AToolbarButton button = AToolbar.createActionButton(action, null);
    UIUtil.adjustFont(button, Env.isWindows() ? 0.9F : 0.85F, -1, true);
    return button;
  }

  public JComponent getComponent() {
    return myWholePanel;
  }

  public void attach(final Database db, final TimeTracker tt) {
    tt.getModifiable().addAWTChangeListener(myLife, new ChangeListener() {
      public void onChange() {
        updateFromTracker(db, tt);
      }
    });
    updateFromTracker(db, tt);
  }

  private void updateFromTracker(Database db, TimeTracker tt) {
    myTrackingActive.setValue(tt.isTracking());
    TimeTrackerTask task = tt.getCurrentTask();
    TimeTrackerTask lastTask = tt.getLastTask();
    updateIssue(db, task, lastTask);
    updatePanel(tt.isTracking(), task != null);
    updateComments(task, tt);
  }

  private void updateComments(TimeTrackerTask task, TimeTracker tt) {
    myCommentsEnabled = task != null;
    myCurrentComments = "";
    if (myCommentsEnabled) {
      java.util.List<TaskTiming> timings = tt.getTaskTimings(task);
      if (timings != null && !timings.isEmpty())
        myCurrentComments = timings.get(timings.size() - 1).getComments();
    }
    updateCommentsField();
  }

  private void updateCommentsField() {
    if (myCurrentCommentsEdited)
      return;
    myComments.setEnabled(myCommentsEnabled);
    Document d = myCommentHintDocument;
    boolean hasComments =
      (myCurrentComments != null && myCurrentComments.trim().length() > 0) || (myComments.isFocusOwner());
    if (hasComments && myCommentsEnabled) {
      d = myCommentsDocument;
      DocumentUtil.setDocumentText(myCommentsDocument, myCurrentComments);
      myCurrentCommentsEdited = false;
    }
    Color fg = hasComments ? UIUtil.getEditorForeground() :
      ColorUtil.between(UIUtil.getEditorForeground(), UIUtil.getEditorBackground(), 0.6F);
    if (!Util.equals(fg, myComments.getForeground())) {
      myComments.setForeground(fg);
    }
    if (myComments.getDocument() != d) {
      myComments.setDocument(d);
      if(d == myCommentHintDocument) {
        myDropTarget = myComments.getDropTarget();
        myComments.setDropTarget(null);
      } else {
        myComments.setDropTarget(myDropTarget);
      }
    }
  }

  private void updatePanel(boolean tracking, boolean hasTask) {
    Color bg;
    if (!hasTask) {
      assert !tracking;
      bg = myStoppedColor;
    } else if (tracking) {
      bg = myStartedColor;
    } else {
      bg = myPausedColor;
    }
    Color cbg = myWholePanel.getBackground();
    if (!Util.equals(cbg, bg)) {
      myWholePanel.setBackground(bg);
    }
  }

  private void updateIssue(Database db, TimeTrackerTask task, TimeTrackerTask lastTask) {
    final TimeTrackerTask t = task != null ? task : lastTask;
    if (Util.equals(t, myShownTask)) {
      return;
    }

    myShownTask = t;
    myShowArtifactLife.cycle();

    if (myShownTask == null) {
      setCurrentIssuePlaceContent(myNoIssueSelectedMessage);
    } else {
      final Lifespan life = myShowArtifactLife.lifespan();
      db.readForeground(new ReadTransaction<LoadedItem>() {
        @Override
        public LoadedItem transaction(DBReader reader) throws DBOperationCancelledException {
          return t.loadLive(life, reader);
        }
      }).onSuccess(ThreadGate.AWT, new Procedure<LoadedItem>() {
        @Override
        public void invoke(LoadedItem arg) {
          if(arg != null) {
            showArtifact(life, arg);
          }
        }
      });
    }
  }

  private boolean setCurrentIssuePlaceContent(Component c) {
    if (myCurrentIssuePlace.getComponentCount() != 1 || myCurrentIssuePlace.getComponent(0) != c) {
      myCurrentIssuePlace.removeAll();
      myCurrentIssuePlace.add(c);
      return true;
    }
    return false;
  }

  private void showArtifact(Lifespan life, LoadedItem item) {
    final JComponent viewer = getCachedViewer(item);
    if (viewer == null) {
      return;
    }
    setCurrentIssuePlaceContent(viewer);
    attachIssue(life, item);
  }

  private void attachIssue(Lifespan life, LoadedItem item) {
    if (life.isEnded() || item.services().isRemoteDeleted()) {
      return;
    }
    ItemUiModelImpl model = ItemUiModelImpl.create(item);
    life.add(ItemUiModelImpl.listenItem(model, item));
    DefaultUIController.connectComponent(life, model.getModelMap(), myCurrentIssuePlace);
    myDataProvider.setSingleData(DBDataRoles.ITEM_ROLE, item.getItem());
    myDataProvider.setSingleData(LoadedItem.LOADED_ITEM, item);
    myDataProvider.setSingleData(ItemWrapper.ITEM_WRAPPER, model);
    life.add(new Detach() {
      @Override
      protected void doDetach() throws Exception {
        myDataProvider.removeData(DBDataRoles.ITEM_ROLE);
        myDataProvider.removeData(LoadedItem.LOADED_ITEM);
        myDataProvider.removeData(ItemWrapper.ITEM_WRAPPER);
      }
    });
  }

  private JComponent getCachedViewer(LoadedItem a) {
    final MetaInfo meta = a.getMetaInfo();

    JComponent viewer = myViewerCache.get(meta);
    if (viewer == null) {
      viewer = myCustomizer.createBoxViewer(myLife, a);
      myViewerCache.put(meta, viewer);
    }

    return viewer;
  }

  public void dispose() {
    myLife.detach();
    myViewerCache.clear();
    myShowArtifactLife.dispose();
  }

  public Component getInitialFocusOwner() {
    return myButtonsPlace;
  }

  private void createUIComponents() {
    final ToolbarBuilder builder = ToolbarBuilder.smallVisibleButtons();
    builder.addAction(MainMenu.Search.OPEN_ITEM_IN_FRAME);
    builder.addAction(MainMenu.Search.OPEN_ITEM_IN_BROWSER);
    builder.addSeparator();
    builder.addAction(MainMenu.X_PUBLISH_TIME);

    final JPanel toolbar = builder.createHorizontalPanel();
    toolbar.setMinimumSize(new Dimension(24*3, 24));
    toolbar.setOpaque(false);
    if (Aqua.isAqua()) {
      toolbar.setBorder(null);
    }
    myToolbar = toolbar;

    // for focus
    myToolbar.setFocusable(true);
  }
}
