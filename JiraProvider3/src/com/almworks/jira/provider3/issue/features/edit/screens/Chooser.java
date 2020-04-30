package com.almworks.jira.provider3.issue.features.edit.screens;

import com.almworks.items.gui.edit.EditItemModel;
import com.almworks.jira.provider3.issue.editor.IssueScreen;
import com.almworks.jira.provider3.issue.editor.ScreenController;
import com.almworks.util.LogHelper;
import com.almworks.util.advmodel.SelectionInListModel;
import com.almworks.util.advmodel.SelectionListener;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.components.AComboBox;
import com.almworks.util.components.Canvas;
import com.almworks.util.components.CanvasRenderer;
import com.almworks.util.components.renderer.CellState;
import com.almworks.util.components.renderer.Renderers;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

class Chooser {
  private final AComboBox<EditIssueScreen> myScreenChooser = new AComboBox<EditIssueScreen>();
  @Nullable
  private final ScreenScheme myResolvedScheme;

  public Chooser(SelectionInListModel<EditIssueScreen> model, @Nullable ScreenScheme resolvedScheme) {
    myResolvedScheme = resolvedScheme;
    myScreenChooser.setCanvasRenderer(Renderers.defaultCanvasRenderer());
    myScreenChooser.setModel(model);
    myScreenChooser.setColumns(0);
    myScreenChooser.setMinColumns(2);
    myScreenChooser.setMaxToPref(true);
  }

  public boolean attach(Lifespan life, ScreenController controller, Configuration config, boolean create) {
    return SelectionConfig.install(life, this, controller, config, create) != null;
  }

  public JComponent getComponent() {
    return myScreenChooser;
  }

  private static EditIssueScreen chooseInitialScreen(ScreenScheme scheme, Iterable<EditIssueScreen> allScreens, EditItemModel model, boolean create, String screenId) {
    if (screenId == null || screenId.isEmpty()) {
      EditIssueScreen screen = RemoteScreen.choose(scheme, allScreens, model, create);
      if (screen != null) return screen;
    } else for (EditIssueScreen screen : allScreens) if (screenId.equals(screen.getLocalId())) return screen;
    EditIssueScreen screen = RelevantScreen.choose(allScreens);
    if (screen == null) LogHelper.error("No screen to edit issue(s)");
    return screen;
  }

  static class SelectionConfig extends SelectionListener.Adapter implements CanvasRenderer<EditIssueScreen> {
    private static final String S_SCREEN_ID = "screenConfig";
    private final Chooser myChooser;
    private final ScreenController myController;
    private final Configuration myConfig;
    private final boolean myCreate;
    /** Flag that selected screen is changed by JC, not by user */
    private boolean myAutoChange = false;
    /** true - mode when JIRA screens changed according to model changes (project, type) */
    private boolean mySwitchJiraMode = true;

    private SelectionConfig(Chooser chooser, ScreenController controller, Configuration config, boolean create) {
      myChooser = chooser;
      myController = controller;
      myConfig = config;
      myCreate = create;
    }

    @Override
    public void renderStateOn(CellState state, Canvas canvas, EditIssueScreen item) {
      if (mySwitchJiraMode && isJiraScreen(item)) canvas.setFontStyle(Font.BOLD);
      if (item != null) item.renderOn(canvas, state);
    }

    @Override
    public void onSelectionChanged() {
      if (!myAutoChange) mySwitchJiraMode = false; // Switch to manual only screen switching
      EditIssueScreen screen = myChooser.myScreenChooser.getModel().getSelectedItem();
      myController.setScreen(screen);
      if (screen != null) {
        String screenId = isJiraScreen(screen) ? "" : screen.getLocalId();
        myConfig.setSetting(S_SCREEN_ID, screenId);
      }
      ThreadGate.AWT_QUEUED.execute(new Runnable() {
        @Override
        public void run() {
          EditIssueScreen screen = myChooser.myScreenChooser.getModel().getSelectedItem();
          myChooser.myScreenChooser.setToolTipText(getScreenTooltip(screen));
        }
      });
    }

    private String getScreenTooltip(EditIssueScreen screen) {
      if (screen == null)
        return null;
      String tooltip = screen.getTooltip();
      if (mySwitchJiraMode && isJiraScreen(screen)) tooltip = tooltip + " auto-switching mode";
      return tooltip;
    }

    private void onModelChanged(EditItemModel model) {
      IssueScreen currentScreen = myChooser.myScreenChooser.getModel().getSelectedItem();
      if (mySwitchJiraMode && isJiraScreen(currentScreen)) {
        EditIssueScreen screen = RemoteScreen.choose(myChooser.myResolvedScheme, myChooser.myScreenChooser.getModel(), model, myCreate);
        setSelectedScreen(screen);
      }
    }

    private void setSelectedScreen(EditIssueScreen screen) {
      try {
        myAutoChange = true;
        if (screen != null) myChooser.myScreenChooser.getModel().setSelectedItem(screen);
      } finally {
        myAutoChange = false;
      }
    }

    private boolean isJiraScreen(IssueScreen currentScreen) {
      return currentScreen instanceof RemoteScreen;
    }

    @Nullable("When cannot select initial screen")
    public static SelectionConfig install(Lifespan life, Chooser chooser, final ScreenController controller, final Configuration config, boolean create) {
      final SelectionConfig selectionConfig = new SelectionConfig(chooser, controller, config, create);
      chooser.myScreenChooser.setCanvasRenderer(selectionConfig);
      chooser.myScreenChooser.getModel().addSelectionListener(life, selectionConfig);
      String screenId = config.getSetting(S_SCREEN_ID, "");
      EditIssueScreen screen = chooseInitialScreen(chooser.myResolvedScheme, chooser.myScreenChooser.getModel(), controller.getModel(), create, screenId);
      if (screen == null) return null;
      selectionConfig.setSelectedScreen(screen);
      controller.getModel().addAWTChangeListener(life, new ChangeListener() {
        @Override
        public void onChange() {
          selectionConfig.onModelChanged(controller.getModel());
        }
      });
      return selectionConfig;
    }
  }
}
