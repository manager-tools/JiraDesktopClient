package com.almworks.gui;

import com.almworks.api.container.MutableComponentContainer;
import com.almworks.api.gui.DialogBuilder;
import com.almworks.api.gui.WindowController;
import com.almworks.util.Env;
import com.almworks.util.L;
import com.almworks.util.commons.Factory;
import com.almworks.util.components.AActionButton;
import com.almworks.util.config.Configuration;
import com.almworks.util.events.FireEventSupport;
import com.almworks.util.images.Icons;
import com.almworks.util.ui.UIComponentWrapper;
import com.almworks.util.ui.UIUtil;
import com.almworks.util.ui.actions.*;
import com.almworks.util.ui.actions.globals.GlobalDataRoot;
import com.almworks.util.ui.swing.Shortcuts;
import org.almworks.util.Collections15;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * @author : Dyoma
 */
public class DialogBuilderImpl2 extends BasicWindowBuilderImpl<DialogBuilderImpl2.MyDialog> implements DialogBuilder {
  private static final Border GAP_AROUND_BORDER = new EmptyBorder(UIUtil.GAP, UIUtil.GAP, UIUtil.GAP, UIUtil.GAP);
  private static final Border GAP_SWE_BORDER = new EmptyBorder(0, UIUtil.GAP, UIUtil.GAP, UIUtil.GAP);
  private static final Border GAP_NORTH_BORDER = new EmptyBorder(UIUtil.GAP, 0, 0, 0);

  private final Factory<Window> myCreator;
//  private final Window myOwner;
  private Dimension myPreferredSize = null;
  private AnAction myOkAction = null;
  private AnAction myCancelAction = null;
  private AnAction myOkCancelAction = null;
  private final List<AnAction> myExtraActions = Collections15.arrayList();
  private boolean myResizable = true;
  private boolean myModal = false;
  private final FireEventSupport<AnActionListener> myOkListeners = FireEventSupport.createSynchronized(AnActionListener.class);
  private final FireEventSupport<AnActionListener> myCancelListeners = FireEventSupport.createSynchronized(AnActionListener.class);
  private AActionButton myOkButton = null;
  private UIComponentWrapper myBottomLineComponent = null;
  private boolean myBottomBevel = true;
  private boolean myAlwaysOnTop;
  private boolean myNullOwner;
  private boolean myBorders = true;
  private boolean myBottomLineShown = true;
  private Image myIcon;

  public DialogBuilderImpl2(MutableComponentContainer container, Configuration configuration, Factory<Window> creator) {
    super(container, configuration);
    myCreator = creator;
//    myOwner = owner;
  }

  public AnAction setOkAction(AnAction action) {
    assert action != null;
    myOkAction = createCloseAction(action, myOkListeners);
    return myOkAction;
  }

  /*
  * Use this method when you need an OK action which will also be performed when a user closes a window.
  * It will also listen to keyboard keys of a cancel action, and cancel listeners will also be performed with it.
  * */
  public AnAction setOkCancelAction(AnAction action) {
    assert action != null;
    myOkCancelAction = createCloseAction(createCloseAction(action, myOkListeners), myCancelListeners);
    return myOkCancelAction;
  }

  public DialogBuilder setModal(boolean modal) {
    myModal = modal;
    return this;
  }

  public void setCancelAction(AnAction action) {
    assert action != null;
    myCancelAction = createCloseAction(action, myCancelListeners);
  }

  public Detach addOkListener(AnActionListener listener) {
    return myOkListeners.addStraightListener(listener);
  }

  public Detach addCancelListener(AnActionListener listener) {
    return myCancelListeners.addStraightListener(listener);
  }

  public void setEmptyCancelAction() {
    setCancelAction(CANCEL);
  }

  public AnAction setEmptyOkAction() {
    return setOkAction(OK);
  }

  public AnAction setEmptyOkCancelAction() {
    return setOkCancelAction(OK);
  }

  public void setResizable(boolean resizable) {
    myResizable = resizable;
  }

  public void addAction(AnAction action) {
    assert action != null;
    myExtraActions.add(action);
  }

  public void addCloseAction(String name) {
    addAction(new SimpleAction(name) {
      protected void customUpdate(UpdateContext context) {
      }

      protected void doPerform(ActionContext context) throws CantPerformException {
        WindowController.CLOSE_ACTION.perform(context);
      }
    });
  }

  public void setCancelAction(String name) {
    setCancelAction(SimpleAction.createDoNothing(name));
  }

  protected Dimension getPreferredSize() {
    return myPreferredSize;
  }

  protected MyDialog createWindow(Detach disposeNotification, boolean addRootPaneDataRoot) {
    Window owner = null;
    if (!myNullOwner) {
      owner = myCreator.create();
    }

    MyDialog dialog = createMyDialog(owner);
    dialog.setTitle(getTitle());
    dialog.setResizable(myResizable);
    dialog.addToDispose(disposeNotification);
    dialog.setModal(myModal);
    if (myIcon != null)
      dialog.setIconImage(myIcon);
    if (myAlwaysOnTop)
      dialog.setAlwaysOnTop(true);
    if (myBottomLineComponent != null)
      dialog.setBottomLineComponent(myBottomLineComponent.getComponent());
    if (myNullOwner && !Env.isMac()) {
      Window dialogOwner = dialog.getOwner();
      if (dialogOwner instanceof Frame) {
        ((Frame) dialogOwner).setIconImage(Icons.APPLICATION_LOGO_ICON_BIG.getImage());
      }
    }
    if (!myBorders) {
      dialog.myMainPanel.setBorder(null);
      dialog.myBottom.setBorder(myBottomBevel ? GAP_AROUND_BORDER : GAP_SWE_BORDER);
    }
    if (myBottomBevel) {
      dialog.addBottomBevel();
    }
    if (!myBottomLineShown) {
      dialog.myMainPanel.remove(dialog.myBottom);
    }
    if (addRootPaneDataRoot) {
      GlobalDataRoot.install(dialog.getRootPane());
    }
    return dialog;
  }

  private static MyDialog createMyDialog(Window owner) {
    MyDialog dialog;
    if (owner instanceof Frame) {
      dialog = new MyDialog((Frame) owner);
    } else if (owner instanceof Dialog) {
      dialog = new MyDialog((Dialog) owner);
    } else {
      if (owner != null) {
        Container parent = owner.getParent();
        if (parent instanceof Window) {
          return createMyDialog((Window) parent);
        }
      }
      assert owner == null : owner.getClass().getName();
      dialog = new MyDialog((JFrame) null);
    }
    return dialog;
  }

  protected void afterShowWindow() {
    if (myInitialFocusOwner == null && myOkButton != null)
      myOkButton.requestFocusInWindow();
  }

  public void insertContent(MyDialog dialog, UIComponentWrapper content) {
    dialog.setContent(content.getComponent());
    dialog.addToDispose(new UIComponentWrapper.Disposer(content));

    if(!Env.isMac()) {
      if (myOkCancelAction == null) {
        myOkButton = dialog.addAction(myOkAction, true, false);
        dialog.addAction(myCancelAction, false, true);
      } else
        myOkButton = dialog.addAction(myOkCancelAction, true, true);
//      if (myOkButton != null)
//        UIUtil.adjustFont(myOkButton, -1, Font.BOLD, false);
      dialog.addActions(myExtraActions);
    } else {
      dialog.addActions(myExtraActions);
      if (myOkCancelAction == null) {
        dialog.addAction(myCancelAction, false, true);
        myOkButton = dialog.addAction(myOkAction, true, false);
      } else
        myOkButton = dialog.addAction(myOkCancelAction, true, true);
    }

    if(myOkButton != null) {
      InitialWindowFocusFinder.setInitialWindowComponent(myOkButton);
    }
  }

  public void setPreferredSize(Dimension size) {
    myPreferredSize = size;
  }

  @Override
  public boolean isModal() {
    return myModal;
  }

  public void setBottomLineComponent(JComponent component) {
    assert myBottomLineComponent == null : "Old:" + myBottomLineComponent + " new:" + component;
    myBottomLineComponent = new UIComponentWrapper.Simple(component);
  }

  public void setAlwaysOnTop(boolean alwaysOnTop) {
    myAlwaysOnTop = alwaysOnTop;
  }

  public void setNullOwner(boolean nullOwner) {
    myNullOwner = nullOwner;
  }

  public void setBorders(boolean borders) {
    myBorders = borders;
  }

  public void setBottomLineShown(boolean shown) {
    myBottomLineShown = shown;
  }

  public void setBottomBevel(boolean bottomBevel) {
    myBottomBevel = bottomBevel;
  }

  public void closeWindow() throws CantPerformExceptionExplained {
    WindowController controller = getWindowContainer().getActor(WindowController.ROLE);
    if (controller != null) {
      CantPerformExceptionExplained exception = controller.close();
      if (exception != null)
        throw exception;
    } else
      assert false;
  }

  public void pressOk() {
    AActionButton button = myOkButton;
    if (button != null)
      button.doClick();
  }

  protected GraphicsConfiguration getRequiredDisplay() {
    if (myModal) {
      // modal windows should always appear on the same display as the current window
      Window window = UIUtil.getDefaultDialogOwner();
      if (window != null)
        return window.getGraphicsConfiguration();
    }
    return super.getRequiredDisplay();
  }

  public void setIcon(Image icon) {
    myIcon = icon;
  }

  public Image getIcon() {
    return myIcon;
  }

  static class MyDialog extends JDialog {
    private final JPanel myButtonPanel = new JPanel(new GridLayout(1, 2, UIUtil.GAP, 0));
    private final JPanel myMainPanel = new JPanel(UIUtil.createBorderLayout());
    private final JPanel myBottom = new JPanel(UIUtil.createBorderLayout());
    private final DetachComposite myOnDispose = new DetachComposite();
    private static final Object ACTION_MAP_KEY = new Object();
    @Nullable
    private AActionButton myCancelButton;

    public MyDialog(Dialog owner) throws HeadlessException {
      super(owner);
      init();
    }

    public MyDialog(Frame owner) throws HeadlessException {
      super(owner);
      init();
    }

    /**
     * @deprecated
     */
    public java.awt.Container getContentPane() {
      return super.getContentPane();
    }

    protected void processWindowEvent(WindowEvent e) {
      if (e.getID() == WindowEvent.WINDOW_CLOSING) {
        if (myCancelButton != null)
          myCancelButton.doClick();
        else
          super.processWindowEvent(e);
      }
    }

    private void init() {
      myMainPanel.setBorder(GAP_AROUND_BORDER);
      myBottom.setBorder(GAP_NORTH_BORDER);

      super.getContentPane().add(myMainPanel);
      myBottom.add(myButtonPanel, BorderLayout.EAST);
      myMainPanel.add(myBottom, BorderLayout.SOUTH);
      JRootPane rootPane = getRootPane();
      rootPane.getActionMap().put(ACTION_MAP_KEY, new AbstractAction() {
        @Override
        public Object getValue(String key) {
          if ("enabled".equals(key)) {
            return isEnabled();
          }
          return super.getValue(key);
        }

        @Override
        public boolean isEnabled() {
          return myCancelButton != null;
        }

        public void actionPerformed(ActionEvent e) {
          if (myCancelButton != null)
            myCancelButton.doClick();
        }
      });
      rootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Shortcuts.ESCAPE, ACTION_MAP_KEY);
/*
      addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          Dimension minimumSize = getContentPane().getMinimumSize();
          Dimension size = getSize();
          final int width = Math.max(minimumSize.width, size.width);
          final int height = Math.max(minimumSize.height, size.height);
          if (height != size.height || width != size.width) {
            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                setSize(width, height);
              }
            });
          }
        }
      });
*/
    }

    private void addBottomBevel() {
      UIUtil.addOuterBorder(myBottom, UIUtil.createNorthBevel(myBottom.getBackground()));
    }

    public void setContent(JComponent component) {
      myMainPanel.add(component, BorderLayout.CENTER);
    }

    public void setBottomLineComponent(JComponent component) {
      myBottom.add(component, BorderLayout.CENTER);
      component.setAlignmentX(0);
    }

    public void addToDispose(Detach onDispose) {
      myOnDispose.add(onDispose);
    }

    public void dispose() {
      super.dispose();
      myOnDispose.detach();
      super.getContentPane().removeAll();
      DataProvider.DATA_PROVIDER.removeAllProviders(getRootPane());
      myMainPanel.removeAll();
      myBottom.removeAll();
      myButtonPanel.removeAll();
    }

    @Nullable
    public AActionButton addAction(AnAction action, boolean isDefault, boolean isCancel) {
      if (action == null)
        return null;
      AActionButton button = new AActionButton(action);
      myButtonPanel.add(button);
      if (isDefault)
        getRootPane().setDefaultButton(button);
      if (isCancel) {
        assert myCancelButton == null : "Prev: " + myCancelButton + " new: " + action;
        myCancelButton = button;
      }
      return button;
    }

    public void addActions(List<AnAction> actions) {
      for (AnAction anAction : actions)
        addAction(anAction, false, false);
    }
  }

  private static final AnAction OK = SimpleAction.createDoNothing(L.actionName("OK"));

  private static final AnAction CANCEL = SimpleAction.createDoNothing(L.actionName("Cancel"));
}
