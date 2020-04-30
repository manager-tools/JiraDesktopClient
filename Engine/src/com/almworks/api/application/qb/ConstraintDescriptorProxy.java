package com.almworks.api.application.qb;

import com.almworks.api.application.NameResolver;
import com.almworks.api.constraint.Constraint;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.items.api.DP;
import com.almworks.util.bool.BoolExpr;
import com.almworks.util.collections.ControlledModifiable;
import com.almworks.util.collections.RemoveableModifiable;
import com.almworks.util.components.CanvasRenderable;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.text.parser.FormulaWriter;
import org.almworks.util.Util;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The class is final because setDelegate() is called in constructor
 */
public final class ConstraintDescriptorProxy extends AbstractConstraintDescriptor
  implements ControlledModifiable.Controller
{
  private final ControlledModifiable myModifiable = new ControlledModifiable(this);

  private ConstraintDescriptor myDelegate;
  private Detach myDelegateDetach = Detach.NOTHING;

  public ConstraintDescriptorProxy(ConstraintDescriptor delegate) {
    setDelegate(delegate);
  }

  public static ConstraintDescriptor stub(String id, ConstraintType type) {
    return new ConstraintDescriptorProxy(new ConstraintDescriptorStub(id, type));
  }

  public String getDisplayName() {
    return getDelegate().getDisplayName();
  }

  @NotNull
  public String getId() {
    return getDelegate().getId();
  }

  public ConstraintEditor createEditor(ConstraintEditorNodeImpl node) {
    return getDelegate().createEditor(node);
  }

  public void writeFormula(FormulaWriter writer, PropertyMap data) {
    getDelegate().writeFormula(writer, data);
  }

  public ConstraintType getType() {
    return getDelegate().getType();
  }

  @NotNull
  public ConstraintDescriptor resolve(NameResolver resolver, @Nullable ItemHypercube cube, PropertyMap data) {
    ConstraintDescriptor delegate = getDelegate();
    ConstraintDescriptor resolved = delegate.resolve(resolver, cube, data);
    if (resolved != delegate)
      setDelegate(resolved);
    return this;
  }

  public RemoveableModifiable getModifiable() {
    return myModifiable;
  }

  public BoolExpr<DP> createFilter(PropertyMap data, ItemHypercube hypercube) {
    return getDelegate().createFilter(data, hypercube);
  }

  public Constraint createConstraint(PropertyMap data, ItemHypercube cube) {
    return getDelegate().createConstraint(data, cube);
  }

  public boolean isSameData(PropertyMap data1, PropertyMap data2) {
    return getDelegate().isSameData(data1, data2);
  }

  public CanvasRenderable getPresentation() {
    return getDelegate().getPresentation();
  }

  @Override
  public <T> T cast(Class<T> descriptorClass) {
    T result = Util.castNullable(descriptorClass, getDelegate());
    if (result != null) return result;
    return super.cast(descriptorClass);
  }

  public void onListenerAdded(ControlledModifiable modifiable) {
    ConstraintDescriptor delegate;
    boolean subscribe;
    synchronized (this) {
      subscribe = myModifiable.getListenerCount() > 0 && myDelegateDetach == Detach.NOTHING;
      delegate = myDelegate;
    }
    if (subscribe) {
      listenDelegate(delegate);
    }
  }

  public void onListenerRemoved(ControlledModifiable modifiable) {
    boolean unsubscribe;
    Detach detach;
    synchronized (this) {
      detach = myDelegateDetach;
      unsubscribe = myModifiable.getListenerCount() == 0 && detach != Detach.NOTHING;
    }
    if (unsubscribe) {
      detach.detach();
      synchronized(this) {
        myDelegateDetach = Detach.NOTHING;
      }
    }
  }

  @NotNull
  public synchronized ConstraintDescriptor getDelegate() {
    return myDelegate;
  }

  private void listenDelegate(ConstraintDescriptor delegate) {
    assert !Thread.holdsLock(this) : this;
    DetachComposite detach = new DetachComposite();
    delegate.getModifiable().addChangeListener(detach, myModifiable);
    synchronized (this) {
      if (myDelegateDetach == Detach.NOTHING) {
        myDelegateDetach = detach;
      } else {
        assert false : this;
      }
    }
  }

  private void setDelegate(@NotNull ConstraintDescriptor delegate) {
    boolean hasListeners;
    Detach detach;
    synchronized (this) {
      detach = myDelegateDetach;
      myDelegateDetach = Detach.NOTHING;
      myDelegate = delegate;
      hasListeners = myModifiable.getListenerCount() > 0;
    }
    detach.detach();
    if (hasListeners) {
      // do not add listener under lock (due to back flow through ControlledModifiable.Controller)
      listenDelegate(delegate);
      myModifiable.fireChanged();
    }
  }
}
