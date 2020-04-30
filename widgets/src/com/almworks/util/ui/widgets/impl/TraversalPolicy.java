package com.almworks.util.ui.widgets.impl;

import com.almworks.util.LogHelper;
import com.almworks.util.ui.swing.SwingTreeUtil;
import com.almworks.util.ui.widgets.genutil.StubComponent;
import org.almworks.util.Collections15;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

class TraversalPolicy extends FocusTraversalPolicy implements KeyEventDispatcher, FocusListener {
  private final WidgetHostComponent myHost;

  // focus stubs, in order of their "appearance"
  private final Component myStubEF = createStub("external-forward", -3);
  private final Component myStubIB = createStub("internal-backward", -2);
  private final Component myStubIF = createStub("internal-forward", 2);
  private final Component myStubEB = createStub("external-backward", 3);
  private Component myCurrentRemove = null;
  private final java.util.List<Component> myStubs = Collections15.list(myStubEF, myStubIB, myStubIF, myStubEB);

  private KeyboardFocusManager myFocusManager;

  private TraversalPolicy(WidgetHostComponent host) {
    myHost = host;
    install();
  }

  private void install() {
    for (Component stub : myStubs) {
      myHost.add(stub);
      stub.addFocusListener(this);
    }
    myHost.setFocusTraversalPolicy(this);
  }

  public void uninstall() {
    if (myHost.getFocusTraversalPolicy() != this) LogHelper.error("Not installed", myHost.getFocusTraversalPolicy(), this);
    else {
      for (Component stub : myStubs) {
        myHost.remove(stub);
        stub.removeFocusListener(this);
      }
      myHost.setFocusTraversalPolicy(null);
      myHost.setFocusTraversalPolicyProvider(false);
    }
  }

  public static TraversalPolicy install(WidgetHostComponent host) {
    TraversalPolicy policy = new TraversalPolicy(host);
    LogHelper.assertError(host.getFocusTraversalPolicy() == null, "Other policy already installed", host.getFocusTraversalPolicy(), policy);
    policy.install();
    host.setFocusTraversalPolicyProvider(true);
    return policy;
  }

  private static Component createStub(String name, int order) {
    // using BaseRendererComponent as it is the most light-weight thing
    StubComponent r = new StubComponent(name);
    r.setFocusable(true);
    r.setFocusTraversalKeysEnabled(false);
    int c = order < 0 ? order : Short.MAX_VALUE - order;
    r.setBounds(c, c, 1, 1);
    return r;
  }

  public Component getComponentAfter(Container aContainer, Component aComponent) {
    if (myStubs.contains(aComponent)) {
      return aComponent == myStubIF || aComponent == myStubEB ? null : myStubEB;
    }
    Container sub = findTopmostSubprovider(aContainer, aComponent);
    if (sub != null) {
      Component r = sub.getFocusTraversalPolicy().getComponentAfter(sub, aComponent);
      if (r != null && r != aComponent)
        return r;
    }
    if (myCurrentRemove == aComponent || SwingTreeUtil.isAncestor(myCurrentRemove, aComponent))
      return myHost;
    return myStubIF;
  }

  public Component getComponentBefore(Container aContainer, Component aComponent) {
    if (myStubs.contains(aComponent)) {
      return aComponent == myStubIB || aComponent == myStubEF ? null : myStubEF;
    }
    Container sub = findTopmostSubprovider(aContainer, aComponent);
    if (sub != null) {
      Component r = sub.getFocusTraversalPolicy().getComponentBefore(sub, aComponent);
      if (r != null && r != aComponent)
        return r;
    }
    return myStubIB;
  }

  public Component getFirstComponent(Container aContainer) {
    return myStubEF;
  }

  public Component getLastComponent(Container aContainer) {
    return myStubEB;
  }

  public Component getDefaultComponent(Container aContainer) {
    return myStubEF;
  }

  public void focusGained(FocusEvent e) {
    myHost.requestFocusInWindow();
    HostComponentState<?> state = myHost.getState();
    if (state == null)
      return;
    Component component = e.getComponent();
    if (component == myStubIB || component == myStubIF) {
      boolean forward = component == myStubIF;
      Component opposite = e.getOppositeComponent();
      HostCellImpl owningCell = null;
      while (owningCell == null && opposite != null && opposite != myHost) {
        if (opposite instanceof JComponent) {
          owningCell = HostCellImpl.OWNING_CELL.getClientValue((JComponent) opposite);
          opposite = opposite.getParent();
        }
      }
      FocusedWidgetManager focusManager = state.getFocusManager();
      if (owningCell == null) owningCell = focusManager.getFocusedCell();
      if (owningCell != null) focusManager.traverseFocus(owningCell, forward, true);
    }
  }

  public void focusLost(FocusEvent e) {}

  @Nullable
  private Container findTopmostSubprovider(Container aContainer, Component aComponent) {
    assert aContainer == myHost : myHost + " " + aContainer;
    if (aComponent == aContainer)
      return null;
    if (aContainer == null)
      throw new NullPointerException();
    if (aComponent == null)
      throw new NullPointerException();
    Container result = null;
    while (aComponent != aContainer && aComponent != null) {
      if (aComponent instanceof Container) {
        Container c = (Container) aComponent;
        if ((c.isFocusCycleRoot() || c.isFocusTraversalPolicyProvider()) && c.getFocusTraversalPolicy() != null) {
          result = c;
        }
      }
      aComponent = aComponent.getParent();
    }
    return result;
  }

  public void setCurrentRemove(Component component) {
    assert component == null || myCurrentRemove == null : myCurrentRemove;
    if (component != null && !SwingTreeUtil.isAncestor(myHost, component)) {
      assert false;
      return;
    }
    myCurrentRemove = component;
  }

  public void activate() {
    myFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    myFocusManager.addKeyEventDispatcher(this);
  }

  public void deactivate() {
    if (myFocusManager != null) {
      myFocusManager.removeKeyEventDispatcher(this);
      myFocusManager = null;
    }
  }

  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.isConsumed())
      return false;
    KeyboardFocusManager fm = myFocusManager;
    if (fm != KeyboardFocusManager.getCurrentKeyboardFocusManager())
      return false;
    Component focusOwner = fm.getFocusOwner();
    if (!myStubs.contains(focusOwner) && !myStubs.contains(e.getComponent()))
      return false;
    assert e.getClass() == KeyEvent.class : e;
    KeyEvent e2 = new KeyEvent(myHost, e.getID(), e.getWhen(), e.getModifiers() | e.getModifiersEx(), e.getKeyCode(),
      e.getKeyChar(), e.getKeyLocation());
    fm.redispatchEvent(myHost, e2);
    if (e2.isConsumed())
      e.consume();
    return true;
  }

  public boolean isExternalTraverse(Component focusOwner) {
    return focusOwner == myStubEB || focusOwner == myStubEF;
  }

  public boolean isBackwardTraverse(Component focusOwner) {
    return focusOwner == myStubEB || focusOwner == myStubIB;
  }
}
