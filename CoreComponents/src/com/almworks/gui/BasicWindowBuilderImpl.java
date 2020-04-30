package com.almworks.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.exec.ApplicationManager;
import com.almworks.api.gui.BasicWindowBuilder;
import com.almworks.api.gui.DialogManager;
import com.almworks.api.gui.WindowController;
import com.almworks.api.gui.WindowManager;
import com.almworks.util.Env;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.exec.ThreadGate;
import com.almworks.util.properties.Role;
import com.almworks.util.threads.Computable;
import com.almworks.util.threads.Threads;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.WindowMinSizeWatcher;
import com.almworks.util.ui.WindowUtil;
import com.almworks.util.ui.actions.*;
import org.almworks.util.Collections15;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * @author : Dyoma
 */
abstract class BasicWindowBuilderImpl<W extends Window & RootPaneContainer> implements BasicWindowBuilder<UIComponentWrapper> {
  private static final Role<? extends UIComponentWrapper> ROLE = Role.role("content");

  private final Configuration myConfiguration;
  private final DetachComposite myDetachOnDispose = new DetachComposite();
  private String myTitle;
  private final MutableComponentContainer myContainer;
  private AnActionListener myCloseConfirmation = AnActionListener.DEAF;
  @Nullable
  protected Component myInitialFocusOwner;
  private List<DataProvider> myAdditionalProviders;
  private boolean myIgnoreStoredSize = false;
  private WindowUtil.WindowPositioner myWindowPositioner;
  private String myActionScope;
  private JButton myDefaultButton;
  private boolean myAddRootPaneDataRoot = false;

  protected BasicWindowBuilderImpl(MutableComponentContainer container, Configuration configuration) {
    myContainer = container;
    myConfiguration = configuration;
  }

  public void setTitle(String title) {
    myTitle = title;
  }

  protected String getTitle() {
    return myTitle;
  }

  public WindowController showWindow() {
    return showWindow(Detach.NOTHING);
  }

  public void addGlobalDataRoot() {
    myAddRootPaneDataRoot = true;
  }

  public final WindowController showWindow(final Detach disposeNotification) {
    Threads.assertAWTThread();
    myDetachOnDispose.add(disposeNotification);

    assert myTitle != null;
    final W window = createWindow(myDetachOnDispose, myAddRootPaneDataRoot);
    WindowMinSizeWatcher.install(window);
    final WindowController controller = new WindowControllerImpl(window, myCloseConfirmation, myDetachOnDispose);
    controller.addDataProvider(new ContainerDataProvider(myContainer));
    if (myAdditionalProviders != null) {
      for (DataProvider provider : myAdditionalProviders) {
        controller.addDataProvider(provider);
      }
    }

    myContainer.registerActor(WindowController.ROLE, controller);
    myContainer.registerActorClass(WindowManager.ROLE, DefaultWindowManager.class);
    myContainer.registerActorClass(DialogManager.ROLE, DialogManagerImpl2.class);
    myContainer.start();

    final UIComponentWrapper content = myContainer.getActor(getContentRole());
    assert content != null;
    insertContent(window, content);

    setupCloseConfirmationOnAppExit(content);

    Configuration sizeConfig = myConfiguration.getOrCreateSubset("mainWindow");
    WindowUtil.setupWindow(
      myDetachOnDispose, window, sizeConfig, true, getPreferredSize(), myIgnoreStoredSize,
      getRequiredDisplay(), getWindowPositioner());
    window.setName(window.getName() + "@" + myConfiguration.getName());
    if(myActionScope != null) {
      ActionScope.set(window, myActionScope);
    }
    if (myDefaultButton != null) {
      window.getRootPane().setDefaultButton(myDefaultButton);
    }
    setupSystemCloseShortcut(window);
    Component c = myInitialFocusOwner;
    if (c == null) {
      c = InitialWindowFocusFinder.findInitialWindowComponent(window);
    }
    if (c != null) {
      UIUtil.requestFocusInWindowLater(c);
    }
    controller.show();
    afterShowWindow();
    return controller;
  }

  private void setupCloseConfirmationOnAppExit(final UIComponentWrapper content) {
    final ApplicationManager am = myContainer.getActor(ApplicationManager.ROLE);
    if(am != null) {
      myDetachOnDispose.add(am.addListener(new ApplicationManager.Adapter() {
        @Override
        public void onExitRequested(ApplicationManager.ExitRequest r) {
          if(r.isCancelled()) {
            return;
          }

          final Boolean canClose = ThreadGate.AWT_IMMEDIATE.compute(new Computable<Boolean>() {
            @Override
            public Boolean compute() {
              try {
                myCloseConfirmation.perform(new DefaultActionContext(content.getComponent()));
                return true;
              } catch(CantPerformException e) {
                return false;
              }
            }
          });

          if(!Boolean.TRUE.equals(canClose)) {
            r.cancel();
          }
        }
      }));
    } else {
      assert false;
    }
  }

  private void setupSystemCloseShortcut(final W window) {
    setupSystemCloseShortcut(Env.isMac(), window, KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.META_DOWN_MASK));
  }

  private void setupSystemCloseShortcut(boolean condition, final W window, final KeyStroke keyStroke) {
    if(condition) {
      final String id = "systemClose";
      final JRootPane rp = window.getRootPane();
      rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, id);
      rp.getActionMap().put(id, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          try {
            final JComponent cmp = Util.NN(Util.castNullable(JComponent.class, e.getSource()), rp);
            WindowController.CLOSE_ACTION.perform(new DefaultActionContext(cmp));
          } catch (CantPerformException e1) {
            // can't close?
          }
        }
      });
    }
  }

  @Nullable
  protected GraphicsConfiguration getRequiredDisplay() {
    return null;
  }

  @Nullable
  private WindowUtil.WindowPositioner getWindowPositioner() {
    return myWindowPositioner;
  }

  @Override
  public void setWindowPositioner(WindowUtil.WindowPositioner adjuster) {
    myWindowPositioner = adjuster;
  }

  protected void afterShowWindow() {
  }

  public Configuration getConfiguration() {
    return myConfiguration.getOrCreateSubset("contentConfig");
  }

  public void setContent(UIComponentWrapper content) {
    getWindowContainer().registerActor(getContentRole(), content);
  }

  public void setContent(JComponent content) {
    setContent(new UIComponentWrapper.Simple(content));
  }

  public void setContentClass(Class<? extends UIComponentWrapper> contentClass) {
    getWindowContainer().registerActorClass(getContentRole(), contentClass);
  }

  public Role<UIComponentWrapper> getContentRole() {
    return (Role<UIComponentWrapper>) ROLE;
  }

  protected abstract Dimension getPreferredSize();

  protected abstract W createWindow(Detach disposeNotification, boolean addRootPaneDataRoot);

  protected abstract void insertContent(W window, UIComponentWrapper content);

  public MutableComponentContainer getWindowContainer() {
    return myContainer;
  }

  protected static AnAction createCloseAction(final AnAction action,
    final FireEventSupport<AnActionListener> listeners)
  {
    return new AnActionDelegator(action) {
      public void perform(ActionContext context) throws CantPerformException {
        myDelegate.perform(context);
        WindowController.CLOSE_ACTION.perform(context);
        listeners.getDispatcher().perform(context);
      }
    };
  }

  public void setCloseConfirmation(final AnActionListener listener) {
    assert listener != null;
    myCloseConfirmation = listener;
  }

  public void addProvider(DataProvider provider) {
    if (myAdditionalProviders == null)
      myAdditionalProviders = Collections15.arrayList();
    myAdditionalProviders.add(provider);
  }

  public void setIgnoreStoredSize(boolean ignore) {
    myIgnoreStoredSize = ignore;
  }

  public void setInitialFocusOwner(Component component) {
    myInitialFocusOwner = component;
  }

  @Nullable
  public Component getInitialFocusOwner() {
    return myInitialFocusOwner;
  }

  public void detachOnDispose(Detach detach) {
    myDetachOnDispose.add(detach);
  }

  public void setActionScope(String actionScope) {
    myActionScope = actionScope;
  }

  @Override
  public void setDefaultButton(JButton button) {
    myDefaultButton = button;
  }
}
