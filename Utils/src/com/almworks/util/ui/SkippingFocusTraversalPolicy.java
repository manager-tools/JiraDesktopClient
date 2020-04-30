package com.almworks.util.ui;

import org.almworks.util.Collections15;

import java.awt.*;
import java.util.Set;

public class SkippingFocusTraversalPolicy extends FocusTraversalPolicy {
  private final FocusTraversalPolicy myDelegate;
  private final Set<? extends Component> mySkip;
  private boolean mySkipping = false;

  private static final DelegateCall LAST = new DelegateCall() {
    public Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component) {
      return delegate.getLastComponent(focusCycleRoot);
    }
  };
  private static final DelegateCall AFTER = new DelegateCall() {
    public Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component) {
      return delegate.getComponentAfter(focusCycleRoot, component);
    }
  };
  private static final DelegateCall BEFORE = new DelegateCall() {
    public Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component) {
      return delegate.getComponentBefore(focusCycleRoot, component);
    }
  };
  private static final DelegateCall DEFAULT = new DelegateCall() {
    public Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component) {
      return delegate.getDefaultComponent(focusCycleRoot);
    }
  };
  private static final DelegateCall FIRST = new DelegateCall() {
    public Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component) {
      return delegate.getFirstComponent(focusCycleRoot);
    }
  };

  public SkippingFocusTraversalPolicy(FocusTraversalPolicy delegate, Set<? extends Component> skip) {
    myDelegate = delegate;
    mySkip = Collections15.hashSet(skip);
  }

  public Component getLastComponent(Container focusCycleRoot) {
    return skip(LAST, focusCycleRoot, null, false);
  }

  public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
    return skip(AFTER, focusCycleRoot, aComponent, true);
  }

  public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
    return skip(BEFORE, focusCycleRoot, aComponent, false);
  }

  public Component getDefaultComponent(Container focusCycleRoot) {
    return skip(DEFAULT, focusCycleRoot, null, true);
  }

  public Component getFirstComponent(Container focusCycleRoot) {
    return skip(FIRST, focusCycleRoot, null, true);
  }

  private Component skip(DelegateCall call, Container focusCycleRoot, Component component, boolean forward) {
    boolean alreadySkipping = mySkipping;
    mySkipping = true;
    try {
      // kludge - but without it we risk to clash with other delegating policy, which results in stack overflow
      if (alreadySkipping)
        return component;
      
      Component initial = call.call(myDelegate, focusCycleRoot, component);

      Component result = initial;
      while (mySkip.contains(result)) {
        Component next;
        if (forward)
          next = myDelegate.getComponentAfter(focusCycleRoot, result);
        else
          next = myDelegate.getComponentBefore(focusCycleRoot, result);
        if (next == initial || next == result) {
          result = initial;
          break;
        }
        result = next;
      }
      return result;
    } finally {
      if (!alreadySkipping)
        mySkipping = false;
    }
  }

  private static abstract class DelegateCall {
    public abstract Component call(FocusTraversalPolicy delegate, Container focusCycleRoot, Component component);
  }
}
