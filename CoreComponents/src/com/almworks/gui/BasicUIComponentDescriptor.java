package com.almworks.gui;

import com.almworks.api.container.RootContainer;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.MainMenu;
import com.almworks.api.gui.MainWindowManager;
import com.almworks.api.gui.WindowManager;
import com.almworks.api.misc.TimeService;
import com.almworks.api.platform.ComponentDescriptor;
import com.almworks.api.tray.SystemTrayComponentDescriptor;
import com.almworks.api.tray.TrayIconService;
import com.almworks.feedback.ThreadDumpAction;
import com.almworks.platform.DiagnosticRecorder;
import com.almworks.platform.RegisterActions;
import com.almworks.util.Env;
import com.almworks.util.GlobalProperties;
import com.almworks.util.commons.Factory;
import com.almworks.util.config.JDOMConfigurator;
import com.almworks.util.config.ReadonlyConfiguration;
import com.almworks.util.files.FileUtil;
import com.almworks.util.model.ValueModel;
import com.almworks.util.properties.Role;
import com.almworks.util.ui.actions.ActionRegistry;
import com.almworks.util.ui.actions.MenuLoader;

import javax.swing.*;
import java.util.Properties;

/**
 * @author : Dyoma
 */
public class BasicUIComponentDescriptor implements ComponentDescriptor {
  public void registerActors(RootContainer container) {
    container.registerActorClass(ActionRegistry.ROLE, AppActionRegistry.class);
    container.registerActorClass(WindowTitleSections.ROLE, WindowTitleSections.class);
    container.registerActorClass(MainWindowManager.WindowDescriptor.ROLE, DefaultMainMenu.class);
    container.registerActorClass(MainWindowManager.ROLE, MainWindowManagerImpl.class);
    container.registerActorClass(AboutDialog.ROLE, AboutDialog.class);
    container.registerActorClass(WindowManager.ROLE, DefaultWindowManager.class);
    container.registerActorClass(DialogManager.ROLE, DefaultDialogManager.class);
    container.registerActorClass(Role.role("defaultActions"), DefaultActionsCollection.class);
    container.registerActorClass(TrayIconService.ROLE, SystemTrayComponentDescriptor.class);
    container.registerActorClass(TimeService.ROLE, TimeService.class);
    container.registerActorClass(DiagnosticRecorder.ROLE, DiagnosticRecorder.class);
    container.registerActorClass(Role.role("diagnosticUI"), DiagnosticUI.class);
    if (Env.getBoolean(GlobalProperties.INTERNAL_ACTIONS)) RegisterActions.registerAction(container, MainMenu.Tools.DUMP_THREADS, new ThreadDumpAction());
  }

  static class DefaultMainMenu implements MainWindowManager.WindowDescriptor {
    private static final String MAIN_MENU = "com/almworks/gui/MainMenu.xml";
    private static final String MENU_118N = "com/almworks/rc/menu.properties";

    private final WindowTitleSections myWindowTitle;

    public DefaultMainMenu(WindowTitleSections windowTitle) {
      myWindowTitle = windowTitle;
    }

    @Override
    public Factory<JMenuBar> getMainMenuFactory() {
      return new Factory<JMenuBar>() {
        final Properties i18n = FileUtil.loadProperties(DefaultMainMenu.class.getClassLoader(), MENU_118N);
        final MenuLoader menuLoader = new MenuLoader();
        final ReadonlyConfiguration menuConfig = JDOMConfigurator.parse(DefaultMainMenu.class.getClassLoader(), MAIN_MENU);

        public JMenuBar create() {
          return menuLoader.loadMenuBar(menuConfig, i18n);
        }
      };
    }

    @Override
    public ValueModel<String> getWindowTitle() {
      return myWindowTitle.getModel();
    }
  }
}
