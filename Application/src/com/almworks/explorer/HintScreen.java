package com.almworks.explorer;

import com.almworks.api.application.ExplorerComponent;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.misc.WorkArea;
import com.almworks.util.components.ALabel;
import com.almworks.util.components.ScrollablePanel;
import com.almworks.util.components.plaf.macosx.Aqua;
import com.almworks.util.components.plaf.patches.Aero;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.files.FileUtil;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.SingleChildLayout;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.ActionContext;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.CantPerformException;
import com.almworks.util.ui.actions.EnabledAction;
import org.almworks.util.Log;
import org.picocontainer.Startable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class HintScreen implements Startable {
  public static final Role<HintScreen> ROLE = Role.role("WhatsNew");

  private final WorkArea myWorkArea;
  private final ExplorerComponent myExplorerComponent;
  private final Configuration myConfig;
  private final ActionRegistry myActionRegistry;
  private final ALabel myWhatsNewLabel = new ALabel();

  private static final String WHATSNEW_TIMESTAMP = "whatsnewtime";

  public HintScreen(WorkArea workArea, ExplorerComponent explorerComponent, Configuration config,
    ActionRegistry actionRegistry)
  {
    myWorkArea = workArea;
    myExplorerComponent = explorerComponent;
    myConfig = config;
    myActionRegistry = actionRegistry;

    myWhatsNewLabel.setAlignmentX(0F);
    myWhatsNewLabel.setAlignmentY(0F);
    myWhatsNewLabel.setVerticalAlignment(SwingConstants.TOP);
  }

  @Override
  public void start() {
    myActionRegistry.registerAction(MainMenu.Help.WHATS_NEW, new EnabledAction("What's &New") {
      @Override
      protected void doPerform(ActionContext context) throws CantPerformException {
        showWhatsNew(false);
      }
    });
  }

  @Override
  public void stop() {
  }

  public void showWhatsNew(final boolean onlyIfNewer) {
    ThreadGate.LONG.execute(new Runnable() {
      @Override
      public void run() {
        final String text = readWhatsNew(onlyIfNewer);
        if (text == null)
          return;
        ThreadGate.AWT.execute(new Runnable() {
          @Override
          public void run() {
            openWhatsNew(text);
          }
        });
      }
    });
  }

  private void openWhatsNew(String text) {
    myWhatsNewLabel.setText(text);
    myWhatsNewLabel.setPreferredWidth(UIUtil.getColumnWidth(myWhatsNewLabel) * 80);
    JPanel panel = SingleChildLayout.envelop(myWhatsNewLabel, 0, 0);
    panel.setOpaque(true);
    panel.setBackground(UIUtil.getEditorBackground());
    panel.setBorder(UIUtil.BORDER_9);

    final JScrollPane scrollPane = new JScrollPane(new ScrollablePanel(panel));
    Aqua.setLightNorthBorder(scrollPane);
    Aero.cleanScrollPaneBorder(scrollPane);

    myExplorerComponent.showComponent(new UIComponentWrapper.Simple(scrollPane), "What's New");
  }

  private String readWhatsNew(boolean onlyIfNewer) {
    File whatsnew = myWorkArea.getEtcFile(WorkArea.ETC_WELCOME_HTML);
    if (whatsnew == null) {
      Log.warn("not whats new file");
      return null;
    }
    if (!whatsnew.isFile()) {
      Log.warn(whatsnew + " is not a file");
      return null;
    }
    if (!whatsnew.canRead()) {
      Log.warn("cannot read " + whatsnew);
      return null;
    }

    long time = whatsnew.lastModified();
    if (time != 0 && onlyIfNewer) {
      long last = myConfig.getLongSetting(WHATSNEW_TIMESTAMP, 0);
      if (Math.abs(time - last) < 30000) {
        // already shown
        return null;
      }
    }

    String text;
    try {
      text = FileUtil.readFile(whatsnew);
    } catch (IOException e) {
      Log.warn("cannot read " + whatsnew, e);
      return null;
    }

    if (time != 0) {
      myConfig.setSetting(WHATSNEW_TIMESTAMP, time);
    }

    return text;
  }
}