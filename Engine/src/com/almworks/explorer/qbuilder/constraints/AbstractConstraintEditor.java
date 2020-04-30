package com.almworks.explorer.qbuilder.constraints;

import com.almworks.api.application.qb.ConstraintEditor;
import com.almworks.api.application.qb.ConstraintEditorNodeImpl;
import com.almworks.api.syncreg.ItemHypercube;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.ComponentKeyBinder;
import com.almworks.util.ui.ModelMapBinding;
import org.almworks.util.detach.Detach;
import org.almworks.util.detach.DetachComposite;
import org.almworks.util.detach.Lifespan;
import org.jetbrains.annotations.NotNull;

/**
 * @author : Dyoma
 */
public abstract class AbstractConstraintEditor implements ConstraintEditor {
  private final DetachComposite myDetach = new DetachComposite();
  private final ComponentKeyBinder myBinder;
  private final ConstraintEditorNodeImpl myNode;

  protected AbstractConstraintEditor(ConstraintEditorNodeImpl node) {
    myNode = node;
    myBinder = new ComponentKeyBinder(myDetach, node.getBinding());
  }

  protected final ComponentKeyBinder getBinder() {
    return myBinder;
  }

  protected void addToDispose(Detach detach) {
    myDetach.add(detach);
  }

  protected Lifespan getLifespan() {
    return myDetach;
  }

  @NotNull
  protected ItemHypercube getContextHypercube() {
    ItemHypercube cube = myNode.getContextHypercube();
    //noinspection ConstantConditions
    assert cube != null : myNode;
    return cube;
  }

  protected ModelMapBinding getBinding() {
    return myNode.getBinding();
  }

  public boolean wasChanged(PropertyKey<?, ?> ... keys) {
    for (PropertyKey<?, ?> key : keys) {
      if (getBinding().wasChanged(key))
        return true;
    }
    return false;
  }

  public <T> T getValue(PropertyKey<?, T> key) {
    return getBinding().getValue(key);
  }

  public void setValues(@NotNull PropertyMap values) {
    getBinding().setValues(values);
  }

  public boolean getBooleanValue(PropertyKey<?, Boolean> key) {
    return getValue(key);
  }

  public void dispose() {
    myDetach.detach();
  }

  public void onComponentDisplayble() {
  }
}
