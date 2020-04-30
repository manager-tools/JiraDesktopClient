package com.almworks.gui;

import com.almworks.api.application.ApplicationLoadStatus;
import com.almworks.api.container.ComponentContainer;
import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.*;
import com.almworks.util.Env;
import com.almworks.util.collections.ChangeListener;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.commons.Factory;
import com.almworks.util.config.Configuration;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.images.Icons;
import com.almworks.util.model.ValueModel;
import com.almworks.util.ui.*;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import com.almworks.util.ui.macosx.FullScreenEvent;
import org.almworks.util.Log;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifecycle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;

import static java.awt.Frame.ICONIFIED;

/**
 * @author : Dyoma
 */
public class MainWindowManagerImpl implements MainWindowManager {
  private final Configuration myLayout;
  private final JFrame myFrame;
  private final JPanel myMainPanel;
  @Nullable
  private JComponent myContentComponent;
  private final Lifecycle myLayoutDetach = new Lifecycle();
  private final ApplicationManager myApplicationManager;
  private final StatusBarImpl myStatusBarImpl;
  private final WindowControllerImpl myWindowController;
  private final WindowDescriptor myWindowDescriptor;
  private boolean myHideOnMinimizeAndClose;

  private final ComponentContainer myMainContainer;
  private final DetachComposite myShowLife;

  public MainWindowManagerImpl(ApplicationManager applicationManager, Configuration configuration,
    ComponentContainer container, WindowDescriptor windowDescriptor, ActionRegistry actionRegistry, ApplicationLoadStatus startup) {

    myLayout = configuration;
    myApplicationManager = applicationManager;
    myMainContainer = container;
    myWindowDescriptor = windowDescriptor;
    myStatusBarImpl = new StatusBarImpl();
    myMainPanel = new JPanel(new BorderLayout(0, 4));
    ThreadGate.AWT_IMMEDIATE.execute(new Runnable() {
      public void run() {
        GlobalDataRoot.install(myMainPanel);
      }
    });
    myShowLife = new DetachComposite();
    myFrame = new JFrame() {
      @Override
      public void dispose() {
        super.dispose();
        myShowLife.detach();
      }
    };
    myFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    if(!Env.isMac()) {
      myFrame.setIconImage(Icons.APPLICATION_LOGO_ICON_BIG.getImage());
    } else {
      if (Env.isMacLionOrNewer() && MacIntegration.isFullScreenSupported()) {
        MacIntegration.makeWindowFullScreenable(myFrame);
        FullScreenAction.install(actionRegistry, myFrame);
      }
      MacIntegration.setReopenHandler(new Runnable() {
        @Override
        public void run() {
          if(UIUtil.isMinimized(myFrame)) {
            MainWindowManagerImpl.this.bringToFront();
          }
        }
      });
    }
    myFrame.setName(myFrame.getName() + "@MainFrame");
    ActionScope.set(myFrame, "MainFrame");
    myFrame.addWindowStateListener(new WindowStateListener() {
      public void windowStateChanged(WindowEvent e) {
        int os = e.getOldState();
        int ns = e.getNewState();
        if ((os & ICONIFIED) == 0 &&  (ns & ICONIFIED) != 0 && myHideOnMinimizeAndClose && !Env.isMac()) {
          if (myFrame.isVisible())
            myFrame.setVisible(false);
        }
      }
    });

    myWindowController = new WindowControllerImpl(myFrame, ApplicationManager.EXIT_ACTION, myShowLife);
    myFrame.setFocusTraversalPolicy(new FilteringFocusTraversalPolicy());
    myFrame.getContentPane().add(myMainPanel);
    myFrame.getContentPane().add(myStatusBarImpl.getComponent(), BorderLayout.SOUTH);
    MutableComponentContainer subcontainer = container.createSubcontainer("mainWindowContainer");
    subcontainer.registerActor(WindowController.ROLE, myWindowController);
    subcontainer.registerActorClass(DialogManager.ROLE, DialogManagerImpl2.class);
    myWindowController.addDataProvider(new ContainerDataProvider(subcontainer));
    JOptionPane.setRootFrame(myFrame);

    createMainFrame();
    final ApplicationLoadStatus.StartupActivity windowStartup = startup.createActivity("mainWindow");
    UIUtil.addComponentListener(windowStartup.getLife(), myFrame, new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        check();
      }

      public void componentResized(ComponentEvent e) {
        check();
      }

      private void check() {
        if (!myFrame.isDisplayable()) return;
        Dimension size = myFrame.getSize();
        if (size != null && (size.width != 0 || size.height != 0)) {
          windowStartup.done();
        }
      }
    });
  }

  private void createMainFrame() {
    if (!Env.isMac()) {
      Factory<JMenuBar> menuFactory = myWindowDescriptor.getMainMenuFactory();
      myFrame.setJMenuBar(createMainFrameMenu(menuFactory));
    }

    final ValueModel<String> windowTitle = myWindowDescriptor.getWindowTitle();
    ChangeListener titleListener = new ChangeListener() {
      @Override
      public void onChange() {
        myFrame.setTitle(windowTitle.getValue());
      }
    };
    windowTitle.addAWTChangeListener(myShowLife, titleListener);
    titleListener.onChange();

    myApplicationManager.addListener(new ApplicationManager.Adapter() {
      public void onBeforeExit() {
        try {
          myFrame.dispose();
        } catch (Exception e) {
          // ignore
          Log.debug("on dispose: " + e);
        }
      }
    });
  }

  private JMenuBar createMainFrameMenu(Factory<JMenuBar> menuFactory) {
    final JMenuBar jmb = menuFactory.create();
    ComponentProperty.JUMP.putClientValue(jmb, myMainPanel);
    return jmb;
  }

  public void setContentComponent(@Nullable JComponent component) {
    if (myContentComponent != null)
      myMainPanel.remove(myContentComponent);
    if (component != null)
      myMainPanel.add(component, BorderLayout.CENTER);
    myContentComponent = component;
    myLayoutDetach.cycle();
    if (myContentComponent != null) {
      Dimension preferred = UIUtil.getDefaultScreenUserSize();
      preferred.width -= 100;
      preferred.height -= 120;
      Configuration config = myLayout.getOrCreateSubset("mainWindow");
      WindowUtil.setupWindow(myLayoutDetach.lifespan(), myFrame, config, true, preferred, false, null, null);
    }
    myMainPanel.invalidate();
    myMainPanel.revalidate();
    myMainPanel.repaint();
  }

  public void showWindow(boolean show) {
    if (!show) {
      myFrame.setVisible(false);
      return;
    }
    installDefaultMenuBar();
    Runnable showWindow = () -> {
      myFrame.setVisible(true);
//      if (menuBar != null) menuBar.addNotify();
    };
    SwingUtilities.invokeLater(showWindow);
  }

  private JMenuBar installDefaultMenuBar() {
    if (!Env.isMac()) return null;
    final JMenuBar menuBar = createMainFrameMenu(myWindowDescriptor.getMainMenuFactory());
    final MutableComponentContainer container = myMainContainer.createSubcontainer("macDefaultMenuBar");
    final DataProvider provider = new ContainerDataProvider(container);
    DataProvider.DATA_PROVIDER.putClientValue(menuBar, provider);
    MacIntegration.setDefaultMenuBar(menuBar);
    menuBar.addNotify(); // Must be done immediately. Otherwise menu items may be disabled on Mac
    return menuBar;
  }

  public StatusBar getStatusBar() {
    return myStatusBarImpl;
  }

  // kludge: dyoma review - see AboutDialog
  public JFrame getMainFrame() {
    return myFrame;
  }

  public void bringToFront() {
    restoreFrame();
    myFrame.toFront();
    grabFocus();
  }

  private void grabFocus() {
    // ??
    Component focusOwner = myFrame.getFocusOwner();
    if (focusOwner != null)
      focusOwner.requestFocus();
  }

  public void minimize() {
    UIUtil.minimizeFrame(myFrame);
  }

  public void setHideOnMinimizeAndClose(boolean hide) {
    myHideOnMinimizeAndClose = hide;
    myWindowController.setHideOnClose(hide);
  }

  private void restoreFrame() {
    UIUtil.restoreFrame(myFrame);
  }

  private static class FullScreenAction implements AnAction, FullScreenEvent.Listener {
    private final Window myWindow;
    private final SimpleModifiable myModifiable = new SimpleModifiable();

    public static void install(ActionRegistry registry, Window window) {
      FullScreenAction action = new FullScreenAction(window);
      registry.registerAction(MainMenu.Windows.TOGGLE_FULL_SCREEN, action);
      MacIntegration.addFullScreenListener(window, action);
    }

    private FullScreenAction(Window window) {
      myWindow = window;
    }

    @Override
    public void update(UpdateContext context) throws CantPerformException {
      context.setEnabled(true);
      context.putPresentationProperty(PresentationKey.NAME,
        MacIntegration.isWindowInFullScreen(myWindow) ? "Exit Full Screen" : "Enter Full Screen");
      context.updateOnChange(myModifiable);
    }

    @Override
    public void perform(ActionContext context) throws CantPerformException {
      MacIntegration.toggleFullScreen(myWindow);
    }

    @Override
    public void onFullScreenEvent(FullScreenEvent e) {
      myModifiable.fireChanged();
    }
  }
}
