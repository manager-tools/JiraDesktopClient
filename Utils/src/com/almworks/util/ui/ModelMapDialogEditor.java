package com.almworks.util.ui;

import com.almworks.util.collections.Modifiable;
import com.almworks.util.collections.SimpleModifiable;
import com.almworks.util.properties.PropertyKey;
import com.almworks.util.properties.PropertyMap;
import com.almworks.util.ui.actions.CantPerformExceptionExplained;
import org.almworks.util.detach.DetachComposite;

import java.util.Set;

/**
 * @author dyoma
 */
public abstract class ModelMapDialogEditor implements DialogEditor {
  private final PropertyMap myValues;
  private final ModelMapBinding myBinding;
  private final DetachComposite myDetach = new DetachComposite();
  private final ComponentKeyBinder myBinder;
  private final SimpleModifiable myModifiable = new SimpleModifiable();

  protected ModelMapDialogEditor(PropertyMap workingValues) {
    myValues = workingValues;
    myBinding = new ModelMapBinding(myValues);
    myBinder = new ComponentKeyBinder(myDetach, myBinding);
    myBinding.addAWTChangeListener(myDetach, myModifiable);
  }

  @Override
  public Modifiable getModifiable() {
    return myModifiable;
  }

  public void reset() {
    myBinding.setValues(myValues);
  }

  public void apply() throws CantPerformExceptionExplained {
    myValues.clear();
    myBinding.getCurrentValues(myValues);
  }

  public void dispose() {
    myDetach.detach();
  }

  public boolean isModified() {
    Set<? extends PropertyKey<?, ?>> keys = myBinding.keySet();
    for (PropertyKey<?, ?> key : keys) {
      if (myBinding.wasChanged(key))
        return true;
    }
    return false;
  }

  protected final ComponentKeyBinder getBinder() {
    return myBinder;
  }
}
