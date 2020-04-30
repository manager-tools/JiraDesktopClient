package com.almworks.util.components.plaf.macosx;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.peer.MenuComponentPeer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * A heavyweight {@link JPopupMenu} implementation, that delegates to an AWT {@link PopupMenu} for display.
 * All actions and listeners are redispacthed to the respective Swing components, and sources are rewritten
 * (ie., the source of an {@code ActionEvent} will be the the Swing {@link JMenuItem}, not the AWT {@link MenuItem}
 * that really triggered the action).
 * Apple seems to do the same for the screen menu bar in their Aqua Look and Feel.
 * <p/>
 * Icon images and tooltips are supported on Apple VMs.
 * <p/>
 * Please not that this implementation is somewhat crippled, and should only be used in very specific scenarios
 * (like on OS X).
 * <br/>
 * Known limitations:
 * <ul>
 * <li><em>Non-menu items are not supported.</em> Trying to add a non-menu item (like a {@code JButton}) will result in
 * an {@link IllegalArgumentException} thrown from the {@code add/insert} methods.</li>
 * <li>{@link javax.swing.event.PopupMenuListener#popupMenuCanceled(javax.swing.event.PopupMenuEvent)
 * PopupMenuListener.popupMenuCanceled()} is not fired immediately when the popup is cancelled.</li>
 * <li>There is no way to programatically dismiss the popup (like
 * {@link javax.swing.JPopupMenu#setVisible(boolean) JPopupMenu.setVisible(false)}).</li>
 * <li>While icons are synced, only the images of the icons are used
 * (ie., roll-over effects or similar will not work).</li>
 * <li>Adding mouse listeneres or similar on menu items is not supported.</li>
 * </ul>
 *
 * @author <a href="mailto:harald.kuhr@gmail.no">Harald Kuhr</a>
 * @author <a href="mailto:pzvyagin@gmail.com">Pavel Zvyagin</a>
 */
// TODO: The current main issues with this class is:
//       - We need a better way to determine if a menu was cancelled
//       - We need a way to hide an AWT popup
//       - There are probably events that are not dispatched correctly
// todo: Could cache AWT menu items rather than recreate them for each showing.
public final class AquaPopupMenuAdapter extends JPopupMenu {
  private static final String AWT_MENU_KEY = "awtMenu";

  private final PopupMenu mAWTMenu = new PopupMenu() {
    @Override
    public void addNotify() {
      super.addNotify();
      syncAppleItems(AquaPopupMenuAdapter.this);
    }
  };

  private final PopupMenuDismissHandler mPopupMenuDismissHandler = new PopupMenuDismissHandler();
  private boolean mVisible;

  public AquaPopupMenuAdapter() {
    this(null);

    // See setFont()
    mAWTMenu.setFont(getFont());
  }

  public AquaPopupMenuAdapter(String pLabel) {
    super(pLabel);
    mAWTMenu.setLabel(pLabel);
    putClientProperty(AWT_MENU_KEY, mAWTMenu);
  }

  @Override
  public boolean isLightWeightPopupEnabled() {
    return false;
  }

  @Override
  public void setLabel(String pLabel) {
    mAWTMenu.setLabel(pLabel);
    super.setLabel(pLabel);
  }

  @Override
  public String getLabel() {
    return mAWTMenu.getLabel();
  }

  @Override
  public void setFont(Font pFont) {
    super.setFont(pFont);
    // NOTE: mAWTMenu may be null, as updateUI (and thus setFont) is called from super contstructor..
    if(mAWTMenu != null) {
      mAWTMenu.setFont(pFont);
    }
  }

  @Override
  public void setInvoker(Component pInvoker) {
    // TODO: Trigger property change event?
    if(mAWTMenu.getParent() != pInvoker) {
      if(pInvoker != null) {
        pInvoker.add(mAWTMenu);
      } else {
        mAWTMenu.getParent().remove(mAWTMenu);
      }
    }
  }

  @Override
  public Component getInvoker() {
    return (Component)mAWTMenu.getParent();
  }

  @Override
  public void setVisible(boolean pVisible) {
    if(pVisible && !mVisible) {
      show(getInvoker(), getX(), getY());
    } else {
      // TODO: What now? There is no way to force hide the AWT popup (AFAIK)..
      firePropertyChange("visible", mVisible, mVisible = false);
    }
  }

  @Override
  public boolean isVisible() {
    return mVisible;
  }

  @Override
  public void show(final Component pInvoker, final int x, final int y) {
    setInvoker(pInvoker);

    firePopupMenuWillBecomeVisible();
    mAWTMenu.show(pInvoker, x, y + 4); // 4 pixel difference, might just be OS X?
    mVisible = true;

    // Invoke later, so we don't dismiss before menus is shown
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Toolkit.getDefaultToolkit().addAWTEventListener(mPopupMenuDismissHandler, -1l);
      }
    });
  }

  @Override
  public void removeAll() {
    super.removeAll();
    mAWTMenu.removeAll();
  }

  @Override
  protected void addImpl(Component pComponent, Object pConstraints, int pIndex) {
    if(!(pComponent instanceof Separator || pComponent instanceof JMenuItem)) {
      throw new IllegalArgumentException("Only menu items supported: " + pComponent);
    }
    super.addImpl(pComponent, pConstraints, pIndex);
  }

  private MenuItem wrap(final JMenuItem pMenuItem) {
    if(pMenuItem instanceof JRadioButtonMenuItem || pMenuItem instanceof JCheckBoxMenuItem) {
      return createCheck(pMenuItem);
    } else if(pMenuItem instanceof JMenu) {
      return createMenu((JMenu)pMenuItem);
    } else {
      return createItem(pMenuItem);
    }
  }

  private Menu createMenu(final JMenu pMenu) {
    final Menu awtMenu = configureAWTItem(new Menu(), pMenu);
    pMenu.putClientProperty(AWT_MENU_KEY, awtMenu);
    return awtMenu;
  }

  private MenuItem createItem(final JMenuItem pMenuItem) {
    return configureAWTItem(new MenuItem(), pMenuItem);
  }

  private CheckboxMenuItem createCheck(final JMenuItem pMenuItem) {
    final CheckboxMenuItem checkItem = configureAWTItem(new CheckboxMenuItem(), pMenuItem);

    checkItem.setState(pMenuItem.isSelected());

    // Sync selected state both ways
    pMenuItem.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent pEvent) {
        checkItem.setState(pMenuItem.isSelected());
      }
    });

    checkItem.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent pEvent) {
        // If pMenuItem is a radiobutton, we should not disable it, if it is the selected button in the group
        if(pMenuItem instanceof JRadioButtonMenuItem) {
          if(pMenuItem.getModel() instanceof DefaultButtonModel) {
            final DefaultButtonModel model = (DefaultButtonModel) pMenuItem.getModel();
            if(model.getGroup() == null || model.getGroup().isSelected(model)) {
              checkItem.setState(true);
              return;
            }
          }
        }

        pMenuItem.setSelected(pEvent.getStateChange() == ItemEvent.SELECTED);

        // Fire action, if any
        final Action action = pMenuItem.getAction();
        if(action != null) {
          // Redispacth with the Swing item as source
          action.actionPerformed(new ActionEvent(pMenuItem, pEvent.getID(), checkItem.getActionCommand()));
        }
      }
    });

    checkItem.addItemListener(mPopupMenuDismissHandler);

    return checkItem;
  }

  private <T extends MenuItem> T configureAWTItem(final T pAWTItem, final JMenuItem pItem) {
    pAWTItem.setLabel(pItem.getText());
    pAWTItem.setName(pItem.getName());
    pAWTItem.setActionCommand(pItem.getActionCommand());
    pAWTItem.setEnabled(pItem.isEnabled());

    pAWTItem.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent pEvent) {
        final Action action = pItem.getAction();
        if(action != null) {
          // Redispacth with the Swing item as source
          action.actionPerformed(
            new ActionEvent(
              pItem, pEvent.getID(), pEvent.getActionCommand(), pEvent.getWhen(), pEvent.getModifiers()));
        }
      }
    });

    pItem.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent pEvent) {
        final String name = pEvent.getPropertyName();

        if("text".equals(name)) {
          pAWTItem.setLabel((String) pEvent.getNewValue());
        } else if("icon".equals(name) || "toolTipText".equals(name) || "accelerator".equals(name)) {
          syncAppleItem(pAWTItem, (JMenuItem) pEvent.getSource());
        } else if("enabled".equals(name)) {
          pAWTItem.setEnabled(Boolean.TRUE.equals(pEvent.getNewValue()));
        }
      }
    });

    pAWTItem.addActionListener(mPopupMenuDismissHandler);

    return pAWTItem;
  }

  /**
   * This method reassembles the AWT menu subtree for a
   * Swing menu subtree starting at the given root menu.
   * @param pRoot The root menu, must be either a {@code JMenu}
   * or {@code JPopupMenu}. Must have its peer AWT {@code Menu}
   * attached under {@link #AWT_MENU_KEY}.
   */
  private void syncAppleItems(final JComponent pRoot) {
    // Checking that pRoot is a menu
    if(!(pRoot instanceof JPopupMenu || pRoot instanceof JMenu)) {
      assert false : pRoot;
      return;
    }

    // Getting pRoot's AWT peer and clearing it
    final Object obj = pRoot.getClientProperty(AWT_MENU_KEY);
    if(!(obj instanceof Menu)) {
      assert false : obj;
      return;
    }

    final Menu awtMenu = (Menu)obj;

    awtMenu.removeAll();

    // Getting pRoot's child items, menus, and separators
    final Component[] components;
    if(pRoot instanceof JMenu) {
      components = ((JMenu)pRoot).getMenuComponents();
    } else if(pRoot instanceof JPopupMenu) {
      components = pRoot.getComponents();
    } else {
      // we'll never get here, but IDEA won't figure it out
      return;
    }

    // Creating and adding AWT peers for visible items only;
    // for separators we check there's at most one in a row
    boolean hasSeparator = true; // true to avoid separator at the beginning
    for(final Component element : components) {
      if(element instanceof JMenuItem) {
        final JMenuItem item = (JMenuItem) element;
        if(item.isVisible()) {
          final MenuItem awtItem = wrap(item);
          awtMenu.add(awtItem);
          syncAppleItem(awtItem, item);
          if(item instanceof JMenu) {
            syncAppleItems(item);
          }
          hasSeparator = false;
        }
      } else if(element instanceof Separator && !hasSeparator) {
        awtMenu.addSeparator();
        hasSeparator = true;
      }
    }

    // Cutting of the separator at the end, if there is one
    if(hasSeparator) {
      final int itemCount = awtMenu.getItemCount();
      if(itemCount > 0) {
        awtMenu.remove(itemCount - 1);
      }
    }
  }

  // NOTE: This method must be invoked after the AWT popup has received it's peer
  private static <T extends MenuItem> void syncAppleItem(final T pAWTItem, final JMenuItem pItem) {
    final Class<?> applePeerClass;
    try {
      applePeerClass = Class.forName("apple.awt.CMenuItem");
    } catch (ClassNotFoundException e) {
      // Not an Apple VM/library, nothing to do..
      return;
    }

    @SuppressWarnings({"deprecation"})
    final MenuComponentPeer peer = pAWTItem.getPeer();

    if(applePeerClass.isInstance(peer)) {
      final Image image = imageForIcon(pItem.getIcon(), pItem);
      invokeMethod("setImage", applePeerClass, peer, Image.class, image);

      final String tip = pItem.getToolTipText();
      invokeMethod("setToolTipText", applePeerClass, peer, String.class, tip);

      final KeyStroke ks = pItem.getAccelerator();
      if(ks != null) {
        invokeMethod("setLabel", applePeerClass, peer,
          new Class[] { String.class, char.class, int.class, int.class },
          new Object[] { pItem.getText(), ks.getKeyChar(), ks.getKeyCode(), ks.getModifiers() });
      }
    }
  }

  // TODO: This might be a SwingUtil method
  private static Image imageForIcon(final Icon pIcon, final Component pComponent) {
    final Image image;

    if(pIcon == null) {
      return null;
    } else if(pIcon instanceof ImageIcon) {
      image = ((ImageIcon)pIcon).getImage();
    } else {
      final BufferedImage buffered = new BufferedImage(
        pIcon.getIconWidth(), pIcon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g = buffered.createGraphics();
      try {
        pIcon.paintIcon(pComponent, g, 0, 0);
      } finally {
        g.dispose();
      }
      image = buffered;
    }
    
    return image;
  }

  private static void invokeMethod(
    String pMethodName, Class<?> pType, Object pInstance, Class<?> pParameterType, Object pParameter)
  {
    invokeMethod(pMethodName, pType, pInstance, new Class[] { pParameterType }, new Object[] { pParameter });
  }

  private static void invokeMethod(
    String pMethodName,  Class<?> pType, Object pInstance, Class<?>[] pParameterTypes, Object[] pParameters)
  {
    try {
      pType.getMethod(pMethodName, pParameterTypes).invoke(pInstance, pParameters);
    } catch(NoSuchMethodException ignore) {
    } catch(IllegalAccessException ignore) {
    } catch(InvocationTargetException e) {
      final Throwable targetException = e.getTargetException();
      if(targetException instanceof Error) {
        throw (Error)targetException;
      }
      if(targetException instanceof RuntimeException) {
        throw (RuntimeException)targetException;
      }
      throw new UndeclaredThrowableException(targetException);
    }
  }

  private final class PopupMenuDismissHandler implements ActionListener, ItemListener, AWTEventListener {
    public void actionPerformed(ActionEvent pEvent) {
      // NOTE: Only for normal items, checkbox items will fire itemStateChanged instead...
      deregisterAWTListener();
      firePopupMenuWillBecomeInvisible();
      mVisible = false;
    }

    public void itemStateChanged(ItemEvent pEvent) {
      deregisterAWTListener();
      firePopupMenuWillBecomeInvisible();
      mVisible = false;
    }

    public void eventDispatched(AWTEvent pEvent) {
      // HACK: We rely on AWT event system NOT dispatching while menu is visible, and fire on first event after
      if(mVisible) {
        deregisterAWTListener();
        setVisible(false);
        firePopupMenuCanceled();
      }
    }

    private void deregisterAWTListener() {
      Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }
  }
}
